/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.service.TagsService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;
import org.springframework.stereotype.Service;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.Constants;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;

import java.io.IOException;
import java.util.*;

@Service
public class TagsServiceImpl implements TagsService {

    //private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private PropertiesCache properties = PropertiesCache.getInstance();
    private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
    private Util.DbInfo contentTagDb = Util.dbInfoMap.get(LexJsonKey.CONTENT_TAGS_DB);
    private CassandraConnectionManager connectionManager;

    private Map<String, Object> tagsWithParent = new HashMap<String, Object>();

    public TagsServiceImpl() {
        Util.checkCassandraDbConnections(bodhiKeyspace);
        PropertiesCache propertiesCache = PropertiesCache.getInstance();
        String cassandraMode = propertiesCache.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
        connectionManager = CassandraConnectionMngrFactory.getObject(cassandraMode);
    }

    //Prepares the select query with specified columns from a table in a particular keyspace in Cassandra
    private static String prepareSelectStatementWithSpecifiedColumns(String keyspaceName, String tableName, String... columns) {
        StringBuilder query = new StringBuilder(Constants.SELECT);
        query.append(String.join(",", columns));
        query.append(Constants.FROM + keyspaceName + Constants.DOT + tableName);
        query.append(';');
        return query.toString();
    }

    @Override
    public List<Map<String, Object>> getNestedTags() throws IOException {
        List<Map<String, Object>> result = ElasticSearchUtil.getNestedAggregation(LexProjectUtil.EsIndex.bodhi.getIndexName(),
                LexProjectUtil.EsType.resource.getTypeName(), JsonKey.TAGS, JsonKey.TAGS + "." + JsonKey.TYPE,
                JsonKey.TAGS + "." + LexJsonKey.VALUE);
        for (Map<String, Object> x : result) {
            x.put(JsonKey.NAME, x.get(JsonKey.IDENTIFIER));
        }
        return result;
    }

    @Override
    //Return all content Tags without any hierarchy
    public Response fetchContentTagsWithoutHierarchy() {
        Response contentResponse = new Response();
        try {
            contentResponse = getRecordsWithSpecifiedColumns(bodhiKeyspace, contentTagDb.getTableName(), "id", "type", "value", "parent", "children");
        } catch (ProjectCommonException e) {
            ProjectLogger.log("Exception occured while processing user goals progress : " + e.getMessage(), e);
            ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
            contentResponse.setResponseCode(responseCode);
            return contentResponse;
        }
        return contentResponse;
    }

    @SuppressWarnings("unchecked")
    @Override
    //Return all content Tags with hierarchy
    public Response fetchContentTagsWithHierarchy() {
        Response contentResponse = new Response();
        try {
            contentResponse = getRecordsWithSpecifiedColumns(bodhiKeyspace, contentTagDb.getTableName(), "id", "type", "value", "children", "parent");
            List<Map<String, Object>> allContentTags = (List<Map<String, Object>>) contentResponse.getResult()
                    .get("response");

            ArrayList<Map<String, Object>> hierarchialTags = new ArrayList<Map<String, Object>>();


            for (Map<String, Object> item : allContentTags) {
                if (item.get("parent") == null) { // Adds the tags which will be the grand parents
                    item.remove("parent");
                    hierarchialTags.add(item);
                } else { //Adds all other tags in another map
                    item.remove("parent");
                    tagsWithParent.put(item.get("id").toString(), item);
                }
            }
            //iterates through grandparent tags and prepares the hierarchy
            for (Map<String, Object> tag : hierarchialTags) {
                List<Map<String, Object>> newChildren = findChildrenData(tag);
                tag.remove("children");
                tag.put("children", newChildren);
            }

            contentResponse.put(Constants.RESPONSE, hierarchialTags);
        } catch (ProjectCommonException e) {
            ProjectLogger.log("Exception occured while processing user goals progress : " + e.getMessage(), e);
            ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
            contentResponse.setResponseCode(responseCode);
            return contentResponse;
        }
        return contentResponse;
    }

    @SuppressWarnings("unchecked")
    //Recursive function which finds the children data and creates the hierarchy
    private List<Map<String, Object>> findChildrenData(Map<String, Object> item) {
        List<UUID> childrenList = (List<UUID>) item.get("children");
        if (!childrenList.isEmpty()) {
            List<Map<String, Object>> newChildrenList = new ArrayList<Map<String, Object>>();
            for (UUID childTag : childrenList) {
                Map<String, Object> tagData = (Map<String, Object>) tagsWithParent.get(childTag.toString());
                if (tagData != null) {
                    List<Map<String, Object>> newData = findChildrenData(tagData);
                    tagData.remove("children");
                    tagData.put("children", newData);
                    newChildrenList.add(tagData);
                }
            }
            return newChildrenList;
        }
        return new ArrayList<Map<String, Object>>();
    }

    /*
    //Returns a specified tag from a Collection
    private Map<String,Object> fetchTagwithID(List<Map<String,Object>> map,UUID id){
        for(Map<String, Object> item : map) {
            if (item.get("id").equals(id)) {
                return item;
            }
        }
        return null;
    }
    */
    //Returns the table response from a table in a particular keyspace in Cassandra with specified columns
    private Response getRecordsWithSpecifiedColumns(String keyspaceName, String tableName, String... properties) {
        Response response = new Response();
        try {
            String selectQuery = prepareSelectStatementWithSpecifiedColumns(keyspaceName, tableName, properties);
            PreparedStatement statement = connectionManager.getSession(keyspaceName).prepare(selectQuery);
            BoundStatement boundStatement = new BoundStatement(statement);
            ResultSet results =
                    connectionManager.getSession(keyspaceName).execute(boundStatement);
            response = CassandraUtil.createResponse(results);
        } catch (Exception e) {
            throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
                    ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
        }
        return response;
    }
}
