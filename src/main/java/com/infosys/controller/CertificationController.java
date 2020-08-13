/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.Request;

import com.infosys.exception.BadRequestException;
import com.infosys.service.CertificationsService;
import com.infosys.util.LexConstants;

@RestController
public class CertificationController {
	@Autowired
	CertificationsService certificationsService;

	@PostMapping("v1/user/certifications")
	public ResponseEntity<Response> certifications(@RequestBody Request request)
			throws ParseException, ClientProtocolException, IOException {
		Response response = new Response();
		Map<String, Object> requestMap = request.getRequest();
		try {
			response.put("resultList", certificationsService.getUserCertifications(requestMap));
		} catch (BadRequestException bre) {
			response.put(LexConstants.ERROR_MESSAGE, bre.getMessage());
			return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			e.printStackTrace();
			response.put(LexConstants.ERROR_MESSAGE, "Something went wrong");
			return new ResponseEntity<Response>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
}
