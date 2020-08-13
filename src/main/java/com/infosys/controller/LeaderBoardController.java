/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.exception.InvalidDataInputException;
import com.infosys.exception.NoContentException;
import com.infosys.service.LeaderBoardService;

@RestController
@CrossOrigin(origins = "*")
public class LeaderBoardController {

	@Autowired
	LeaderBoardService leaderBoardService;

	@GetMapping("/v1/LeaderBoard")
	public ResponseEntity<Response> getLeaderBoard(
			@RequestParam(defaultValue = "L", required = false, name = "leaderboard_type") String leaderboard_type,
			@RequestParam(defaultValue = "M", required = false, name = "duration_type") String duration_type,
			@RequestParam("email_id") String email_id,
			@RequestParam(required = true, name = "year") String year,
			@RequestParam(required = true, name = "duration_value") String duration_value) throws Exception {
		String url ="/v1/LeaderBoard";
		HttpStatus httpStatus = null;
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.getLeaderBoard");
		resp.setTs(ProjectUtil.getFormattedDate());
		Long start = System.currentTimeMillis();
		try {
			resp.put(Constants.RESPONSE,
					leaderBoardService.getLeaderBoard(duration_type, leaderboard_type, email_id, year, duration_value));
			httpStatus = HttpStatus.OK;
		} catch (Exception e) {
			resp.setResponseCode(ResponseCode.CLIENT_ERROR);
			resp.put("errmsg","Bad Request");
			httpStatus = HttpStatus.BAD_REQUEST;
		}
		//ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return new ResponseEntity<Response>(resp,httpStatus);
		
	}

	@GetMapping("/v1/TopLearners")
	public Response getPastTopLearners(@RequestParam("leaderboard_type") String leaderboard_type,
			@RequestParam("duration_type") String duration_type) {
		String url = "/v1/TopLearners";
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.getTopLearners");
		resp.setTs(ProjectUtil.getFormattedDate());
		Long start = System.currentTimeMillis();
		try {
			resp.put(Constants.RESPONSE, leaderBoardService.pastTopLearners(duration_type, leaderboard_type));
		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}
		//ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return resp;
	}
}
