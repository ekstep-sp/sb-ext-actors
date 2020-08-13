/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

import com.infosys.service.EmailNotificationService;

@RestController
@CrossOrigin(origins = "*")
public class EmailNotificationController {

	@Autowired
	EmailNotificationService notificationService;

	// New uuid Fetch Email
	@PostMapping("/v2/Notification/Authoring/Review")
	public Response sendReviewNotification_v2(@RequestBody Map<String, Object> requestBody) {
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.mail.review");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			notificationService.convertUUIDtoEmail(requestBody);
			notificationService.VerifyForReview(requestBody);
			if (!requestBody.containsKey("emailTo"))
				notificationService.NotifyReview(requestBody);
			resp.put(Constants.RESPONSE, requestBody.get("message"));
			resp.put("invalidIds", requestBody.get("invalid_ids"));

		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}
		return resp;
	}

	@PostMapping("/v1/Notification/Authoring/Review")
	public Response sendReviewNotification(@RequestBody Map<String, Object> requestBody) {
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.mail.review");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			notificationService.VerifyForReview(requestBody);
			if (!requestBody.containsKey("emailTo"))
				notificationService.NotifyReview(requestBody);
			resp.put(Constants.RESPONSE, requestBody.get("message"));
			resp.put("invalidIds", requestBody.get("invalid_ids"));

		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}
		return resp;
	}

	@PostMapping("/v2/Notification/Send")
	public Response sendNotification_v2(@RequestBody Map<String, Object> requestBody) {

		Response resp = new Response();
		resp.setVer("v2");
		resp.setId("api.content.create");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			notificationService.convertUUIDtoEmail(requestBody);
			notificationService.VerifyForOneMail(requestBody);
			if (!requestBody.containsKey("emailTo"))
				notificationService.Notify(requestBody);
			resp.put(Constants.RESPONSE, requestBody.get("message"));
			resp.put("invalidIds", requestBody.get("invalid_ids"));

		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}
		return resp;
	}

	@PostMapping("/v1/Notification/Send")
	public Response sendNotification(@RequestBody Map<String, Object> requestBody) {

		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.content.create");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			notificationService.VerifyForOneMail(requestBody);
			if (!requestBody.containsKey("emailTo"))
				notificationService.Notify(requestBody);
			resp.put(Constants.RESPONSE, requestBody.get("message"));
			resp.put("invalidIds", requestBody.get("invalid_ids"));

		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}
		return resp;
	}

	@PostMapping("/v2/Notification/Send/Group")
	public Response sendGoalNotification_v2(@RequestBody Map<String, Object> requestBody) {
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.content.create");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			notificationService.convertUUIDtoEmail(requestBody);
			notificationService.VerifyForGroup(requestBody);
			if (!requestBody.containsKey("emailTo"))
				notificationService.NotifyGroup(requestBody);
			resp.put(Constants.RESPONSE, requestBody.get("message"));
			resp.put("invalidIds", requestBody.get("invalid_ids"));

		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}
		return resp;
	}

	@PostMapping("/v1/Notification/Send/Group")
	public Response sendGoalNotification(@RequestBody Map<String, Object> requestBody) {
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.content.create");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			notificationService.VerifyForGroup(requestBody);
			if (!requestBody.containsKey("emailTo"))
				notificationService.NotifyGroup(requestBody);
			resp.put(Constants.RESPONSE, requestBody.get("message"));
			resp.put("invalidIds", requestBody.get("invalid_ids"));

		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}
		return resp;
	}

	@PostMapping("/v2/Notification/Send/Text")
	public Response sendTextNotification_v2(@RequestBody Map<String, Object> requestBody) {
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.content.create");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			notificationService.convertUUIDtoEmail(requestBody);
			Map<String, Object> rep = notificationService.PlainMail(requestBody);
			resp.put(Constants.RESPONSE, rep.get("message"));
			resp.put("invalidIds", rep.get("invalidIds"));

		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}
		return resp;
	}

	@PostMapping("/v1/Notification/Send/Text")
	public Response sendTextNotification(@RequestBody Map<String, Object> requestBody) {
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.content.create");
		resp.setTs(ProjectUtil.getFormattedDate());
		try {
			Map<String, Object> rep = notificationService.PlainMail(requestBody);
			resp.put(Constants.RESPONSE, rep.get("message"));
			resp.put("invalidIds", rep.get("invalidIds"));

		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
		}
		return resp;
	}

	@GetMapping("/v1/Notification/Send/pdf")
	public ResponseEntity<String> sendPDF() throws UnsupportedEncodingException, MessagingException, IOException {
		notificationService.PDFMail();
		return new ResponseEntity<>("Success", HttpStatus.OK);
	}

}
