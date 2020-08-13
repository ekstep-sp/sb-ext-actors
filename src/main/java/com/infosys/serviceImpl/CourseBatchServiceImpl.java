/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.infosys.service.CourseBatchService;
import org.json.simple.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;

import java.util.Collections;

import static com.infosys.helper.SSLHelper.disableSslVerification;

@Service
public class CourseBatchServiceImpl implements CourseBatchService {

    @Override
    public ResponseEntity<JSONObject> createCourseBatch(RestTemplate restTemplate, JSONObject requestBody) {

        disableSslVerification();

        HttpHeaders headers = new HttpHeaders();

        PropertiesCache properties = PropertiesCache.getInstance();

        String bearer = properties.getProperty("bearer");

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", bearer);

        HttpEntity<JSONObject> entity = new HttpEntity<JSONObject>(requestBody, headers);

        String batch_creation_url = properties.getProperty("create_batch_url");

        ProjectLogger.log("Create Batch request receive for url " + batch_creation_url, entity, LoggerEnum.INFO.name());

        ResponseEntity<JSONObject> batchCreationResponse = restTemplate.exchange(batch_creation_url, HttpMethod.POST, entity, JSONObject.class);

        return batchCreationResponse;
    }
}
