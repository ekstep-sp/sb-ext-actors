/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.service.FeedbackService;
import com.infosys.util.LexProjectUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class FeedbackController {

    @Autowired
    FeedbackService feedbackService;

    /**
     * Adds the user's rating and comment on the course/content in table
     *
     * @param req Request body containing userId, courseId, rating and comments
     * @return Response
     */
    @PostMapping("v1/course/feedback/add/{userId}")
    public Response addUserTopic(@RequestBody Request req, @PathVariable("userId") String userId) {
       String url = "v1/course/feedback/add/" + userId;
    	//Response resp = new Response();
        Map<String, Object> reqMap = req.getRequest();
        reqMap.put("userId", userId);
        Long start = System.currentTimeMillis();  
        Response resp = feedbackService.addFeedback(reqMap);
        //Setting other attributes of response like version, timestamp, response param etc.
        resp.setVer("v1");
        resp.setId("api.course.feedback.add");
        resp.setTs(ProjectUtil.getFormattedDate());
        ResponseCode responseCode = resp.getResponseCode();
        //ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
        //code.setResponseCode(ResponseCode.OK.getResponseCode());
        //Preparing and setting response param
        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(responseCode.getResponseCode()).name());
        if (responseCode.getResponseCode() != 200)
            params.setErr("Internal Server problem");
        resp.setParams(params);
        //Response success for feedback add
        //resp.put(Constants.RESPONSE, respMap);
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return resp;
    }

    /**
     * Fetches all the ratings and comments given by users for the course/content
     *
     * @param courseId
     * @return Response
     */
    @GetMapping("/v1/course/feedback/read/{cid}")
    public Response getAllTopics(@PathVariable("cid") String courseId) {
        Response resp = new Response();
        String url = "/v1/course/feedback/read/" + courseId;
        //Response map containing info to be sent as result along with other info
        Long start = System.currentTimeMillis();
        Map<String, Object> respMap = feedbackService.getFeedbackForCourse(courseId);
        //Setting other attributes of response like version, timestamp, response param etc.
        resp.setVer("v1");
        resp.setId("api.course.feedback.read");
        resp.setTs(ProjectUtil.getFormattedDate());
        ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
        code.setResponseCode(ResponseCode.OK.getResponseCode());
        //Preparing and setting response param
        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
        resp.setParams(params);
        //Response success for logout
        resp.put(Constants.RESPONSE, respMap);
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return resp;
    }
}
