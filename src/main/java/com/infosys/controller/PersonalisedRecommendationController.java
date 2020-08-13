/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.infosys.service.PersonalisedRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.responsecode.ResponseCode;

import java.io.IOException;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class PersonalisedRecommendationController {
   
	@Autowired
	PersonalisedRecommendationService personalisedRecommendationService;
	
	@GetMapping("/v1/course/personalised/{uid}")
	public Response getPersonalisedCourses(@PathVariable("uid") String userId) throws JsonParseException, JsonMappingException, IOException
	{
         Response resp = new Response();
		
        ProjectLogger.log("Popular courses request recieved");
	
		Map<String,Object> personalisedCourses = personalisedRecommendationService.getPersonalisedCourses(userId);
		 
		resp.setVer("v1");
		resp.setId("api.courses.personalised");
		resp.setTs(ProjectUtil.getFormattedDate());
		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
		code.setResponseCode(ResponseCode.OK.getResponseCode());
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
		resp.setParams(params);
		// personalised courses for successful response
		resp.put(Constants.RESPONSE, personalisedCourses);
		return resp;
		
	}
	
	
}
