/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.model.ContentMeta;
import com.infosys.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class SearchController {

	@Autowired
	SearchService searchService;

	@GetMapping("v1/filters")
	public Response getAllFilters() {
		Response resp = new Response();
		// Map<String, Object> reqMap = requestBody.getRequest();
		// String query = (String) reqMap.get("query");
		// ProjectLogger.log("Create User request received as ", reqMap,
		// LoggerEnum.INFO.name());
		// Setting other attributes of response like version, timestamp, response param
		// etc.
		resp.setVer("v1");
		resp.setId("api.filters");
		resp.setTs(ProjectUtil.getFormattedDate());

		ResponseParams params = new ResponseParams();

		// Map<String,Map<String,Integer>> map = searchService.getFiltersAndCount();

		try {
			Map<String, Map<String, Integer>> filterMap = searchService.getFiltersAndCount();
			resp.put(Constants.RESPONSE, filterMap);

			ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
			code.setResponseCode(ResponseCode.OK.getResponseCode());
			params.setMsgid(ExecutionContext.getRequestId());
			params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
			resp.setParams(params);
			resp.setResponseCode(code);
		} catch (ProjectCommonException e) {
			resp.put(Constants.RESPONSE, "FAILED");
			ResponseCode code = ResponseCode.getResponse(e.getCode());
			System.out.println(e.getResponseCode());
			code.setResponseCode(e.getResponseCode());
			params.setMsgid(ExecutionContext.getRequestId());
			params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
			params.setErrmsg(e.getMessage());
			resp.setParams(params);
			resp.setResponseCode(code);
		}
		// Preparing and setting response param

		return resp;
	}

	@PostMapping("v1/search/resource")
	public Response search(@RequestBody Request requestBody) {
		Response resp = new Response();
		Map<String, Object> reqMap = requestBody.getRequest();
		// String query = (String) reqMap.get("query");
		ProjectLogger.log("Create User request received as ", reqMap, LoggerEnum.INFO.name());
		// Setting other attributes of response like version, timestamp, response param
		// etc.
		resp.setVer("v1");
		resp.setId("api.user.create");
		resp.setTs(ProjectUtil.getFormattedDate());

		ResponseParams params = new ResponseParams();

		// Map<String,Map<String,Integer>> map = searchService.getFiltersAndCount();

		try {
			List<ContentMeta> contentMetaList = searchService.searchAPI(requestBody);
			resp.put(Constants.RESPONSE, contentMetaList);

			ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
			code.setResponseCode(ResponseCode.OK.getResponseCode());
			params.setMsgid(ExecutionContext.getRequestId());
			params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
			resp.setParams(params);
			resp.setResponseCode(code);
		} catch (ProjectCommonException e) {
			resp.put(Constants.RESPONSE, "FAILED");
			ResponseCode code = ResponseCode.getResponse(e.getCode());
			System.out.println(e.getResponseCode());
			code.setResponseCode(e.getResponseCode());
			params.setMsgid(ExecutionContext.getRequestId());
			params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
			params.setErrmsg(e.getMessage());
			resp.setParams(params);
			resp.setResponseCode(code);
		}
		// Preparing and setting response param
		catch (IOException e) {
			resp.put(Constants.RESPONSE, "FAILED");
			ResponseCode code = ResponseCode.getResponse("500");
			System.out.println(500);
			code.setResponseCode(500);
			params.setMsgid(ExecutionContext.getRequestId());
			params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
			params.setErrmsg(e.getMessage());
			resp.setParams(params);
			resp.setResponseCode(code);
		}

		return resp;
	}
}