/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.BadRequestException;
import com.infosys.service.EducatorService;
import com.infosys.util.LexConstants;

@RestController
@CrossOrigin(origins = "*")
public class EducatorController {

	@Autowired
	EducatorService educatorService;

	@PostMapping("/v1/educators/add")
	public ResponseEntity<Response> addEducators(@RequestParam("educators") MultipartFile file) {
		Response response = new Response();
		response.setVer("v1");
		response.setId("api.educator.add");
		response.setTs(ProjectUtil.getFormattedDate());
		Date startTime = new Date();
		try {
			educatorService.addEducators(file);
			response.put(Constants.RESPONSE, "SUCCESS");
		} catch (BadRequestException badRequestException) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.CLIENT_ERROR.getErrorCode());
			response.setResponseCode(responseCode);
			response.put(LexConstants.ERROR_MESSAGE, badRequestException.getMessage());
			response.put(Constants.RESPONSE, "FAILURE");
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		} catch (ApplicationLogicError exception) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.internalError.getErrorCode());
			response.setResponseCode(responseCode);
			response.put(LexConstants.ERROR_MESSAGE, exception.getMessage());
			response.put(Constants.RESPONSE, "FAILURE");
			return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		ProjectLogger.log("Added New Educators started at " + startTime,LoggerEnum.INFO);
		ProjectLogger.log("Added New Educators ended at " + new Date(),LoggerEnum.INFO);
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
}
