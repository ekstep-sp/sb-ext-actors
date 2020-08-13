/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.infosys.cassandra.CassandraOperation;
import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.helper.ServiceFactory;
import com.infosys.service.DownloadService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;
import org.springframework.stereotype.Service;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DownloadServiceImpl implements DownloadService {

    private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private PropertiesCache properties = PropertiesCache.getInstance();
    private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
    private String keyspace = properties.getProperty(JsonKey.DB_KEYSPACE);
    private Util.DbInfo userDb = Util.dbInfoMap.get(JsonKey.USER_DB);
    private Util.DbInfo userContentDownloadDb = Util.dbInfoMap.get(LexJsonKey.USER_CONTENT_DOWNLOAD_DB);
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    public DownloadServiceImpl() {
        Util.checkCassandraDbConnections(keyspace);
        Util.checkCassandraDbConnections(bodhiKeyspace);
    }

    @Override
    public boolean addContentDownload(String userId, String contentId) throws IOException {

        //Validating the userId
        Response cassandraResponse = cassandraOperation.getRecordById(userDb.getKeySpace(), userDb.getTableName(),
                userId);
        List<Map<String, Object>> userDetails = (List<Map<String, Object>>) cassandraResponse.get(Constants.RESPONSE);
        if (userDetails.size() == 0 || userDetails.get(0) == null || userDetails.get(0).isEmpty()) {
            ProjectLogger.log("Invalid userId " + userId);
            return false;
        }

        //Validate content Id
        Map<String, Object> result = ElasticSearchUtil
                .getDataByIdentifier(LexProjectUtil.EsIndex.bodhi.getIndexName(),
                        LexProjectUtil.EsType.resource.getTypeName(), contentId);
        if (result == null || result.isEmpty()) {
            ProjectLogger.log("Invalid contentId " + contentId);
            return false;
        } else if (!result.get(JsonKey.STATUS).equals(LexProjectUtil.Status.LIVE.getValue())) {
            ProjectLogger.log("Content " + contentId + " is not Live");
            return false;
        }

        Timestamp dateDownloaded = new Timestamp(System.currentTimeMillis());

        Map<String, Object> request = new HashMap<>();
        request.put(JsonKey.USER_ID, userId);
        request.put(JsonKey.CONTENT_ID, contentId);
        request.put(LexJsonKey.DATE_DOWNLOADED, dateDownloaded);

        cassandraOperation.insertRecord(userContentDownloadDb.getKeySpace(), userContentDownloadDb.getTableName(),
                request);

        String downloadColumnName = "me_totalDownloads";
        int totalDownloads = (int) result.get(downloadColumnName);
        Map<String, Object> data = new HashMap<>();
        data.put(downloadColumnName, totalDownloads + 1);
        boolean ret = ElasticSearchUtil.updateData(LexProjectUtil.EsIndex.bodhi.getIndexName(),
                LexProjectUtil.EsType.resource.getTypeName(), contentId, data);
        return ret;
    }

    @Override
    public List<Map<String, Object>> getContentDownloaded(String userId) throws IOException {
        Response response = cassandraOperation.getRecordsByProperty(userContentDownloadDb.getKeySpace(),
                userContentDownloadDb.getTableName(), JsonKey.USER_ID, userId);

        List<Map<String, Object>> result = (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);

        // Sending blank list if user has not downloaded any course
        if (result.size() == 0) {
            return new ArrayList<>();
        }

        List<String> contents = new ArrayList<>();

        for (Map<String, Object> entry : result) {
            contents.add(entry.get(JsonKey.CONTENT_ID).toString());
        }

        Map<String, Object> searchData = new HashMap<>();
        searchData.put(JsonKey.IDENTIFIER, contents);
        searchData.put(JsonKey.STATUS, LexProjectUtil.Status.LIVE.getValue());

        List<Map<String, Object>> coursesList = ElasticSearchUtil.searchMatchedData(LexProjectUtil.EsIndex.bodhi.getIndexName(),
                LexProjectUtil.EsType.resource.getTypeName(), searchData, null, contents.size());

        return coursesList;
    }

    @Override
    public List<String> syncContentDownload(String userId, List<String> contentIds) throws IOException {

        List<String> ret = new ArrayList<>();

        //Validating the userId
        Response cassandraResponse = cassandraOperation.getRecordById(userDb.getKeySpace(), userDb.getTableName(),
                userId);
        List<Map<String, Object>> userDetails = (List<Map<String, Object>>) cassandraResponse.get(Constants.RESPONSE);
        if (userDetails.size() == 0 || userDetails.get(0) == null || userDetails.get(0).isEmpty()) {
            ProjectLogger.log("Invalid userId " + userId);
            ret.add(userId);
            return ret;
        }

        //Fetch the currently existing download details for the user in DB
        Response response = cassandraOperation.getRecordsByProperty(userContentDownloadDb.getKeySpace(),
                userContentDownloadDb.getTableName(), JsonKey.USER_ID, userId);

        List<Map<String, Object>> courseDownloads = (List<Map<String, Object>>) response.getResult()
                .get(JsonKey.RESPONSE);

        //Deleting contents which are not present in the local device
        for (Map<String, Object> course : courseDownloads) {
            String contentId = (String) course.get(JsonKey.CONTENT_ID);
            Map<String, Object> prop = new HashMap<>();
            prop.put(JsonKey.USER_ID, userId);
            if (!contentIds.contains(contentId)) {
                prop.put(JsonKey.CONTENT_ID, contentId);
                cassandraOperation.deleteRecordByProperties(userContentDownloadDb.getKeySpace(),
                        userContentDownloadDb.getTableName(), prop);
            } else {
                //Deleting the content if from the input list if it is present in DB
                contentIds.remove(contentId);
            }
        }

        //Updating other contents which are present in the device but not present in DB
        for (String contentId : contentIds) {

            //Validate content Id
            Map<String, Object> result = ElasticSearchUtil
                    .getDataByIdentifier(LexProjectUtil.EsIndex.bodhi.getIndexName(),
                            LexProjectUtil.EsType.resource.getTypeName(), contentId);
            if (result == null || result.isEmpty()) {
                ProjectLogger.log("Invalid contentId " + contentId);
                ret.add(contentId);
            } else if (!result.get(JsonKey.STATUS).equals(LexProjectUtil.Status.LIVE.getValue())) {
                ProjectLogger.log("Content " + contentId + " is not Live");
                ret.add(contentId);
            } else {

                Timestamp dateDownloaded = new Timestamp(System.currentTimeMillis());

                Map<String, Object> request = new HashMap<>();
                request.put(JsonKey.USER_ID, userId);
                request.put(JsonKey.CONTENT_ID, contentId);
                request.put(LexJsonKey.DATE_DOWNLOADED, dateDownloaded);

                cassandraOperation.insertRecord(userContentDownloadDb.getKeySpace(), userContentDownloadDb.getTableName(),
                        request);
            }
        }
        return ret;
    }
}