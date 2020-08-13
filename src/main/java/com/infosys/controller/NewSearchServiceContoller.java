/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.BadRequestException;
import com.infosys.exception.NoContentException;
import com.infosys.service.NewLexSearchService;
import com.infosys.service.NewSearchService;
import com.infosys.service.SearchTemplateService;
import com.infosys.util.ErrorGenerator;
import com.infosys.util.LexConstants;
import com.infosys.util.SearchConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class NewSearchServiceContoller {

	@Autowired
	NewLexSearchService newLexSearchService;

	@Autowired
	NewSearchService newSearchService;
	
	@Autowired
	SearchTemplateService searchTemplateService;

	@Autowired
	Environment environment;

	private boolean accessControlEnabled;
	private String rootOrg;

	@PostConstruct
	private void initialize() {
		accessControlEnabled = environment.getProperty(LexConstants.ENABLE_ACCESS_CONTROL, Boolean.class);
		rootOrg = environment.getProperty(LexConstants.ROOT_ORG, String.class);
	}
	
	@PostMapping("/search4")
	public ResponseEntity<Response> generalSearchTemplateAPI(@RequestBody Request request) {
		if (request == null) {
			throw new BadRequestException("API Contract not adhered.");
		}

		Map<String, Object> requestMap = request.getRequest();

		if (requestMap == null || requestMap.isEmpty()) {
			throw new BadRequestException("API Contract not adhered.");
		}
		
		Response resp = new Response();
		try{
			if (null != (String) requestMap.get(SearchConstants.ROOT_ORG)) {
				rootOrg = (String) requestMap.get(SearchConstants.ROOT_ORG);
				if (null == rootOrg || rootOrg.isEmpty()) {
					throw new ApplicationLogicError("NO DEFAULT VALUE FOR rootOrg FOUND");
				}
			}
			resp = searchTemplateService.generalSearchService(request, accessControlEnabled, rootOrg);
		}catch (BadRequestException e) {
			ProjectLogger.log("SearchV4", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData, "search4", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
		}catch(ApplicationLogicError e) {
			ProjectLogger.log("SearchV4", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError, "search4", "1", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}catch (NoContentException e) {
			ProjectLogger.log("SearchV4", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.noDataForConsumption, "search4", "1", e.getMessage()), HttpStatus.NO_CONTENT);
		}catch (Exception e) {
			e.printStackTrace();
			ProjectLogger.log("SearchV4", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError, "search4", "1", "controller error"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		resp.setVer("v1");
		resp.setId("api.search4");
		resp.setTs(ProjectUtil.getFormattedDate());

		return new ResponseEntity<Response>(resp, HttpStatus.OK);
	}
	
	@PostMapping("/adminsearch4")
	public ResponseEntity<Response> adminSearchTemplateAPI(@RequestBody Request request) {
		if (request == null) {
			throw new BadRequestException("API Contract not adhered.");
		}

		Map<String, Object> requestMap = request.getRequest();

		if (requestMap == null || requestMap.isEmpty()) {
			throw new BadRequestException("API Contract not adhered.");
		}
		
		Response resp = new Response();
		try{
			if (null != (String) requestMap.get(SearchConstants.ROOT_ORG)) {
				rootOrg = (String) requestMap.get(SearchConstants.ROOT_ORG);
				if (null == rootOrg || rootOrg.isEmpty()) {
					throw new ApplicationLogicError("NO DEFAULT VALUE FOR rootOrg FOUND");
				}
			}
			resp = searchTemplateService.adminSearchService(request, accessControlEnabled, rootOrg);
		}catch (BadRequestException e) {
			ProjectLogger.log("adminSearch4", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData, "adminSearch4", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
		}catch(ApplicationLogicError e) {
			ProjectLogger.log("adminSearch4", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError, "adminSearch4", "1", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}catch (NoContentException e) {
			ProjectLogger.log("adminSearch4", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.noDataForConsumption, "adminSearch4", "1", e.getMessage()), HttpStatus.NO_CONTENT);
		}catch (Exception e) {
			e.printStackTrace();
			ProjectLogger.log("adminSearch4", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError, "adminSearch4", "1", "controller error"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		resp.setVer("v1");
		resp.setId("api.adminSearch4");
		resp.setTs(ProjectUtil.getFormattedDate());

		return new ResponseEntity<Response>(resp, HttpStatus.OK);
	}
	
	@PostMapping("/authsearch4")
	public ResponseEntity<Response> authoringSearchTemplateAPI(@RequestBody Request request) {
		if (request == null) {
			throw new BadRequestException("API Contract not adhered.");
		}

		Map<String, Object> requestMap = request.getRequest();

		if (requestMap == null || requestMap.isEmpty()) {
			throw new BadRequestException("API Contract not adhered.");
		}
		
		Response resp = new Response();
		try{
			if (null != (String) requestMap.get(SearchConstants.ROOT_ORG)) {
				rootOrg = (String) requestMap.get(SearchConstants.ROOT_ORG);
				if (null == rootOrg || rootOrg.isEmpty()) {
					throw new ApplicationLogicError("NO DEFAULT VALUE FOR rootOrg FOUND");
				}
			}
			resp = searchTemplateService.authoringSearchService(request, accessControlEnabled, rootOrg);
		}catch (BadRequestException e) {
			ProjectLogger.log("authSearch4", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData, "adminSearch4", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
		}catch(ApplicationLogicError e) {
			ProjectLogger.log("authSearch4", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError, "adminSearch4", "1", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}catch (NoContentException e) {
			ProjectLogger.log("authSearch4", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.noDataForConsumption, "adminSearch4", "1", e.getMessage()), HttpStatus.NO_CONTENT);
		}catch (Exception e) {
			e.printStackTrace();
			ProjectLogger.log("authSearch4", e);
			return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError, "adminSearch4", "1", "controller error"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		resp.setVer("v1");
		resp.setId("api.authSearch4");
		resp.setTs(ProjectUtil.getFormattedDate());

		return new ResponseEntity<Response>(resp, HttpStatus.OK);
	}
	
	@PostMapping("/search3")
	public ResponseEntity<Response> newSearchAPI(@RequestBody Request request) throws IOException {
		String url ="/search3";
		if (request == null) {
			throw new BadRequestException("API Contract not adhered.");
		}

		Map<String, Object> requestMap = request.getRequest();

		if (requestMap == null || requestMap.isEmpty()) {
			throw new BadRequestException("API Contract not adhered.");
		}

		Long start = System.currentTimeMillis();
		Response resp = newLexSearchService.searchSearvice(request);

		if (resp.getResponseCode() == ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode())) {
			return new ResponseEntity<Response>(resp, HttpStatus.BAD_REQUEST);
		}

		try {
			Map<String, Object> responseMap = (Map<String, Object>) resp.get(JsonKey.RESPONSE);
			List<Map<String, Object>> listOfFilters = (List<Map<String, Object>>) responseMap.get(JsonKey.FILTERS);
			List<Map<String, Object>> listOfFiltersWithoutNull = new ArrayList<Map<String, Object>>();

			// This is for removing the filtes with null or empty entries.
			for (int i = 0; i < listOfFilters.size(); i++) {
				Map<String, Object> map = listOfFilters.get(i);
				List<Map<String, Object>> contentList = (List<Map<String, Object>>) map.get(JsonKey.CONTENT);
				if (contentList != null && !contentList.isEmpty())
					listOfFiltersWithoutNull.add(map);
			}

			responseMap.put(JsonKey.FILTERS, listOfFiltersWithoutNull);
			resp.put(JsonKey.RESPONSE, responseMap);
		} catch (NullPointerException npe) {
			throw new ApplicationLogicError("Error processing request.");
		}catch(ApplicationLogicError e)
		{
			System.out.println("Caught it");
		}

		resp.setVer("v1");
		resp.setId("api.search3");
		resp.setTs(ProjectUtil.getFormattedDate());

		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
		code.setResponseCode(ResponseCode.OK.getResponseCode());
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
		resp.setParams(params);
		//ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return new ResponseEntity<Response>(resp, HttpStatus.OK);
	}

	@PostMapping("/search2")
	public ResponseEntity<Response> searchAPI(@RequestBody Request request) throws IOException {
		if (request == null) {
			throw new BadRequestException("API Contract not adhered.");
		}
		String url = "/search2";
		Map<String, Object> requestMap = request.getRequest();

		if (requestMap == null || requestMap.isEmpty()) {
			throw new BadRequestException("API Contract not adhered.");
		}

		Response resp = newSearchService.searchSearvice(request);

		if (resp.getResponseCode() == ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode())) {
			return new ResponseEntity<Response>(resp, HttpStatus.BAD_REQUEST);
		}
		Long start = System.currentTimeMillis();
		try {
			Map<String, Object> responseMap = (Map<String, Object>) resp.get(JsonKey.RESPONSE);

			List<Map<String, Object>> listOfFilters = (List<Map<String, Object>>) responseMap.get(JsonKey.FILTERS);
			List<Map<String, Object>> listOfFiltersWithoutNull = new ArrayList<Map<String, Object>>();

			// This is for removing the filtes with null or empty entries.
			for (int i = 0; i < listOfFilters.size(); i++) {
				Map<String, Object> map = listOfFilters.get(i);
				List<Map<String, Object>> contentList = (List<Map<String, Object>>) map.get(JsonKey.CONTENT);
				if (contentList != null && !contentList.isEmpty())
					listOfFiltersWithoutNull.add(map);
			}

			responseMap.put(JsonKey.FILTERS, listOfFiltersWithoutNull);
			resp.put(JsonKey.RESPONSE, responseMap);
		} catch (NullPointerException npe) {
			throw new ApplicationLogicError("Error processing request.");
		}

		resp.setVer("v1");
		resp.setId("api.search2");
		resp.setTs(ProjectUtil.getFormattedDate());

		ResponseCode code = ResponseCode.getResponse(ResponseCode.success.getErrorCode());
		code.setResponseCode(ResponseCode.OK.getResponseCode());
		ResponseParams params = new ResponseParams();
		params.setMsgid(ExecutionContext.getRequestId());
		params.setStatus(ResponseCode.getHeaderResponseCode(code.getResponseCode()).name());
		resp.setParams(params);
		//ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return new ResponseEntity<Response>(resp, HttpStatus.OK);
	}

}
