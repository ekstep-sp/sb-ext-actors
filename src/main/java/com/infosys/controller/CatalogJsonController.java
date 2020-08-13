/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@RestController
@CrossOrigin(origins = "*")
public class CatalogJsonController {

    @GetMapping("action/meta/v1/catalog/structure")
    public Response getContentStructure() {
        Response response = new Response();
        response.setVer("v1");
        response.setId("api.meta.catalog.structure");
        response.setTs(ProjectUtil.getFormattedDate());
        ResponseCode responseCode = null;

        responseCode = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
        responseCode.setResponseCode(ResponseCode.OK.getResponseCode());

//        List<Map<String, Object>> catalog = new ArrayList<>();
        List<Map<String, Object>> newServices = new ArrayList<>();
        String line;
        try {
//            InputStream in = this.getClass().getResourceAsStream("/catalogJSON.txt");
//            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//            StringBuffer catalogJSONString = new StringBuffer();
//            while ((line = reader.readLine()) != null) {
//                line = line.trim();
//                catalogJSONString.append(line);
//            }
//            catalog = new ObjectMapper().readValue(catalogJSONString.toString(), List.class);

            InputStream in = this.getClass().getResourceAsStream("/newServicesJsonV1.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuffer newServicesJSONString = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                newServicesJSONString.append(line);
            }
//            System.out.println(newServicesJSONString.toString().substring(60990, 61000));
            newServices = new ObjectMapper().readValue(newServicesJSONString.toString(), List.class);
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        response.put("tags", newServices);
//        response.put("catalog", catalog);
        return response;
    }

//    @PostMapping("action/meta/v1/catalog/structure")
//    public Response addUserContentDownload(@RequestBody Request req) {
//        ProjectLogger.log("Catalog Meta structure change request received");
//
//        Map<String, Object> reqMap = req.getRequest();
//
//        //Setting other attributes of response like version, timestamp, response param etc.
//        Response response = new Response();
//        response.setVer("v1");
//        response.setId("api.meta.catalog.structure");
//        response.setTs(ProjectUtil.getFormattedDate());
//        ResponseCode respCode = null;
//
//        String responseCode = "SUCCESS";
//
//        List<Map<String, Object>> tags = (List<Map<String, Object>>) reqMap.get("tags");
//
//
//        ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
//        code.setResponseCode(ResponseCode.OK.getResponseCode());
//        //Preparing and setting response param
//        ResponseParams params = new ResponseParams();
//        params.setMsgid(ExecutionContext.getRequestId());
//        params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
//        response.setParams(params);
//
//        response.put(Constants.RESPONSE, responseCode);
//        return response;
//    }
}

