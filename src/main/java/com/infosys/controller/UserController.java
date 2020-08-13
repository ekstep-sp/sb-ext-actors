/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

import com.infosys.exception.BadRequestException;
import com.infosys.service.UserService;
import com.infosys.util.LexConstants;

@RestController
@CrossOrigin(origins = "*")
public class UserController {

	@Autowired
	UserService userService;

	@GetMapping("/user/toemails")
	public ResponseEntity<Response> convertToEmails(@RequestParam(name = "ids") List<String> ids){
		Response response = new Response();
		response.setVer("v1");
		response.setId("api.user.toemails");
		response.setTs(ProjectUtil.getFormattedDate());
		try {
			Map<String, String> responseMap = userService.toEmails(ids);
			response.put("result", responseMap);
		} catch (BadRequestException badRequestException) {
			response.put("error", badRequestException.getMessage());
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		} catch (Exception exception) {
			response.put("error", exception.getMessage());
			return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		response.put(Constants.RESPONSE, "SUCCESS");
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
	
	@PostMapping("/v1/user/finduuids")
	public ResponseEntity<Response> findUuids(@RequestBody List<String> emails){
		Response response = new Response();
		response.setVer("v1");
		response.setId("api.user.finduuids");
		response.setTs(ProjectUtil.getFormattedDate());
		try {
			Map<String, Object> responseMap = userService.findUuids(emails);
			response.put("result", responseMap);
		} catch (BadRequestException badRequestException) {
			response.put("error", badRequestException.getMessage());
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		} catch (Exception exception) {
			response.put("error", exception.getMessage());
			return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		response.put(Constants.RESPONSE, "SUCCESS");
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
	

	@GetMapping("/v1/user/finduuid")
	public ResponseEntity<Response> findUUID(
			@RequestParam(required = true, name = LexConstants.USER_EMAIL) String email) {
		Response response = new Response();
		response.setVer("v1");
		response.setId("api.user.finduuid");
		response.setTs(ProjectUtil.getFormattedDate());
		try {
			Map<String, Object> responseMap = userService.findUUID(email);
			response.put("result", responseMap);
		} catch (BadRequestException badRequestException) {
			response.put("error", badRequestException.getMessage());
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		} catch (Exception exception) {
			response.put("error", exception.getMessage());
			return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		response.put(Constants.RESPONSE, "SUCCESS");
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}

	@PostMapping("/v1/user/create")
	public ResponseEntity<Response> createUser(@RequestBody Request req) {
		Response resp = new Response();
		Map<String, Object> reqMap = req.getRequest();
		ProjectLogger.log("Create User request received as ", reqMap, LoggerEnum.INFO.name());
		// Setting other attributes of response like version, timestamp, response param
		// etc.
		resp.setVer("v1");
		resp.setId("api.user.create");
		resp.setTs(ProjectUtil.getFormattedDate());
		resp.put(Constants.RESPONSE, "SUCCESS");
		ResponseParams params = new ResponseParams();

		try {
			Response response = userService.createUser(reqMap);
			ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
			code.setResponseCode(ResponseCode.OK.getResponseCode());
			params.setMsgid(ExecutionContext.getRequestId());
			params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
			resp.setParams(params);
			resp.setResponseCode(code);
			return new ResponseEntity<>(resp, HttpStatus.OK);
		} catch (ProjectCommonException e) {
			resp.put(Constants.RESPONSE, "FAILED");
			ResponseCode code = ResponseCode.getResponse(e.getCode());
			params.setMsgid(ExecutionContext.getRequestId());
			params.setErrmsg(e.getMessage());
			resp.setParams(params);
			resp.setResponseCode(code);
			return new ResponseEntity<Response>(resp, HttpStatus.valueOf(e.getResponseCode()));
		} catch (IOException e) {
			resp.put(Constants.RESPONSE, "FAILED");
			ResponseCode code = ResponseCode.getResponse("500");
			params.setMsgid(ExecutionContext.getRequestId());
			params.setErrmsg(e.getMessage());
			resp.setParams(params);
			resp.setResponseCode(code);
			return new ResponseEntity<Response>(resp, HttpStatus.valueOf(500));
		}

	}

	@GetMapping("/v1/users/{userId}/separation")
	public Response getSeparationDate(@PathVariable String userId) {
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.user.getSeperationDate");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			Map<String, Object> res = userService.getSeparationDate(userId);
			resp.putAll(userService.getSeparationDate(userId));
			resp.setResponseCode(ResponseCode.success);
		} catch (Exception e) {
			resp.setResponseCode(ResponseCode.RESOURCE_NOT_FOUND);
			resp.put("msg", "The Entered User_Id is invalid");
		}
		return resp;

	}
}
