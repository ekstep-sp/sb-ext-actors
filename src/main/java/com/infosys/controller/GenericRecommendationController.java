/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/*package com.infosys.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;import com.infosys.util.LexProjectUtil;import com.infosys.util.LexProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.responsecode.ResponseCode;
import com.infosys.model.ContentMeta;
import com.infosys.service.GenericRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

//import com.infosys.service.RecommendationServiceImpl;

@RestController
@CrossOrigin(origins = "*")
public class GenericRecommendationController {

	@Autowired
	GenericRecommendationService recommendationService;
		
	@GetMapping("/v1/course/popular")
    public Response getPopularCourses(@RequestParam(value = "resourceCount", defaultValue = "5") String numberOfResources) throws JsonParseException, JsonMappingException, IOException {
        Response resp = new Response();

        ProjectLogger.log("Popular courses request recieved");
	
		List<ContentMeta> popularCourses = recommendationService.getPopularCourses(Integer.parseInt(numberOfResources));
		 
		resp.setVer("v1");
		resp.setId("api.courses.popular");
		resp.setTs(ProjectUtil.getFormattedDate());
		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
		code.setResponseCode(ResponseCode.OK.getResponseCode());
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
		resp.setParams(params);
		// popular courses for successful response
		resp.put(Constants.RESPONSE, popularCourses);
		return resp;
		
	}
	
	@GetMapping("/v1/course/new")
    public Response getNewCourses(@RequestParam(value = "resourceCount", defaultValue = "5") String numberOfResources) throws JsonParseException, JsonMappingException, IOException {
        Response resp = new Response();

        ProjectLogger.log("New courses request recieved");
		
        
		List<ContentMeta> newCourses = recommendationService.getNewCourses(Integer.parseInt(numberOfResources));
		 
		resp.setVer("v1");
		resp.setId("api.courses.new");
		resp.setTs(ProjectUtil.getFormattedDate());
		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
		code.setResponseCode(ResponseCode.OK.getResponseCode());
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
		resp.setParams(params);
		// new courses for successful response
		resp.put(Constants.RESPONSE, newCourses);
		return resp;
	}

}
*/

