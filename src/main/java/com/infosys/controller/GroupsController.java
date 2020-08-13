/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.util.List;
import java.util.Map;

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

import com.infosys.exception.InvalidDataInputException;
import com.infosys.service.ExerciseService;

@RestController
@CrossOrigin(origins = "*")
public class GroupsController {

	@Autowired
	ExerciseService exerciseServ;
	
	@GetMapping("/v1/educators/{educator_id}/groups")
	public ResponseEntity<Response> getAllGroups(
			@PathVariable("educator_id") String educatorId,
			@RequestParam(defaultValue = "uuid", required=false, name ="user_id_type") String userIdType) {
		Response resp = new Response();
		HttpStatus status=HttpStatus.OK;
		resp.setVer("v1");
		resp.setId("api.groups.get");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
            List<Map<String,String>> rep =exerciseServ.getEducatorGroups(educatorId, userIdType);
            resp.put(Constants.RESPONSE, rep);
		} catch(InvalidDataInputException e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.put("Error", e.getMessage());
			resp.setResponseCode(responseCode);
			status=HttpStatus.BAD_REQUEST;
		}catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.internalError.getErrorCode());
			resp.put("Error", e.getMessage());
			resp.setResponseCode(responseCode);
			status=HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return new ResponseEntity<Response>(resp,status);
	}
	
}
