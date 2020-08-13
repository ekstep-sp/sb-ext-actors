/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
//package com.infosys.controller;
//
//import java.io.IOException;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.CrossOrigin;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.sunbird.common.models.response.Response;
//import org.sunbird.common.models.response.ResponseParams;
//import org.sunbird.common.models.util.ProjectUtil;
//import org.sunbird.common.request.ExecutionContext;
//import org.sunbird.common.responsecode.ResponseCode;
//
//import com.infosys.service.ViewCountService;
//
//@RestController
//@CrossOrigin(origins = "*")
//public class viewCountController {
//
//	@Autowired
//	ViewCountService viewCountService;
//
//	@PostMapping("/v1/addView/{resourceId}")
//	public Response updateViews(@PathVariable("resourceId") String resourceId) throws IOException {
//		String url ="/v1/addView/" + resourceId;
//		Response resp = new Response();
//		resp.setVer("v1");
//		resp.setId("api.addViewCount");
//		resp.setTs(ProjectUtil.getFormattedDate());
//		Long start = System.currentTimeMillis();
//		viewCountService.updateViewCount(resourceId);
//
//		ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
//		resp.setResponseCode(responseCode);
//		//ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
//		return resp;
//	}
//
//	@GetMapping("/v1/viewCount/{resourceId}")
//	public Response getViews(@PathVariable("resourceId") String resourceId) throws IOException {
//		String url ="/v1/viewCount/"+resourceId;
//		Response resp = new Response();
//		resp.setVer("v1");
//		resp.setId("api.views.count");
//		resp.setTs(ProjectUtil.getFormattedDate());
//		Long start = System.currentTimeMillis();
//		Integer viewCount = viewCountService.getViewCount(resourceId);
//
//		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
//		code.setResponseCode(ResponseCode.OK.getResponseCode());
//
//		ResponseParams respParams = new ResponseParams();
//		respParams.setMsgid(ExecutionContext.getRequestId());
//		respParams.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
//		resp.setParams(respParams);
//
//		resp.put("ViewCount", viewCount);
//		//ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
//		return resp;
//	}
//
//}