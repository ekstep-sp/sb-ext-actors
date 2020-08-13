/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.service.DownloadService;
import com.infosys.util.LexJsonKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class DownloadController {

    @Autowired
    DownloadService downloadService;

    /**
     * Invoked when user downloads a content and registers it in cassandra
     *
     * @param req request body containing userId and contentId
     *  {
     *      request: {
     *          userId: "<userId>",
     *          contentId: "<contentId>"
     *      }
     *  }
     * @return
     * @throws IOException 
     */

    @PostMapping("/v1/user/content/download")
    public Response addUserContentDownload(@RequestBody Request req) throws IOException {
        Response resp = new Response();
        ProjectLogger.log("User Content Download event received as " + req, LoggerEnum.INFO.name());

        Map<String, Object> reqMap = req.getRequest();

        //Setting other attributes of response like version, timestamp, response param etc.
        resp.setVer("v1");
        resp.setId("api.user.content.download");
        resp.setTs(ProjectUtil.getFormattedDate());

        String userId = (String) reqMap.get(JsonKey.USER_ID);
        String contentId = (String) reqMap.get(JsonKey.CONTENT_ID);
        String responseCode = "SUCCESS";

        if (downloadService.addContentDownload(userId, contentId) == false)
            responseCode = "FAILED";

        ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
        code.setResponseCode(ResponseCode.OK.getResponseCode());
        //Preparing and setting response param
        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
        resp.setParams(params);
        //Response success for topic add
        resp.put(Constants.RESPONSE, responseCode);
        return resp;
    }

    /**
     * Invoked when user opens the app to sync the local downloads with the server
     *
     * @param req request body containing userId and contentIds
     *            {
     *            request: {
     *            userId: "<userId>",
     *            contentIds: [
     *            "<contentId1>",
     *            "<contentId2>",
     *            "<contentId3>",
     *            ]
     *            }
     *            }
     * @return
     * @throws IOException 
     */

    @PostMapping("/v1/user/content/download/sync")
    public Response syncContentDownloads(@RequestBody Request req) throws IOException {
        Response resp = new Response();
        ProjectLogger.log("User Content Download sync event received as " + req, LoggerEnum.INFO.name());

        Map<String, Object> reqMap = req.getRequest();

        //Setting other attributes of response like version, timestamp, response param etc.
        resp.setVer("v1");
        resp.setId("api.user.content.download.sync");
        resp.setTs(ProjectUtil.getFormattedDate());

        String userId = (String) reqMap.get(JsonKey.USER_ID);
        List<String> contentIds = (List) reqMap.get(JsonKey.CONTENT_IDS);
        String responseCode = "SUCCESS";

        if (!downloadService.syncContentDownload(userId, contentIds).isEmpty())
            responseCode = "FAILED";

        ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
        code.setResponseCode(ResponseCode.OK.getResponseCode());
        //Preparing and setting response param
        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
        resp.setParams(params);
        //Response success for topic add
        resp.put(Constants.RESPONSE, responseCode);
        return resp;
    }

    /**
     * Invoked when user needs to check the contents he/she has downloaded
     *
     * @param userId userId
     * @return
     * @throws IOException 
     */
    @GetMapping("/v1/user/{userId}/content/downloaded")
    public Response getDownloadedCourses(@PathVariable("userId") String userId) throws IOException {
        Response resp = new Response();
        ProjectLogger.log("Get dowloaded content request received for user ", userId, LoggerEnum.INFO.name());
        //Response map containing info to be sent as result along with other info
        Map<String, Object> respMap = new HashMap<>();

        List<Map<String, Object>> courseList = downloadService.getContentDownloaded(userId);
        respMap.put(LexJsonKey.RESOURCES, courseList);
        //Setting other attributes of response like version, timestamp, response param etc.
        resp.setVer("v1");
        resp.setId("api.user.content.downloaded");
        resp.setTs(ProjectUtil.getFormattedDate());
        ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
        code.setResponseCode(ResponseCode.OK.getResponseCode());
        //Preparing and setting response param
        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
        resp.setParams(params);
        //Response sent back
        resp.put(Constants.RESPONSE, respMap);
        return resp;
    }
}
