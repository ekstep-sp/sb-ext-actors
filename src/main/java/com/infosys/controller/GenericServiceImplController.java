/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;


import com.infosys.service.GenericRecommendationService;
import com.infosys.util.LexProjectUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.List;


@RestController
@CrossOrigin(origins = "*")
public class GenericServiceImplController {

    @Autowired
    GenericRecommendationService genericRecommendationService;

    @GetMapping("/v1/course/popular")
    public Response getPopularCourses(@RequestParam(value = "resourceCount", defaultValue = "5") String numberOfResources, @RequestParam(value = "pageNumber", defaultValue = "0") String pageNumber) {

        Response resp = new Response();
        String url = "/v1/course/popular";
		resp.setVer("v1");
		resp.setId("api.courses.popular");
		resp.setTs(ProjectUtil.getFormattedDate());
        Long start = System.currentTimeMillis();
		try {
            List<Object> allEntries = genericRecommendationService.getPopularCourses(numberOfResources, pageNumber);

            ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
            code.setResponseCode(ResponseCode.OK.getResponseCode());
            ResponseParams params = new ResponseParams();
            params.setMsgid(ExecutionContext.getRequestId());
            params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
            resp.setParams(params);

            resp.put(Constants.RESPONSE, allEntries);
        } catch (Exception e) {
            ResponseCode code = ResponseCode.getResponse(ResponseCode.esError.getErrorCode());
            code.setResponseCode(ResponseCode.esError.getResponseCode());
        }
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return resp;
    }


    @GetMapping("/v1/course/new")
    public Response getNewCourses(@RequestParam(value = "resourceCount", defaultValue = "5") String numberOfResources,
                                  @RequestParam(value = "pageNumber", defaultValue = "0") String pageNumber,
                                  @RequestParam(value = "contentType", defaultValue = "") String contentType
    ) {
    	String url ="/v1/course/new";
        Response resp = new Response();
        ProjectLogger.log("New courses request recieved");
		resp.setVer("v1");
        resp.setId("api.courses.new");
        resp.setTs(ProjectUtil.getFormattedDate());
        Long start = System.currentTimeMillis();
        try {
            List<Object> allEntries = genericRecommendationService.getNewCourses(numberOfResources, pageNumber, contentType);

            ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
            code.setResponseCode(ResponseCode.OK.getResponseCode());
            ResponseParams params = new ResponseParams();
            params.setMsgid(ExecutionContext.getRequestId());
            params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
            resp.setParams(params);


            resp.put(Constants.RESPONSE, allEntries);
        } catch (Exception e) {
            ResponseCode code = ResponseCode.getResponse(ResponseCode.esError.getErrorCode());
            code.setResponseCode(ResponseCode.esError.getResponseCode());
        }
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return resp;
    }
    
    @GetMapping("/v1/iap/assessments")
    public Response getIAPAssessments(@RequestParam(value = "resourceCount", defaultValue = "5") String numberOfResources,
                                  @RequestParam(value = "pageNumber", defaultValue = "0") String pageNumber
    ) {
    	String url="/v1/iap/assessments";
        Response resp = new Response();
        ProjectLogger.log("Asssesments request recieved");
		resp.setVer("v1");
        resp.setId("api.iap.assessments");
        resp.setTs(ProjectUtil.getFormattedDate());
        Long start = System.currentTimeMillis();
        try {
            List<Object> allEntries = genericRecommendationService.getAssessments(numberOfResources, pageNumber);

            ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
            code.setResponseCode(ResponseCode.OK.getResponseCode());
            ResponseParams params = new ResponseParams();
            params.setMsgid(ExecutionContext.getRequestId());
            params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
            resp.setParams(params);
            resp.put(Constants.RESPONSE, allEntries);
        } catch (Exception e) {
            ResponseCode code = ResponseCode.getResponse(ResponseCode.esError.getErrorCode());
            code.setResponseCode(ResponseCode.esError.getResponseCode());
        }
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return resp;

    }


}
