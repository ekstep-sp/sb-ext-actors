/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.service.LearningHistoryService;

@RestController
@CrossOrigin(origins = "*")
public class LearningHistoryController {

	@Autowired
	LearningHistoryService lhService;

	@GetMapping("/v2/users/{email_id}/dashboard/courses")
	public ResponseEntity<Response> getContent(@PathVariable("email_id") String emailId,
			@RequestParam(defaultValue = "0", required = false, name = "pagenumber") String pageNumber,
			@RequestParam(defaultValue = "10", required = false, name = "pagesize") String pageSize,
			@RequestParam(defaultValue = "inprogress", required = false, name = "status") String status,
			@RequestParam(defaultValue = "1", required = false, name = "version") String version,
			@RequestParam(defaultValue = "0", required = false, name = "contenttype") String contentType){
		String url ="/v2/users/" + emailId + "/dashboard/courses";
		System.out.println(pageNumber);
		Response resp = new Response();
		resp.setVer("v2");
		resp.setId("api.courses");
		resp.setTs(ProjectUtil.getFormattedDate());
		HttpStatus httpStatus = null;
		Long start = System.currentTimeMillis();
		try {
			resp.put(Constants.RESPONSE,
					lhService.getUserCourseProgress(emailId, Integer.parseInt(pageNumber), Integer.parseInt(pageSize), status, Integer.parseInt(version), contentType));
			httpStatus = HttpStatus.OK;
		} catch(NumberFormatException nfe) {
			httpStatus = HttpStatus.BAD_REQUEST;
			resp.put("errmsg","Bad Request");
			resp.setResponseCode(ResponseCode.CLIENT_ERROR);
		}
		catch (Exception e) {
			httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
			resp.put("errmsg",e.getMessage());
			resp.setResponseCode(ResponseCode.internalError);
		}
		//ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return new ResponseEntity<Response>(resp,httpStatus);
	}

	@PostMapping("/v2/users/{email_id}/dashboard/courses/details")
	public Response getContentListData(@PathVariable("email_id") String emailId,
			@RequestBody List<String> requestBody) {
		String url ="/v2/users/"+emailId+"/dashboard/courses/details";
		Response resp = new Response();
		List<String> request = (List<String>) requestBody;
		resp.setVer("v2");
		resp.setId("api.courses.details");
		resp.setTs(ProjectUtil.getFormattedDate());
		Long start = System.currentTimeMillis();
		try {
			resp.put(Constants.RESPONSE, lhService.getUserContentListProgress(emailId, request));
		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}
		//ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return resp;
	}

	@GetMapping("/v2/users/{email_id}/contentlist/progress")
	public Response getContentProgress(@PathVariable("email_id") String emailId,
			@RequestParam(defaultValue = "", required = false, name = "contentIds") String contentIds) {
		String url = "/v2/users/" + emailId  + "/contentlist/progress";

		Response resp = new Response();
		resp.setVer("v2");
		resp.setId("api.contentlist.progress");
		resp.setTs(ProjectUtil.getFormattedDate());
		Long start = System.currentTimeMillis();
		try {
			if (contentIds.isEmpty())
				resp.put(Constants.RESPONSE, lhService.getContentListProgress(emailId, new ArrayList<String>()));
			else
				resp.put(Constants.RESPONSE, lhService.getContentListProgress(emailId, Arrays.asList(contentIds.split(","))));
		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}
		//ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return resp;
	}

}
