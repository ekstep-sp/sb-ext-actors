/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.BadRequestException;
import com.infosys.service.AssessmentService;

@RestController
@CrossOrigin(origins = "*")
public class AssessmentController {

	@Autowired
	AssessmentService assessmentService;

	@PostMapping("/v1/user/{userEmail}/assessment/submit")
	public Response gaeMultipleUserData(@RequestBody Request requestBody, @PathVariable("userEmail") String userEmail) {
		Response resp = new Response();
		String url = "/v1/user/" + userEmail + "/assessment/submit";
		Map<String, Object> request = requestBody.getRequest();
		resp.setVer("v1");
		resp.setId("api.assessment.submit");
		resp.setTs(ProjectUtil.getFormattedDate());
		Long start = System.currentTimeMillis();
		
		try {
			
			resp.put(Constants.RESPONSE, assessmentService.submitAssessment(request));
			
		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}
		//ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return resp;
	}

	/*
	 * Controller to a post request to Submit AssessmentData Request Body will be
	 * Given through an iframe The data will be validated and Inserted into
	 * user_assessment_master table
	 */
	@PostMapping("/v1/assessment/submitbyIframe")
	public Response submitAssessmentByIframe(@RequestBody Request requestBody) {
		System.out.println("Hello from iframe");
		String url = "/v1/assessment/submitbyIframe";
		Response resp = new Response();
		Map<String, Object> request = requestBody.getRequest();
		resp.setVer("v1");
		resp.setId("api.assessment.submitByIframe");
		resp.setTs(ProjectUtil.getFormattedDate());
		Long start =  System.currentTimeMillis();
		try {
			// calling the service to return a response
			resp.put(Constants.RESPONSE, assessmentService.submitAssessmentByIframe(request));
		} catch (BadRequestException e) {
			resp.put("errorMsg", e.getMessage());
			resp.setResponseCode(ResponseCode.invalidRequestData);
		} catch (Exception e) {
			resp.put("errorMsg", e.getMessage());
			resp.setResponseCode(ResponseCode.internalError);
		}
		//ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return resp;

	}

	/*
	 * Controller to a get request to Fetch AssessmentData the request requires
	 * user_id and course_id returns a JSON of processesd data and list of
	 * Assessments Given
	 */
	@GetMapping("/v1/content/{course_id}/user/{user_id}/assessment")
	public ResponseEntity<Response> getAssessmentByContentUser(@PathVariable String course_id,
			@PathVariable String user_id) {
		Response resp = new Response();
		HttpStatus httpStatus = null;
		resp.setVer("v1");
		resp.setId("api.assessment.getAssessmentByContentUser");
		resp.setTs(ProjectUtil.getFormattedDate());
			// calling the service to return a response
		Date startTime = new Date();
		try {
			
			resp.put(Constants.RESPONSE, assessmentService.getAssessmentByContentUser(course_id, user_id));
			ProjectLogger.log("Fetching of Assessment Information Started at " + startTime,LoggerEnum.INFO);
			ProjectLogger.log("Fetching of Assessment Information Ended at " + new Date(),LoggerEnum.INFO);
			resp.setResponseCode(ResponseCode.OK);
			httpStatus = HttpStatus.OK;
		}catch (BadRequestException e) {
			resp.put("errmsg",e.getMessage());
			httpStatus = HttpStatus.BAD_REQUEST;	
		}
		catch(ApplicationLogicError e){
			resp.put("errmsg",e.getMessage());
			httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return new ResponseEntity<Response>(resp, httpStatus);
	}
}
