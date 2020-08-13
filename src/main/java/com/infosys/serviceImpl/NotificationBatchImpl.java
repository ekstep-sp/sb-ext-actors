/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.infosys.service.NotificationBatch;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

@Service
public class NotificationBatchImpl implements NotificationBatch {

	@Autowired
	UserUtilityService userUtilService;
	
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    private PropertiesCache properties = PropertiesCache.getInstance();
    private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
    private String leaderboard = properties.getProperty(LexJsonKey.LeaderboardTable);
    private String userBadges = properties.getProperty(LexJsonKey.UserBadges);
    private CassandraConnectionManager connectionManager;

    public NotificationBatchImpl() {
        Util.checkCassandraDbConnections(bodhiKeyspace);
        PropertiesCache propertiesCache = PropertiesCache.getInstance();
        String cassandraMode = propertiesCache.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
        connectionManager = CassandraConnectionMngrFactory.getObject(cassandraMode);
    }

    private Response getRecordsbetweenDateForUser(String keyspaceName, String tableName, String gtdateName, Date gtdate, String ltdateName, Date ltdate, String key, String value) {
        Response response = new Response();
        try {
            Select selectQuery = QueryBuilder.select().all().from(keyspaceName, tableName);
            Where selectWhere = selectQuery.where();
            Clause clause = eq(key, value);
            selectWhere.and(clause);
            clause = gt(gtdateName, gtdate);
            selectWhere.and(clause);
            clause = lt(ltdateName, ltdate);
            selectWhere.and(clause);
            clause=gt("received_count",0);
            selectWhere.and(clause);
            ResultSet results =
                    connectionManager.getSession(keyspaceName).execute(selectQuery.allowFiltering());
            response = CassandraUtil.createResponse(results);
        } catch (Exception e) {
            throw new ProjectCommonException(ResponseCode.SERVER_ERROR.getErrorCode(),
                    ResponseCode.SERVER_ERROR.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> getPoints(String email_id) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        Integer leaderboard_year = cal.get(Calendar.YEAR);
        Integer duration_value = cal.get(Calendar.WEEK_OF_YEAR);
        propertyMap.put("leaderboard_year", leaderboard_year);
        propertyMap.put("leaderboard_type", "L");
        propertyMap.put("duration_type", "W");
        propertyMap.put("duration_value", duration_value);
        propertyMap.put("email_id", email_id);

        try {
            result = (List<Map<String, Object>>) userUtilService.getRecordsByProperties(bodhiKeyspace, leaderboard, propertyMap).getResult().get("response");
        } catch (Exception e) {
            result = new ArrayList<Map<String, Object>>();
            ProjectLogger.log("Error : " + e.getMessage(), e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> getBadges(String email_id) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(formatter.parse(formatter.format(new Date())));
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            Date ltDate = cal.getTime();
            cal.add(Calendar.DAY_OF_YEAR, -7);
            Date gtDate = cal.getTime();

            result = (List<Map<String, Object>>) getRecordsbetweenDateForUser(bodhiKeyspace, userBadges, "last_received_date", gtDate, "last_received_date", ltDate, "email_id", email_id).getResult().get("response");
        } catch (Exception e) {
            result = new ArrayList<Map<String, Object>>();
            ProjectLogger.log("Error : " + e.getMessage(), e);
        }
        return result;
    }
}
