/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.service.PersistentMapService;
import com.infosys.util.ErrorGenerator;

@RestController
public class PersistentMapController {
	
	@Autowired
	private PersistentMapService persistentMapService;
	
	@GetMapping("/pmap")
	public ResponseEntity<Response> getKeyValuePairs(@RequestParam(required = true, name = "keys") ArrayList<String> keys) {
		Response response = new Response();

		try {
			response = persistentMapService.getKeyValuePairs(keys);
		} catch (Exception e) {
			ProjectLogger.log("pmap.getKeyValues", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
					"pmap.getKeyValues", "1", "controller error"), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.setVer("v1");
		response.setId("api.pmap");
		response.setTs(ProjectUtil.getFormattedDate());

		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
	
	@PostMapping("/pmap")
	public ResponseEntity<Response> saveKeyValuePairs(@RequestBody Request request) {
		Response response = new Response();

		try {
			response = persistentMapService.saveKeyValuePairs(request);
		} catch (Exception e) {
			ProjectLogger.log("pmap.saveKeyValuePairs", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
					"pmap.saveKeyValuePairs", "1", "controller error"), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.setVer("v1");
		response.setId("api.pmap");
		response.setTs(ProjectUtil.getFormattedDate());

		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
	
	@DeleteMapping("/pmap")
	public ResponseEntity<Response> deleteKeyValuePaires(@RequestParam(required = true, name = "keys") ArrayList<String> keys) {
		Response response = new Response();

		try {
			response = persistentMapService.deleteKeyValuePairs(keys);
		} catch (Exception e) {
			ProjectLogger.log("pmap.deleteKeyValuePaires", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError,
					"pmap.deleteKeyValuePaires", "1", "controller error"), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.setVer("v1");
		response.setId("api.pmap");
		response.setTs(ProjectUtil.getFormattedDate());

		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}
}
