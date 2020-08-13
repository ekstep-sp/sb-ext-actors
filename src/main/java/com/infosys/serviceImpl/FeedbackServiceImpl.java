/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.infosys.cassandra.CassandraOperation;
import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.helper.ServiceFactory;
import com.infosys.service.FeedbackService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;
import org.springframework.stereotype.Service;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private PropertiesCache properties = PropertiesCache.getInstance();
    private String keyspace = properties.getProperty(JsonKey.DB_KEYSPACE);
    private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
    private Util.DbInfo userDb = Util.dbInfoMap.get(JsonKey.USER);
    private Util.DbInfo userTopicsDb = Util.dbInfoMap.get(LexJsonKey.USER_TOPICS_MAPPING_DB);
    private Util.DbInfo topicsDb = Util.dbInfoMap.get(LexJsonKey.TOPICS_DB);
    private Util.DbInfo feedbackDb = Util.dbInfoMap.get(LexJsonKey.USER_FEEDBACK_DB);
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    public FeedbackServiceImpl() {
        Util.checkCassandraDbConnections(keyspace);
        Util.checkCassandraDbConnections(bodhiKeyspace);
    }


    @Override
    public Response addFeedback(Map<String, Object> Requestfeedback) {
        Response contentResponse = new Response();
        String userId = null;
        String ratingString = null;
        String feedbackType = null;
        List<Map<String, String>> feedback = null;
        Timestamp timestamp = null;
        String nowAsISO;
        String subType;
        String contentId;
        Float rating;
        String identifier;
        try {
            userId = (String) Requestfeedback.get("userId");
            //System.out.println(userId);
            ratingString = (String) Requestfeedback.get("rating");
            if (ratingString != null)
                rating = Float.valueOf(ratingString);
            else
                rating = null;
            //System.out.println(rating);
            feedbackType = (String) Requestfeedback.get("feedbackType");
            subType = (String) Requestfeedback.get("feedbackSubtype");
            contentId = (String) Requestfeedback.get("contentId");
            
            //System.out.println(feedbackType);
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
            identifier = UUID.randomUUID().toString();
            df.setTimeZone(tz);
            nowAsISO = df.format(new Date());
            feedback = (List<Map<String, String>>) Requestfeedback.get("feedback");
            
        } catch (ProjectCommonException e) {
            ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
            //responseCode.setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
            contentResponse.setResponseCode(responseCode);
            //System.out.println(e.getMessage());
            return contentResponse;
        }
        Map<String, Object> objectToBeInserted = new HashMap<String, Object>();
        
        
        
        
        //System.out.println(feedback.toString());
        // System.out.println(timestamp);

        objectToBeInserted.put("user_id", userId);
        objectToBeInserted.put("rating", rating);
        objectToBeInserted.put("feedback_type", feedbackType);
        objectToBeInserted.put("timestamp", nowAsISO);
        objectToBeInserted.put("feedback", feedback);
        //request.put("identifier", "1");
        objectToBeInserted.put("content_id", contentId);
        objectToBeInserted.put("feedback_subtype", subType);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        list.add(objectToBeInserted);
        System.out.println(nowAsISO);

       // System.out.println("cassandra operation started");
       
        try
        {
            ElasticSearchUtil.upsertData(LexProjectUtil.EsIndex.lex_user_feedback.getIndexName(), LexProjectUtil.EsType.feedback.getTypeName(), identifier, objectToBeInserted);
        //System.out.println("executed");
        }
        catch(Exception e)
        {
        	ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.dbInsertionError.getErrorCode());
			//responseCode.setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
			contentResponse.setResponseCode(responseCode);
			//System.out.println(e.getMessage());
			return contentResponse;

        }
        // System.out.println("cassandra operation ended");
        ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
        responseCode.setResponseCode(ResponseCode.OK.getResponseCode());

        return contentResponse;
    }

    @Override
    public Map<String, Object> getFeedbackForCourse(String courseId) {
        Map<String, Object> resp = new HashMap<>();
        List<Map<String, Object>> feedList = new ArrayList<>();
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("rating", 4);
        feedback.put("comment", "Very good course, More quiz needed");
        feedback.put("userId", "uid1");
        feedList.add(feedback);
        feedback.put("rating", 5);
        feedback.put("comment", "Awesome learning. Thanks!!");
        feedback.put("userId", "uid2");
        feedList.add(feedback);
        resp.put("avgRating", 4.5);
        resp.put("feedback", feedList);
        return resp;
    }
}
