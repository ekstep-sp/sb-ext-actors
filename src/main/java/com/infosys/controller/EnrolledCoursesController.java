/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.Date;
import java.util.Map;

//import org.springframework.web.bind.annotation.GetMapping;

@RestController
@CrossOrigin(origins = "*")
public class EnrolledCoursesController {
	
	@Autowired
	EnrollmentService enrolmentService;

    @GetMapping("/v1/course/enrolled/{uid}")
    public Response getUserEnrolledCourses(@PathVariable("uid") String userId)
	{
    	Date startTime = new Date();
		Response resp = new Response();
		
		ProjectLogger.log("enrolled courses request of userid " + userId + "recieved");
        
		Map<String,Object> newCourses = enrolmentService.getUserEnrolledCourses(userId);
		
		resp.setVer("v1");
		resp.setId("api.courses.enrolled");
		resp.setTs(ProjectUtil.getFormattedDate());
		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
		code.setResponseCode(ResponseCode.OK.getResponseCode());
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
		resp.setParams(params);
		// user enrolled courses for successful response
		resp.put(Constants.RESPONSE, newCourses);
		ProjectLogger.log("Fetching user enroled Courses at " + startTime + " for user : " + userId,LoggerEnum.INFO);
		ProjectLogger.log("Ending user enroled Courses at " + startTime + " for user : " + userId,LoggerEnum.INFO);
		return resp;
	}
}
