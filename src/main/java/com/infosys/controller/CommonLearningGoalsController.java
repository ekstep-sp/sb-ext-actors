/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at 
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.exception.BadRequestException;
import com.infosys.service.CommonLearningGoalsService;
import com.infosys.util.LexConstants;
import com.infosys.util.LexProjectUtil;

@RestController
@CrossOrigin(origins = "*")
public class CommonLearningGoalsController {

	@Autowired
	CommonLearningGoalsService commonLearningGoalsService;

	@PostMapping("/v1//commongoals/add")
	public ResponseEntity<Response> createCommonLearningGoal(@RequestBody Request request) {
		Response response = new Response();
		String url = "/v1//commongoals/add";
		Long start = System.currentTimeMillis(); 
		try {
			response.setVer("v1");
			response.setId("api.commongoals.add");
			response.setTs(ProjectUtil.getFormattedDate());
			Map<String, Object> requestMap = request.getRequest();
			 
			commonLearningGoalsService.createCommonGoal(requestMap);
		} catch (BadRequestException badRequestException) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.CLIENT_ERROR.getErrorCode());
			response.setResponseCode(responseCode);
			response.put("error", badRequestException.getMessage());
			response.put(Constants.RESPONSE, "FAILED");
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		} catch (Exception exception) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.internalError.getErrorCode());
			response.setResponseCode(responseCode);
			response.put("error", exception.getMessage());
			response.put(Constants.RESPONSE, "FAILED");
			return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		response.put(Constants.RESPONSE, "SUCCESS");
		//ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return new ResponseEntity<Response>(response, HttpStatus.OK);
		
	}
}
