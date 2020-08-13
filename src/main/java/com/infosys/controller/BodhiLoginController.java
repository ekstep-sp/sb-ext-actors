/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.service.LoginService;
import com.infosys.util.LexProjectUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

@RestController
public class BodhiLoginController {

	@Autowired
	LoginService loginService;

	@PostMapping("v1/user/createUser")
	public ResponseEntity<Response> createUser(@RequestBody Request requestBody) {
		Response resp = new Response();
		Map<String, Object> reqMap = requestBody.getRequest();
		ProjectLogger.log("Create User started at " + new Date() + "request received as ", reqMap,
				LoggerEnum.INFO.name());
		// Setting other attributes of response like version, timestamp, response param
		// etc.
		resp.setVer("v1");
		resp.setId("api.user.create");
		resp.setTs(ProjectUtil.getFormattedDate());
		resp.put(Constants.RESPONSE, "SUCCESS");
		ResponseParams params = new ResponseParams();
		String url = "v1/user/createUser";
		Long start = System.currentTimeMillis();
		try {
			boolean response = loginService.createUser(requestBody);
			ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
			code.setResponseCode(ResponseCode.OK.getResponseCode());
			params.setMsgid(ExecutionContext.getRequestId());
			params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
			resp.setParams(params);
			resp.setResponseCode(code);
			return new ResponseEntity<Response>(resp, HttpStatus.OK);
		} catch (ProjectCommonException e) {
			e.printStackTrace();
			resp.put(Constants.RESPONSE, "FAILED");
			// e.printStackTrace();
			/*
			 * ResponseCode code = ResponseCode.getResponse(e.getCode());
			 * System.out.println(e.getResponseCode()); //
			 * code.setResponseCode(e.getResponseCode());
			 * params.setMsgid(ExecutionContext.getRequestId());
			 * params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).
			 * name()); params.setErrmsg(e.getMessage()); resp.setParams(params); //
			 * resp.setResponseCode(code);
			 */
		} catch (IOException e) {
			e.printStackTrace();
			resp.put(Constants.RESPONSE, "FAILED");
		}
		// Preparing and setting response param
		// ProjectLogger.log(url +":" +
		// LexProjectUtil.getFormattedTime(System.currentTimeMillis() -
		// start),LoggerEnum.INFO);
		return new ResponseEntity<Response>(resp, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}