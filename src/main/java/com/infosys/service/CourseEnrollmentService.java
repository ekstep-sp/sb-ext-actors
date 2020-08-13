/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import com.infosys.cassandra.CassandraOperation;
import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.helper.ServiceFactory;
import com.infosys.model.ContentMeta;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;
import org.springframework.stereotype.Service;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class CourseEnrollmentService {

	private static final String DEFAULT_BATCH_ID = "1";
	 private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
	    private PropertiesCache properties = PropertiesCache.getInstance();
	    private String keyspace = properties.getProperty(JsonKey.DB_KEYSPACE);
	    
	    public CourseEnrollmentService() {
	        Util.checkCassandraDbConnections(keyspace);
	    }
	
	public void enrollToCourse(Request request) throws Exception
	{
		 Util.DbInfo courseEnrollmentdbInfo = Util.dbInfoMap.get(JsonKey.LEARNER_COURSE_DB);
         Util.DbInfo batchDbInfo = Util.dbInfoMap.get(JsonKey.COURSE_BATCH_DB);
         
         Map<String, Object> courseMap = (Map<String, Object>) request.get(JsonKey.COURSE);
         
         if (ProjectUtil.isNull(courseMap.get(JsonKey.BATCH_ID))) {
             courseMap.put(JsonKey.BATCH_ID, DEFAULT_BATCH_ID);
           } else {
             Response response = cassandraOperation.getRecordById(batchDbInfo.getKeySpace(),
                 batchDbInfo.getTableName(), (String) courseMap.get(JsonKey.BATCH_ID));
             List<Map<String, Object>> responseList =
                 (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
             // Checking batch existence
             if (responseList.isEmpty()) {
               throw new ProjectCommonException(ResponseCode.invalidCourseBatchId.getErrorCode(),
                   ResponseCode.invalidCourseBatchId.getErrorMessage(),
                   ResponseCode.CLIENT_ERROR.getResponseCode());
             }
           }
         
         Response dbResult = cassandraOperation.getRecordById(courseEnrollmentdbInfo.getKeySpace(),
                 courseEnrollmentdbInfo.getTableName(), generateUserCoursesPrimaryKey(courseMap));
             List<Map<String, Object>> dbList =
                 (List<Map<String, Object>>) dbResult.get(JsonKey.RESPONSE);
             if (!dbList.isEmpty()) {
               ProjectLogger.log("User Already Enrolled Course ");
//               ProjectCommonException exception = new ProjectCommonException(
//                   ResponseCode.userAlreadyEnrolledCourse.getErrorCode(),
//                   ResponseCode.userAlreadyEnrolledCourse.getErrorMessage(),
//                   ResponseCode.CLIENT_ERROR.getResponseCode());
               throw new Exception("User Already Enrolled");
             }
             
             Map<String,Object> contentMetaMap = getCourseMeta((String) courseMap.get(JsonKey.COURSE_ID));
             ContentMeta contentMeta = ContentMeta.fromMap(contentMetaMap);
             
             Timestamp ts = new Timestamp(new Date().getTime());
             courseMap.put(JsonKey.COURSE_LOGO_URL, contentMeta.getAppIcon());
             courseMap.put(JsonKey.CONTENT_ID, courseMap.get(JsonKey.COURSE_ID) );
             courseMap.put(JsonKey.COURSE_NAME, contentMeta.getName());
             courseMap.put(JsonKey.DESCRIPTION,contentMeta.getDescription());
             //courseMap.put(JsonKey.ADDED_BY, addedBy);
             courseMap.put(JsonKey.COURSE_ENROLL_DATE, ProjectUtil.getFormattedDate());
             courseMap.put(JsonKey.ACTIVE, ProjectUtil.ActiveStatus.ACTIVE.getValue());
             courseMap.put(JsonKey.STATUS, ProjectUtil.ProgressStatus.NOT_STARTED.getValue());
             courseMap.put(JsonKey.DATE_TIME, ts);
             courseMap.put(JsonKey.ID, generateUserCoursesPrimaryKey(courseMap));
             courseMap.put(JsonKey.COURSE_PROGRESS, 0);
           //  courseMap.put(JsonKey.LEAF_NODE_COUNT, ekStepContent.get(JsonKey.LEAF_NODE_COUNT));
             Response result = cassandraOperation.insertRecord(courseEnrollmentdbInfo.getKeySpace(),
                     courseEnrollmentdbInfo.getTableName(), courseMap);
        insertDataToElastic(LexProjectUtil.EsIndex.sunbird.getIndexName(),
                LexProjectUtil.EsType.usercourses.getTypeName(), (String) courseMap.get(JsonKey.ID), courseMap);
             
	}
	
	private boolean insertDataToElastic(String index, String type, String identifier,
		      Map<String, Object> data) {
		    ProjectLogger
		        .log("making call to ES for type ,identifier ,data==" + type + " " + identifier + data);
           /* if (type.equalsIgnoreCase(LexProjectUtil.EsType.user.getTypeName())) {
              // now calculate profile completeness and error filed and store it in ES
		      ProfileCompletenessService service = ProfileCompletenessFactory.getInstance();
		      Map<String, Object> responsemap = service.computeProfile(data);
		      data.putAll(responsemap);
		    }*/
		    String response = ElasticSearchUtil.createData(index, type, identifier, data);
		    ProjectLogger.log("Getting ES save response for type , identiofier==" + type + "  " + identifier
		        + "  " + response);
		    if (!ProjectUtil.isStringNullOREmpty(response)) {
		      ProjectLogger.log("User Data is saved successfully ES ." + type + "  " + identifier);
		      return true;
		    }
		    ProjectLogger.log("unbale to save the data inside ES with identifier " + identifier,
		        LoggerEnum.INFO.name());
		    return false;
		  }
	
	
	private String generateUserCoursesPrimaryKey(Map<String, Object> req) {
	    String userId = (String) req.get(JsonKey.USER_ID);
	    String courseId = (String) req.get(JsonKey.COURSE_ID);
	    String batchId = (String) req.get(JsonKey.BATCH_ID);
	    return OneWayHashing.encryptVal(userId + JsonKey.PRIMARY_KEY_DELIMETER + courseId
	        + JsonKey.PRIMARY_KEY_DELIMETER + batchId);
	  }
	
	public Map<String,Object> getCourseMeta(String courseId) throws IOException
	{

        Map<String, Object> mappedObject = ElasticSearchUtil.getDataByIdentifier(LexProjectUtil.EsIndex.bodhi.getIndexName(), LexProjectUtil.EsType.resource.getTypeName(), courseId);
        return mappedObject;

    }
	
}
