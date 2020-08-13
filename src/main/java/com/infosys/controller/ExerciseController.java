/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.exception.InvalidDataInputException;
import com.infosys.exception.ResourceNotFoundException;
import com.infosys.service.ExerciseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ExerciseController {

	@Autowired
	ExerciseService exerciseServ;

	@PostMapping("/v1/users/{user_id}/exercises/{content_id}/submissions")
	public ResponseEntity<Response> submitExercise(@RequestBody Request requestBody,
												   @PathVariable("user_id") String userId,
												   @PathVariable("content_id") String contentId) {
		Response resp = new Response();
		HttpStatus status = HttpStatus.CREATED;
		resp.setVer("v1");
		resp.setId("api.exercise.submit");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			Map<String, Object> jsonMap = requestBody.getRequest();
			jsonMap.put("result_percent", "0");
			jsonMap.put("total_testcases", "0");
			jsonMap.put("testcases_passed", "0");
			jsonMap.put("testcases_failed", "0");
			jsonMap.put("user_id_type", jsonMap.getOrDefault("user_id_type", "uuid"));
			String rep = exerciseServ.insertSubmission(jsonMap, contentId, userId);
			resp.put(Constants.RESPONSE, rep);
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

	@PostMapping("/v1/users/{user_id}/exercises/{content_id}/code-submissions")
	public ResponseEntity<Response> submitExerciseCode(@PathVariable("user_id") String userId,
													   @PathVariable("content_id") String contentId,
													   @RequestBody Request requestBody) {
		Response resp = new Response();
		HttpStatus status = HttpStatus.CREATED;
		resp.setVer("v1");
		resp.setId("api.exercise.submit");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			Map<String, Object> jsonMap = requestBody.getRequest();
			jsonMap.put("user_id_type", jsonMap.getOrDefault("user_id_type", "uuid"));
			String rep = exerciseServ.insertSubmission(jsonMap, contentId, userId);
			resp.put(Constants.RESPONSE, rep);
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

	@GetMapping("/v1/users/{user_id}/exercises/{content_id}/submissions")
	public ResponseEntity<Response> getAllSubmissions(@PathVariable("user_id") String userId,
			@PathVariable("content_id") String contentId,
			@RequestParam(defaultValue = "uuid", required = false, name = "user_id_type") String userIdType,
			@RequestParam(defaultValue = "0", required = false, name = "type") String type) {
		Response resp = new Response();
		HttpStatus status = HttpStatus.OK;
		resp.setVer("v1");
		resp.setId("api.exercise.get");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			List<Map<String, Object>> rep = null;
			if (type.toLowerCase().equals("latest"))
				rep = exerciseServ.getLatestData(userId, contentId, userIdType);
			else
				rep = exerciseServ.getAllData(userId, contentId, userIdType);
			resp.put(Constants.RESPONSE, rep);
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

	@GetMapping("/v1/users/{user_id}/exercises/{content_id}/submissions/{submission_id}")
	public ResponseEntity<Response> getOneSubmission(@PathVariable("user_id") String userId,
			@PathVariable("content_id") String contentId, @PathVariable("submission_id") String submissionId,
			@RequestParam(defaultValue = "uuid", required = false, name = "user_id_type") String userIdType) {
		Response resp = new Response();
		HttpStatus status = HttpStatus.OK;
		resp.setVer("v1");
		resp.setId("api.exercise.get");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			List<Map<String, Object>> rep = exerciseServ.getOneData(userId, contentId, submissionId, userIdType);
			resp.put(Constants.RESPONSE, rep);
		} catch (ResourceNotFoundException e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.resourceNotFound.getErrorCode());
			resp.put("Error", e.getMessage());
			resp.setResponseCode(responseCode);
			status = HttpStatus.NOT_FOUND;
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

	@PostMapping("/v1/users/{user_id}/exercises/{content_id}/submissions/{submission_id}/feedback")
	public ResponseEntity<Response> submitFeedback(@PathVariable("user_id") String userId,
												   @PathVariable("content_id") String contentId, @PathVariable("submission_id") String submissionId,
												   @RequestBody Request requestBody) {
		Response resp = new Response();
		HttpStatus status = HttpStatus.CREATED;
		resp.setVer("v1");
		resp.setId("api.feedback.submit");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			Map<String, Object> jsonMap = requestBody.getRequest();
			jsonMap.put("user_id_type", jsonMap.getOrDefault("user_id_type", "uuid"));
			String rep = exerciseServ.insertFeedback(jsonMap, contentId, userId, submissionId);
			resp.put(Constants.RESPONSE, rep);
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

	@GetMapping("/v1/groups/{group_id}/exercises/{exercise_id}/submissions")
	public ResponseEntity<Response> getSubmissionsByGroup(@PathVariable("group_id") String groupId,
			@PathVariable("exercise_id") String contentId,
			@RequestParam(defaultValue = "latest", required = false, name = "type") String type) {
		Response resp = new Response();
		HttpStatus status = HttpStatus.OK;
		resp.setVer("v1");
		resp.setId("api.submissions.get");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			Map<String, Object> rep = exerciseServ.getSubmissionsByGroups(groupId, contentId);
			resp.put(Constants.RESPONSE, rep);
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

	@GetMapping("/v1/users/{user_id}/exercises/notification")
	public ResponseEntity<Response> getExerciseNotification(@PathVariable("user_id") String userId,
															@RequestParam(defaultValue = "uuid", required = false, name = "user_id_type") String userIdType) {
		Response resp = new Response();
		HttpStatus status = HttpStatus.OK;
		resp.setVer("v1");
		resp.setId("api.exercise.get");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			List<Map<String, Object>> rep = exerciseServ.getExerciseNotification(userId, userIdType);
			resp.put(Constants.RESPONSE, rep);
		} catch (ResourceNotFoundException e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.resourceNotFound.getErrorCode());
			resp.put("Error", e.getMessage());
			resp.setResponseCode(responseCode);
			status = HttpStatus.NOT_FOUND;
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
