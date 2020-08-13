/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.service.TimeSpentService;

@RestController
@CrossOrigin(origins = "*")
public class TimeSpentController {

	@Autowired
	TimeSpentService tsService;

	@GetMapping("/v2/users/{email_id}/dashboard/timespent")
	public ResponseEntity<Response> generateBagdes(@PathVariable("email_id") String emailId,
			@RequestParam(defaultValue = "0", required = false, name = "startdate") String startDate,
			@RequestParam(defaultValue = "0", required = false, name = "enddate") String endDate) {
		String url = "/v2/users/"+emailId+"/dashboard/timespent";
		Response resp = new Response();
		resp.setVer("v2");
		resp.setId("api.courses");
		resp.setTs(ProjectUtil.getFormattedDate());
		HttpStatus httpStatus = null;
		Long start = System.currentTimeMillis();
		try {
			resp.put(Constants.RESPONSE, tsService.getUserDashboard(emailId, startDate, endDate));
			httpStatus = HttpStatus.OK;
		} catch (Exception e) {
			httpStatus = HttpStatus.BAD_REQUEST;
			resp.put("errmsg","Bad Request");
			resp.setResponseCode(ResponseCode.CLIENT_ERROR);
		}
		//ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return new ResponseEntity<Response>(resp,httpStatus);
	}
}
