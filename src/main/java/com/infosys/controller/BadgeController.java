/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.service.BadgeService;
import com.infosys.util.LexProjectUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

@RestController
@CrossOrigin(origins = "*")
public class BadgeController {

	@Autowired
	BadgeService badgeService;

	@GetMapping("/v1/Users/{email_id}/Badges")
	public Response generateBagdes(@PathVariable("email_id") String email_id) {
		String url = "/v1/Users/" + email_id + "/Badges";
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.getBadges");
		resp.setTs(ProjectUtil.getFormattedDate());
		Long start = System.currentTimeMillis();
		try {
			resp.put(Constants.RESPONSE, badgeService.getAllBadges(email_id));
		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}

		ProjectLogger.log(url + ":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return resp;
	}
	
	@GetMapping("/v1/Users/{email_id}/Achievements/Recent")
	public Response getRecentBagde(@PathVariable("email_id") String email_id) {
		String url = "/v1/Users/" +email_id+ "/Achievements/Recent";
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.getRecentBadge");
		resp.setTs(ProjectUtil.getFormattedDate());
		Long start = System.currentTimeMillis();
		try {
			resp.put(Constants.RESPONSE, badgeService.getRecentBadge(email_id));
		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}
		ProjectLogger.log(url + ":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return resp;
	}
}
