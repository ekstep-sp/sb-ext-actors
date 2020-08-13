/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.service.ProfileService;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexJsonKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
public class ProfileController {

	@Autowired
	ProfileService profileService;

    @Autowired
    UserUtilityService userUtilService;

	@GetMapping("/v1/Users/{userEmail}/Data")
    public ResponseEntity<Response> getUserData(@PathVariable("userEmail") String userEmail) {

		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.userProfile");
		resp.setTs(ProjectUtil.getFormattedDate());
        HttpStatus status = HttpStatus.CREATED;
		try {
			if (userEmail == null) {
				// return bad request
			}
			resp.put(Constants.RESPONSE, profileService.getUserData(userEmail));
		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
            status = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<Response>(resp, status);
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/v1/Users/Many")
    public ResponseEntity<Response> getMultipleUserData(@RequestBody Request requestBody) {

		Response resp = new Response();
		Map<String, Object> request = requestBody.getRequest();
		resp.setVer("v1");
		resp.setId("api.content.create");
		resp.setTs(ProjectUtil.getFormattedDate());
        HttpStatus status = HttpStatus.CREATED;
		try {
			resp.put(Constants.RESPONSE,
                    profileService.getMultipleUserData((List<String>) request.get(LexJsonKey.Emails)));
        } catch (Exception e) {
            ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
            status = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<Response>(resp, status);
    }

    @GetMapping("/v1/Users/{userEmail}/Role")
    public ResponseEntity<Response> getUserRoles(@PathVariable("userEmail") String userEmail) {

        Response resp = new Response();
        resp.setVer("v1");
        resp.setId("api.userRoles");
        resp.setTs(ProjectUtil.getFormattedDate());
        HttpStatus status = HttpStatus.CREATED;
        try {
            resp.put(Constants.RESPONSE, profileService.getRole(userEmail));
        } catch (Exception e) {
            ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
            resp.setResponseCode(responseCode);
            status = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<Response>(resp, status);
    }

    @GetMapping(value = "/v1/Users/{userEmail}/Photo", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getImage(@PathVariable("userEmail") String userEmail) {
		byte[] image = profileService.getUserPhoto(userEmail);
		if (image.length == 0)
			return new ResponseEntity<byte[]>(image, HttpStatus.NOT_FOUND);
		return new ResponseEntity<byte[]>(image, HttpStatus.CREATED);
	}

    @GetMapping("/v2/Users/{userUUID}/Data")
    public ResponseEntity<Response> getUserDataByUUID(@PathVariable("userUUID") String userUUID) {

        Response resp = new Response();
        resp.setVer("v2");
        resp.setId("api.userProfile");
        resp.setTs(ProjectUtil.getFormattedDate());
        HttpStatus status = HttpStatus.CREATED;
        try {
            if (userUUID == null) {
                // return bad request
            }
            resp.put(Constants.RESPONSE, profileService.getUserDataByUUID(userUUID));
        } catch (Exception e) {
            ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
            resp.setResponseCode(responseCode);
            status = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<Response>(resp, status);
    }

    @GetMapping(value = "/v2/Users/{userUUID}/Photo", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getImageByUUID(@PathVariable("userUUID") String userUUID) {
        try {
            byte[] image = profileService.getUserPhotoByUUID(userUUID);
            if (image.length == 0)
                return new ResponseEntity<byte[]>(image, HttpStatus.NOT_FOUND);
            return new ResponseEntity<byte[]>(image, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<byte[]>(new byte[0], HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/v1/Users/{userUUID}/Email")
    public ResponseEntity<Response> getEmailByUUID(@PathVariable("userUUID") String userUUID) {

        Response resp = new Response();
        resp.setVer("v1");
        resp.setId("api.userEmail");
        resp.setTs(ProjectUtil.getFormattedDate());
        HttpStatus status = HttpStatus.CREATED;
        try {
            if (userUUID == null) {
                // return bad request
            }
            resp.put(Constants.RESPONSE, profileService.getEmailByUUID(userUUID));
        } catch (Exception e) {
            ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
            resp.setResponseCode(responseCode);
            status = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<Response>(resp, status);
    }

    @GetMapping("/v1/users/{userid:.+}")
    public ResponseEntity<Map<String, Object>> fetchUser(@PathVariable("userid") String userId,
                                                         @RequestParam(defaultValue = "user", required = false, name = "idtype") String idType) {
        HttpStatus status = HttpStatus.OK;
        Map<String, Object> result = null;
        try {
            result = profileService.fetchUserData(userId, idType);
            if (result == null || result.isEmpty()) {
                status = HttpStatus.NOT_FOUND;
            }
        } catch (Exception ex) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<Map<String, Object>>(result, status);

    }

    @SuppressWarnings("unchecked")
    @PostMapping("/v1/users/Validate")
    public ResponseEntity<List<Map<String, Object>>> fetchUserEmailsinActiveDirectory(
            @RequestBody Request requestBody) {
        HttpStatus status = HttpStatus.OK;
        List<Map<String, Object>> result = null;
        try {
            result = userUtilService.getUserIdsFromActiveDirectory((List<String>) requestBody.get(LexJsonKey.Emails));
            if (result == null || result.isEmpty()) {
                status = HttpStatus.NOT_FOUND;
            }
        } catch (Exception ex) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<List<Map<String, Object>>>(result, status);

    }

}
