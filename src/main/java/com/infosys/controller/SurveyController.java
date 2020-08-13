/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.exception.BadRequestException;
import com.infosys.service.SurveyService;

@RestController
public class SurveyController {

	@Autowired
	SurveyService surveyService;

	@PostMapping("v1/survey")
	public ResponseEntity<Response> submitSurvey(@RequestBody Request request) {
		Response response = new Response();
		response.setVer("v1");
		response.setId("api.submit.survey");
		response.setTs(ProjectUtil.getFormattedDate());
		try {
			surveyService.submitSurvey(request);
		} catch (BadRequestException bre) {
			System.out.println(bre.getMessage());
			ProjectLogger.log(bre.getMessage(), bre);
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.CLIENT_ERROR.getErrorCode());
			response.setResponseCode(responseCode);
			response.put("error", bre.getMessage());
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			ProjectLogger.log(e.getMessage(), e);
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.internalError.getErrorCode());
			response.setResponseCode(responseCode);
			response.put("error", "Something went wrong");
			return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
}
