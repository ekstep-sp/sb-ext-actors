/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at 
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.service.NPTELService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.LexProjectUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
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

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class NPTELController {

    @Autowired
    NPTELService nptelService;

    /**
     * @param req Request Body
     *  {
     *      request: {
     *          userId: "<submitterId>",
     *          disciplineId: "<disciplineId>"
     *      }
     *  }
     * @return Sample response
     * {
     *     id: "api.nptel.import.courses",
     *     ver: "v1",
     *     ts: "<timestamp>",
     *     params: {
     *         resmsgid: null,
     *         msgid: null,
     *         err: null,
     *         status: "success",
     *         errmsg: null
     *     },
     *     responseCode: "OK",
     *     result: {
     *         response: [
     *             <list of  module ids>
     *         ]
     *     }
     * }
     */
    @PostMapping("/v1/nptel/import/courses")
    public Response importCourses(@RequestBody Request req) {
        Response resp = new Response();
        String url ="/v1/nptel/import/courses";
        Map<String, Object> reqMap = req.getRequest();
        ProjectLogger.log("Import NPTEL courses request received as ", reqMap, LoggerEnum.INFO.name());
        //Setting other attributes of response like version, timestamp, response param etc.
        resp.setVer("v1");
        resp.setId("api.nptel.import.courses");
        resp.setTs(ProjectUtil.getFormattedDate());

        String userId = (String) reqMap.get(JsonKey.USER_ID);
        String disciplineId = (String) reqMap.get(LexJsonKey.DISCIPLINE_ID);
        
        ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
        code.setResponseCode(ResponseCode.OK.getResponseCode());
        //Preparing and setting response param
        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
        resp.setParams(params);
        //Response
        Long start = System.currentTimeMillis();
        List<String> moduleIds = nptelService.importCourses(userId, disciplineId);
        if (moduleIds.isEmpty())
            resp.put(Constants.RESPONSE, "FAILED");
        else
            resp.put(Constants.RESPONSE, moduleIds);
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return resp;
    }
}
