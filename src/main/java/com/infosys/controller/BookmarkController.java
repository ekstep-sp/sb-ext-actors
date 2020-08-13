/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

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

import com.infosys.util.LexProjectUtil;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class BookmarkController {
    /**
     * Adds a course/content as a bookmark to a user
     *
     * @param req Request body containing userId and courseId
     * @return Response
     */
    @PostMapping("v1/user/bookmark/add")
    public Response addUserBookmark(@RequestBody Request req) {
        Response resp = new Response();
        Map<String, Object> reqMap = req.getRequest();
        String url = "v1/user/bookmark/add";
        //Setting other attributes of response like version, timestamp, response param etc.
        resp.setVer("v1");
        resp.setId("api.user.bookmark.add");
        resp.setTs(ProjectUtil.getFormattedDate());
        Long start = System.currentTimeMillis();      
        ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
        code.setResponseCode(ResponseCode.OK.getResponseCode());
        //Preparing and setting response param
        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
        resp.setParams(params);
        //Response success for bookmark add
        resp.put(Constants.RESPONSE, "SUCCESS");
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return resp;
    }

    /**
     * Removes the bookmark from the course/content
     *
     * @param req Request body containing userId and courseId
     * @return Response
     */
    @DeleteMapping("v1/user/bookmark/remove")
    public Response removeUserBookmark(@RequestBody Request req) {
        Response resp = new Response();
        Map<String, Object> reqMap = req.getRequest();
        //Setting other attributes of response like version, timestamp, response param etc.
        String url = "v1/user/bookmark/remove";
        Long start = System.currentTimeMillis();      
        resp.setVer("v1");
        resp.setId("api.user.topic.update");
        resp.setTs(ProjectUtil.getFormattedDate());
        ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
        code.setResponseCode(ResponseCode.OK.getResponseCode());
        //Preparing and setting response param
        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
        resp.setParams(params);
        //Response success for bookmark logical delete
        resp.put(Constants.RESPONSE, "SUCCESS");
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return resp;
    }
}
