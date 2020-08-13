/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.service.UserDataCorrectionService;

@RestController
@CrossOrigin(origins = "*")
public class UserDataCorrectionController {

	@Autowired
	UserDataCorrectionService uDCService;

	@PostMapping("/v1/User/{emailId}/recalculatebadges")
    public Response sendNotification(@PathVariable("emailId") String emailId) {

		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.recalculatebadges");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
            resp.put(Constants.RESPONSE, uDCService.correctData(emailId));
		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}
		return resp;
	}
	

}
