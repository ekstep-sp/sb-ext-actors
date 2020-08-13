/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.service.TagsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.responsecode.ResponseCode;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class TagsController {

    @Autowired
    TagsService tagsService;


    @GetMapping("/v1/nested/tags/read")
    public Response getNestedTags() throws IOException {
        Response resp = new Response();
        ProjectLogger.log("Nested tags read request received", LoggerEnum.INFO.name());
        //Response map containing info to be sent as result along with other info
        Map<String, Object> respMap = new HashMap<>();

        List<Map<String, Object>> topicList = tagsService.getNestedTags();
        respMap.put("count", topicList.size());
        respMap.put("catalog", topicList);
        //Setting other attributes of response like version, timestamp, response param etc.
        resp.setVer("v1");
        resp.setId("api.nested.tags.read");
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
        return resp;
    }

    //API for Authoring Tool : return all tags without hierarchy, as a Array
    @GetMapping("/v1/tags/get/withoutHierarchy")
    public Response getAllContentTagsWithoutHierarchy() {
        Response resp = tagsService.fetchContentTagsWithoutHierarchy();
        resp.setVer("v1");
        resp.setId("api.tags.get.withoutHierarchy");
        resp.setTs(ProjectUtil.getFormattedDate());
        ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
        code.setResponseCode(ResponseCode.OK.getResponseCode());
        // Preparing and setting response parameters
        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
        resp.setParams(params);
        return resp;
    }

    @GetMapping("/v1/tags/get/withHierarchy")
    public Response getAllContentTagsWithHierarchy() {
        Response resp = tagsService.fetchContentTagsWithHierarchy();
        resp.setVer("v1");
        resp.setId("api.tags.get.withHierarchy");
        resp.setTs(ProjectUtil.getFormattedDate());
        ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
        code.setResponseCode(ResponseCode.OK.getResponseCode());
        // Preparing and setting response parameters
        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
        resp.setParams(params);
        return resp;
    }
}
