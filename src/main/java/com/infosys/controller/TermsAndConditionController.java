/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/**
© 2019 Infosys Limited, Bangalore, India. All Rights Reserved. 
Version: 1.10

Except for any free or open source software components embedded in this Infosys proprietary software program (“Program”), 
this Program is protected by copyright laws, international treaties and other pending or existing intellectual property rights in India,
the United States and other countries. Except as expressly permitted, any unauthorized reproduction, storage, transmission in any form or
by any means (including without limitation electronic, mechanical, printing, photocopying, recording or otherwise), or any distribution of 
this Program, or any portion of it, may result in severe civil and criminal penalties, and will be prosecuted to the maximum extent possible
 under the law.
*/


package com.infosys.controller;

import com.infosys.service.TermsAndConditionService;
import com.infosys.util.LexJsonKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.*;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import com.infosys.exception.ApplicationLogicError;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class TermsAndConditionController {

    @Autowired
    TermsAndConditionService termsAndConditionService;

    /**
     * Invoked when user accepts the T&C and logs it in DB with region, version and datetime
     *
     * @param req request body containing userId, region and version
     *            {
     *            request: {
     *            userId: "<userId>",
     *            userType: "<userType>",
     *            termsAccepted: [
     *            {
     *            docName: "<docName>",
     *            version: "<version>"
     *            },
     *            {
     *            docName: "<docName>",
     *            version: "<version>"
     *            }
     *            ]
     *            }
     *            }
     * @return
     * @throws IOException 
     */

    @PostMapping("/v1/terms/accept")
    public Response addUserTermsAccepted(@RequestBody Request req) throws IOException {
        Response resp = new Response();
        String url ="/v1/terms/accept";
        Map<String, Object> reqMap = req.getRequest();
        
        //Setting other attributes of response like version, timestamp, response param etc.
        resp.setVer("v1");
        resp.setId("api.terms.accept");
        resp.setTs(ProjectUtil.getFormattedDate());

        String userId = (String) reqMap.get(JsonKey.USER_ID);
        String userType = (String) reqMap.get(LexJsonKey.USER_TYPE);
        Long start = System.currentTimeMillis();
        List<Map<String, Object>> termsAccepted = (List<Map<String, Object>>) reqMap.get(LexJsonKey.TERMS_ACCEPTED);

        String responseCode = "SUCCESS";

        if (termsAndConditionService.addUserTermsAcceptance(userId, userType, termsAccepted) == false)
            responseCode = "FAILED";

        ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
        code.setResponseCode(ResponseCode.OK.getResponseCode());
        //Preparing and setting response param
        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
        resp.setParams(params);
        //Response
        resp.put(Constants.RESPONSE, responseCode);
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return resp;
    }

    @PostMapping("/v2/terms/accept")
    public Response addUserTermsAcceptedV2(@RequestBody Request req) throws Exception {
        Response resp = new Response();
        String url = "/v2/terms/accept";
        Map<String, Object> reqMap = req.getRequest();

        //Setting other attributes of response like version, timestamp, response param etc.
        resp.setVer("v2");
        resp.setId("api.terms.accept");
        resp.setTs(ProjectUtil.getFormattedDate());

        String userId = (String) reqMap.get(JsonKey.USER_ID);
        String userType = (String) reqMap.get(LexJsonKey.USER_TYPE);
        Long start = System.currentTimeMillis();
        List<Map<String, Object>> termsAccepted = (List<Map<String, Object>>) reqMap.get(LexJsonKey.TERMS_ACCEPTED);

        Map<String, Object> result = termsAndConditionService.addUserTermsAcceptanceV2(userId, userType, termsAccepted);

        String responseCode = (String) result.get(Constants.RESPONSE);
        String errorMessage = (String) result.get("errorMessage");

        ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
        code.setResponseCode(ResponseCode.OK.getResponseCode());
        //Preparing and setting response param
        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
        resp.setParams(params);
        //Response
        resp.put(Constants.RESPONSE, responseCode);
        resp.put("errorMessage", errorMessage);
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return resp;
    }

    /**
     * Invoked when user logs in to check whether user has accepted the latest terms and conditions
     *
     * @param userId
     * @return {
     * "id": "api.latest.terms",
     * "ver": "v1",
     * "ts": "2018-03-20 14:43:43.261+0530",
     * "params": {
     * "resmsgid": null,
     * "msgid": null,
     * "err": null,
     * "status": "success",
     * "errmsg": null
     * },
     * "responseCode": "OK",
     * "result": {
     * "response": {
     * "isAccepted": true,
     * "termsAndConditions": [
     * {
     * "name": "Generic T&C",
     * "version": "1.0",
     * "content": "<content>"
     * },
     * {
     * "name": "Rest of world",
     * "version": "1.0",
     * "content": "<content>"
     * },
     * {
     * "name": "Europe",
     * "version": "1.0",
     * "content": "<content>"
     * }
     * ]
     * }
     * }
     * }
     */

    @GetMapping("/v1/latest/terms/{userType}")
    public ResponseEntity<Response> getUserTermsAccepted(@RequestParam(value = "userId", defaultValue = "default") String userId,
                                                         @PathVariable(value = "userType") String userType) {
        Response resp = new Response();
        String url = "/v1/latest/terms/"+userType;
        //Response map containing info to be sent as result along with other info
        Map<String, Object> respMap = new HashMap<>();

        if (userType.equalsIgnoreCase("Author"))
            userType = "Author";
        else if (userType.equalsIgnoreCase("User"))
            userType = "User";
        
        ResponseCode code;
        HttpStatus httpStatus;
        Long start = System.currentTimeMillis();
        Map<String, Object> terms = termsAndConditionService.checkVersionChange(userId, userType);
        if (terms == null || terms.isEmpty()) {
            respMap.put(LexJsonKey.TERMS_AND_CONDTIONS, null);
            respMap.put(LexJsonKey.IS_ACCEPTED, null);
            code = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
            code.setResponseCode(ResponseCode.invalidRequestData.getResponseCode());
            httpStatus = HttpStatus.BAD_REQUEST;
        } else {
            respMap.put(LexJsonKey.TERMS_AND_CONDTIONS, terms.get(LexJsonKey.TERMS_AND_CONDTIONS));
            respMap.put(LexJsonKey.IS_ACCEPTED, terms.get(LexJsonKey.IS_ACCEPTED));
            code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
            code.setResponseCode(ResponseCode.OK.getResponseCode());
            httpStatus = HttpStatus.OK;
        }
        //Setting other attributes of response like version, timestamp, response param etc.
        resp.setVer("v1");
        resp.setId("api.latest.terms");
        resp.setTs(ProjectUtil.getFormattedDate());
        //Preparing and setting response param
        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
        resp.setParams(params);
        //Response sent back
        resp.put(Constants.RESPONSE, respMap);
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return new ResponseEntity<Response>(resp, httpStatus);
    }

    @GetMapping("/v2/latest/terms/{userType}")
    public ResponseEntity<Response> getUserTermsAcceptedV2(@RequestParam(value = "userId", defaultValue = "default") String userId,
                                                           @PathVariable(value = "userType") String userType) {
        Response resp = new Response();
        String url = "/v2/latest/terms/" + userType;
        //Response map containing info to be sent as result along with other info
        Map<String, Object> respMap = new HashMap<>();

        if (userType.equalsIgnoreCase("Author"))
            userType = "Author";
        else if (userType.equalsIgnoreCase("User"))
            userType = "User";

        ResponseCode code;
        HttpStatus httpStatus;
        Long start = System.currentTimeMillis();
        Map<String, Object> terms = termsAndConditionService.checkVersionChangeV2(userId, userType);
        if (terms == null || terms.isEmpty()) {
            respMap.put(LexJsonKey.TERMS_AND_CONDTIONS, null);
            respMap.put(LexJsonKey.IS_ACCEPTED, null);
            code = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
            code.setResponseCode(ResponseCode.invalidRequestData.getResponseCode());
            httpStatus = HttpStatus.BAD_REQUEST;
        } else {
            respMap.put(LexJsonKey.TERMS_AND_CONDTIONS, terms.get(LexJsonKey.TERMS_AND_CONDTIONS));
            respMap.put(LexJsonKey.IS_ACCEPTED, terms.get(LexJsonKey.IS_ACCEPTED));
            code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
            code.setResponseCode(ResponseCode.OK.getResponseCode());
            httpStatus = HttpStatus.OK;
        }
        //Setting other attributes of response like version, timestamp, response param etc.
        resp.setVer("v2");
        resp.setId("api.latest.terms");
        resp.setTs(ProjectUtil.getFormattedDate());
        //Preparing and setting response param
        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
        resp.setParams(params);
        //Response sent back
        resp.put(Constants.RESPONSE, respMap);
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return new ResponseEntity<Response>(resp, httpStatus);
    }
    
    @PostMapping("/v1/user/{user_id}/postprocessing")
	public ResponseEntity<?> postProcessing(@PathVariable(name = "user_id") String userId,
			@RequestParam(name = "userType", defaultValue="User") String userType, @RequestHeader("rootOrg") String rootOrg, @RequestHeader("org") String org) {
		try {
			termsAndConditionService.userPostProcessing(rootOrg,org, userId, userType);
			return new ResponseEntity(HttpStatus.OK);
		} catch (ApplicationLogicError e) {
			// TODO Auto-generated catch block
			Map<String, Object> errorMap = new HashMap<>();
			errorMap.put("error", e.getMessage());
			return new ResponseEntity(errorMap, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
    
}
