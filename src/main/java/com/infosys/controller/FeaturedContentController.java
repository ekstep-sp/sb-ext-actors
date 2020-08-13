/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.infosys.service.FeaturedContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.responsecode.ResponseCode;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class FeaturedContentController {
   
	@Autowired
	FeaturedContentService featuredContentService;
	
	@GetMapping("/v1/course/featured")
	public Response getFeaturedCourseContent() throws JsonParseException, JsonMappingException, IOException
	{
		    Response resp = new Response();
	        ProjectLogger.log("featured content request received");
	        Date startTime = new Date();
	        Map<String,Object> respMap = featuredContentService.getFeaturedCourseContent();
	        
	        //Setting other attributes of response like version, timestamp, response param etc.
	        resp.setVer("v1");
	        resp.setId("api.course.featured");
	        resp.setTs(ProjectUtil.getFormattedDate());
	        ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
	        code.setResponseCode(ResponseCode.OK.getResponseCode());
	        //Preparing and setting response param
	        ResponseParams params = new ResponseParams();
	        params.setMsgid(ExecutionContext.getRequestId());
	        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
	        resp.setParams(params);
	        //Finally adding featured course content to response
	        resp.put(Constants.RESPONSE, respMap);
	        ProjectLogger.log("Fetching Featured Courses started at " + startTime,LoggerEnum.INFO);
	        ProjectLogger.log("Fetching Featured Courses ended at " + new Date(),LoggerEnum.INFO);	        
	        return resp;
	        
	
	}
	
	
	@GetMapping("/v1/resource/featured")
	public Response getFeaturedContent() throws JsonParseException, JsonMappingException, IOException
	{
		    Response resp = new Response();
	        ProjectLogger.log("featured content request received");
	       
	        Map<String,Object> respMap = featuredContentService.getFeaturedResourceContent();
	        Date startTime = new Date();
	        //Setting other attributes of response like version, timestamp, response param etc.
	        resp.setVer("v1");
	        resp.setId("api.resource.featured");
	        resp.setTs(ProjectUtil.getFormattedDate());
	        ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
	        code.setResponseCode(ResponseCode.OK.getResponseCode());
	        //Preparing and setting response param
	        ResponseParams params = new ResponseParams();
	        params.setMsgid(ExecutionContext.getRequestId());
	        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
	        resp.setParams(params);
	        //Finally adding featured resource content to response
	        resp.put(Constants.RESPONSE, respMap);
	        ProjectLogger.log("Fetching Featured Resources started at " + startTime,LoggerEnum.INFO);
	        ProjectLogger.log("Fetching Featured Resources ended at " + new Date(),LoggerEnum.INFO);
	        return resp;
	        
	
	}
	
	
	
	
}
