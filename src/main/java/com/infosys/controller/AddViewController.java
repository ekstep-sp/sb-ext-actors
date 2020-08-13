/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.util.LexProjectUtil;

import org.springframework.web.bind.annotation.*;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.Date;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class AddViewController {

	@PostMapping("/v1/view/{resourceId}")
	public Response updateViews(@RequestBody Request requestBody, @PathVariable("resourceId") String resourceId) {
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.addViewCount");
		resp.setTs(ProjectUtil.getFormattedDate());
		Date startTime = new Date();
		try {
			
            System.out.println(resourceId);
            Map<String, Object> resourceResult = ElasticSearchUtil.getDataByIdentifier(
                    LexProjectUtil.EsIndex.bodhi.getIndexName(), LexProjectUtil.EsType.resource.getTypeName(), resourceId);
            System.out.println(resourceResult);
            Integer viewCount = (Integer) resourceResult.get("me_totalSessionsCount");
            viewCount += 1;
			resourceResult.put("me_totalSessionsCount", viewCount);

            ElasticSearchUtil.upsertData(LexProjectUtil.EsIndex.bodhi.getIndexName(),
                    LexProjectUtil.EsType.resource.getTypeName(), resourceId, resourceResult);

		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.dbInsertionError.getErrorCode());

			resp.setResponseCode(responseCode);
			ProjectLogger.log("Update Views Started At " + startTime + "for LexId : " + resourceId,LoggerEnum.INFO);			
			ProjectLogger.log("Update Views Ended At " + new Date() + "for LexId : " + resourceId,LoggerEnum.INFO);
			return resp;
		}
		try {
			Map<String, Object> reqMap = requestBody.getRequest();
			String courseId = (String) reqMap.get("courseId");

			Map<String, Object> courseResult = ElasticSearchUtil.getDataByIdentifier(
                    LexProjectUtil.EsIndex.bodhi.getIndexName(), LexProjectUtil.EsType.resource.getTypeName(), courseId);

			Integer viewCount = (Integer) courseResult.get("me_totalSessionsCount");
			viewCount += 1;
			courseResult.put("me_totalSessionsCount", viewCount);

            ElasticSearchUtil.upsertData(LexProjectUtil.EsIndex.bodhi.getIndexName(),
                    LexProjectUtil.EsType.resource.getTypeName(), courseId, courseResult);

			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
			responseCode.setResponseCode(ResponseCode.OK.getResponseCode());
			resp.setResponseCode(responseCode);
		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.dbInsertionError.getErrorCode());

			resp.setResponseCode(responseCode);

			return resp;
		}

		return resp;
	}

}
