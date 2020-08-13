/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/**
 * 
 */
package com.infosys.controller;

import com.infosys.service.CohortService;
import com.infosys.util.LexProjectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * @author Gaurav_Kumar48
 *
 */
@RestController
@CrossOrigin(origins = "*")
public class CohortController {

	@Autowired
	CohortService cohortService;

	@GetMapping("/v1/resources/{resourceId}/user/{userEmail}&type=activeusers")
	public Response getActiveUsers(@PathVariable("resourceId") String resourceId,
			@PathVariable("userEmail") String userEmail,
			@RequestParam(value = "count", required = false) Integer count) {
		// ProjectLogger.log("Fetching Active Users(Cohorts) Available at " + new Date()
		// + "" + " for user " + userEmail + "on resorce : " +
		// resourceId,LoggerEnum.INFO);
		if (count == null)
			count = 20; // default number of records to show for each type
		//String url = "/v1/resources/" + resourceId + "/user/" + userEmail + "&type=activeusers";
		//Long start = System.currentTimeMillis();
		Response resp = cohortService.getActiveUsers(resourceId, userEmail.toLowerCase(), count);
		resp.setVer("v1");
		resp.setId("api.resources.getactiveusers");
		resp.setTs(ProjectUtil.getFormattedDate());
		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
		code.setResponseCode(ResponseCode.OK.getResponseCode());
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
		resp.setParams(params);
		//ProjectLogger.log(url + ":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),
		//	LoggerEnum.INFO);
		return resp;
	}

	@GetMapping("/v1/resources/{resourceId}/user/{userEmail}&type=sme")
	public Response getSMEs(@PathVariable("resourceId") String resourceId, @PathVariable("userEmail") String userEmail,
			@RequestParam(value = "count", required = false) Integer count) {
		Response resp = new Response();
		if (count == null)
			count = 20; // default number of records to show for each type
		String url = "/v1/resources/" + resourceId + "/user/" + userEmail + "&type=sme";
		Long start = System.currentTimeMillis();
		resp = cohortService.getListofSMEs(resourceId, userEmail.toLowerCase(), count);
		resp.setVer("v1");
		resp.setId("api.resources.getSMEs");
		resp.setTs(ProjectUtil.getFormattedDate());

		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
		code.setResponseCode(ResponseCode.OK.getResponseCode());
		// Preparing and setting response param
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
		resp.setParams(params);
		ProjectLogger.log(url + ":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),
				LoggerEnum.INFO);
		return resp;
	}
}
