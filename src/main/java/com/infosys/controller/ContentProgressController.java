/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.exception.InvalidDataInputException;
import com.infosys.service.ContentProgressService;

@RestController
@CrossOrigin(origins = "*")
public class ContentProgressController {

	@Autowired
	ContentProgressService cPService;

	@PostMapping("/v1/users/{email_id}/content/{content_Id}/progress/update")
	public ResponseEntity<Response> getRecentBagde(@RequestBody Map<String, Object> requestBody,
			@PathVariable("email_id") String emailId, @PathVariable("content_Id") String contentId,
			@RequestHeader("rootOrg")String rootOrg) {
		HttpStatus status = HttpStatus.CREATED;
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.updateProgress");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			resp.put(Constants.RESPONSE, cPService.updateProgress(rootOrg,emailId, contentId, requestBody));
		} catch (InvalidDataInputException e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.put("Error", e.getMessage());
			resp.setResponseCode(responseCode);
			status = HttpStatus.BAD_REQUEST;
		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.internalError.getErrorCode());
			resp.put("Error", e.getMessage());
			resp.setResponseCode(responseCode);
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return new ResponseEntity<Response>(resp, status);
	}
}
