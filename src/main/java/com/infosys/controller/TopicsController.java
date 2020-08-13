/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.model.Topic;
import com.infosys.service.TopicService;

@RestController
@CrossOrigin(origins = "*")
public class TopicsController {

	@Autowired
	TopicService topicService;

	@GetMapping("/v1/topics/read")
	public Response getAllTopics(@RequestParam(value = "size", defaultValue = "10") int size) {
		Response resp = new Response();
		ProjectLogger.log("Topics read request received", LoggerEnum.INFO.name());
		// Response map containing info to be sent as result along with other info
		Map<String, Object> respMap = new HashMap<>();

		List<Topic> topicList = topicService.getTopics(size);
		respMap.put(JsonKey.TOPICS, topicList);
		// Setting other attributes of response like version, timestamp, response param
		// etc.
		resp.setVer("v1");
		resp.setId("api.topics.read");
		resp.setTs(ProjectUtil.getFormattedDate());
		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
		code.setResponseCode(ResponseCode.OK.getResponseCode());
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
		resp.setParams(params);
		// Response success for logout
		resp.put(Constants.RESPONSE, respMap);
		return resp;
	}

	@GetMapping("/v1/topics/recommended")
	public Response getRecommendedTopics(@RequestParam(value = "q", defaultValue = "popular") String type) throws IOException {
		Response resp = new Response();
		ProjectLogger.log("Recommended topics read request received for type " + type, LoggerEnum.INFO.name());
		// Response map containing info to be sent as result along with other info
		Map<String, Object> respMap = new HashMap<>();

		List<Map<String, Object>> topicList = null;
		if (type.equals("new")) {
			topicList = topicService.getNewTopics();
		} else {
			topicList = topicService.getTopTopics();
		}
		respMap.put(JsonKey.TOPICS, topicList);
		// Setting other attributes of response like version, timestamp, response param
		// etc.
		resp.setVer("v1");
		resp.setId("api.topics.recommended");
		resp.setTs(ProjectUtil.getFormattedDate());
		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
		code.setResponseCode(ResponseCode.OK.getResponseCode());
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
		resp.setParams(params);
		// Response success for logout
		resp.put(Constants.RESPONSE, respMap);
		return resp;
	}

	@PostMapping("v1/user/topic/add")
	public ResponseEntity<Response> addUserTopic(@RequestBody Request req) {
		Response response = new Response();
		Map<String, Object> reqMap = req.getRequest();
		ProjectLogger.log("Add topic request received as ", reqMap, LoggerEnum.INFO.name());
		// Setting other attributes of response like version, timestamp, response param
		// etc.
		response.setVer("v1");
		response.setId("api.user.topic.add");
		response.setTs(ProjectUtil.getFormattedDate());

		String userId = (String) reqMap.get(JsonKey.USER_ID);
		List<String> topicList = (ArrayList<String>) reqMap.get(JsonKey.TOPICS);
		String responseCode = "SUCCESS";

		if (topicService.addUserTopics(userId, topicList) == false) {
			responseCode = "FAILED";
			ResponseCode errorCode = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
			errorCode.setResponseCode(ResponseCode.internalError.getResponseCode());
			ResponseParams errorParams = new ResponseParams();
			errorParams.setMsgid(ExecutionContext.getRequestId());
			errorParams.setStatus(ResponseCode.getHeaderResponseCode(errorCode.getResponseCode()).name());
			response.setParams(errorParams);
			return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
		code.setResponseCode(ResponseCode.OK.getResponseCode());
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
		response.setParams(params);
		// Response success for topic add
		response.put(Constants.RESPONSE, responseCode);
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}

	@GetMapping("v1/user/topic/read/{uid}")
	public Response getUserTopic(@PathVariable("uid") String userId) {
		Response resp = new Response();
		ProjectLogger.log("Read topic request received for user ", userId, LoggerEnum.INFO.name());
		// Response map containing info to be sent as result along with other info
		Map<String, Object> respMap = new HashMap<>();

		List<Topic> topicList = topicService.getUserTopics(userId);
		respMap.put(JsonKey.TOPICS, topicList);
		// Setting other attributes of response like version, timestamp, response param
		// etc.
		resp.setVer("v1");
		resp.setId("api.user.topic.read");
		resp.setTs(ProjectUtil.getFormattedDate());
		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
		code.setResponseCode(ResponseCode.OK.getResponseCode());
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
		resp.setParams(params);
		// Response sent back
		resp.put(Constants.RESPONSE, respMap);
		return resp;
	}

	// Add will act as update
	// @PatchMapping("v1/user/topic/update")
	// public Response updateUserTopic(@RequestBody Request req) {
	// Response resp = new Response();
	// Map<String, Object> reqMap = req.getRequest();
	// ProjectLogger.log("Update topic request received as ", reqMap,
	// LoggerEnum.INFO.name());
	// //Setting other attributes of response like version, timestamp, response
	// param etc.
	// resp.setVer("v1");
	// resp.setId("api.user.topic.update");
	// resp.setTs(ProjectUtil.getFormattedDate());
	// ResponseCode code =
	// ResponseCode.getResponse(ResponseCode.success.getErrorCode());
	// code.setResponseCode(ResponseCode.OK.getResponseCode());
	// //Preparing and setting response param
	// ResponseParams params = new ResponseParams();
	// params.setMsgid(ExecutionContext.getRequestId());
	// params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
	// resp.setParams(params);
	// //Response success for topic add
	// resp.put(Constants.RESPONSE, "SUCCESS");
	// return resp;
	// }
}
