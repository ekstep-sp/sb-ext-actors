/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.service.CourseBatchService;
import com.infosys.util.LexProjectUtil;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;

import java.io.IOException;
import java.util.Date;

@RestController
@CrossOrigin(origins = "*")
public class CourseBatchController {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    CourseBatchService courseBatchService;

    @PostMapping("/v1/course/batch/create")
    public ResponseEntity<JSONObject> createCourseBatch(@RequestBody JSONObject requestBody) throws IOException {
    	String url = "/v1/course/batch/create";
    	Long start = System.currentTimeMillis();
    	ResponseEntity<JSONObject> batchCreationResponse = courseBatchService.createCourseBatch(restTemplate, requestBody);
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return batchCreationResponse;
    }

}
