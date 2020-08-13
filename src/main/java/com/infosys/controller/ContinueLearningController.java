/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.cassandra.CassandraOperation;
import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.BadRequestException;
import com.infosys.helper.ServiceFactory;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;

@RestController
public class ContinueLearningController {

	private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private Util.DbInfo continueLearningConfig = Util.dbInfoMap.get(LexJsonKey.CONTINUE_LEARNING_TABLE);

	@PostMapping(path="/continue/{userEmail:.+}")
	public ResponseEntity<Map<String, Object>> cassandraBug(@PathVariable("userEmail") String userEmail, @RequestBody Request requestBody) {
//		String url = "/continue/"+ userEmail;
		Map<String, Object> reqMap = requestBody.getRequest();

        if(reqMap == null || reqMap.isEmpty()){
            throw new BadRequestException("Request body is null or empty");
        }
        
        String contentPathId = (String) reqMap.get("contextPathId");
        String resourceId = (String) reqMap.get("resourceId");
        
        if(resourceId==null || contentPathId==null || resourceId.isEmpty() || contentPathId.isEmpty()){
            throw new BadRequestException("Request body should contain contextPathId and resourceId");
        }

//        Long start = System.currentTimeMillis();      
		Map<String, Object> continueLearningMap = new HashMap<String, Object>();
		continueLearningMap.put("user_email", userEmail);
		//System.out.println(reqMap);
		continueLearningMap.put("context_path_id", contentPathId);
		continueLearningMap.put("resource_id", resourceId);
		

		long dateAccessed = System.currentTimeMillis();
		if (reqMap.get("dateAccessed") != null) {
			dateAccessed = (long) reqMap.get("dateAccessed");
		}
		continueLearningMap.put("date_accessed", dateAccessed);
		
		String data = "";
		if (reqMap.get("data") != null) {
			data = (String) reqMap.get("data");
		}
		continueLearningMap.put("data", data);
		
		try {
			//System.out.println(continueLearningMap.toString());
			cassandraOperation.upsertRecord(continueLearningConfig.getKeySpace(), continueLearningConfig.getTableName(), continueLearningMap);
			//System.out.println(res.getResult().toString());
		} catch (ProjectCommonException pce) {
			if (pce.getCode().equals(ResponseCode.SERVER_ERROR.getErrorCode())) {
				throw new ApplicationLogicError("Error while saving data for continue learning");
			} else {
				throw new ApplicationLogicError("Internal Server Error");
			}
		}
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return new ResponseEntity<Map<String,Object>>(continueLearningMap,HttpStatus.OK);
	}
}