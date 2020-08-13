/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.exception.InvalidDataInputException;
import com.infosys.service.AutoCompleteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AutoCompleteController {

    @Autowired
    AutoCompleteService autoCompleteService;

    @PostMapping("/v1/autoComplete")
    public Map<String, Object> getMetaDetailsForAuthoringTool(@RequestBody Request requestBody,
                                                              @RequestParam(value = "field", defaultValue = "name") String fieldName) {

        Map<String, Object> reqMap = requestBody.getRequest();
        String prefix = (String) reqMap.get("prefix");
        Date startTime = new Date();
        Response resp = autoCompleteService.getSuggestionsForQuery(prefix, fieldName);
		ProjectLogger.log("Fetching Content meta details Service at  " + startTime,LoggerEnum.INFO);
        ProjectLogger.log("Ending Content meta details Service at " + new Date(),LoggerEnum.INFO);
        resp.setVer("v1");
        resp.setId("api.autocomplete");
        resp.setTs(ProjectUtil.getFormattedDate());

        ResponseCode responseCode = resp.getResponseCode();

        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(responseCode.getResponseCode()).name());
        if (responseCode.getResponseCode() != 200)
            params.setErr("Internal Server problem");
        resp.setParams(params);

        Map<String, Object> dataMap = new HashMap<String, Object>();
        dataMap.put("data", resp);
        return dataMap;
    }
    
    @GetMapping("/v1/autoComplete/{search}/Data")
	public ResponseEntity<Response> getUserData(@PathVariable("search") String searchString) {
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.user.autoComplete");
		
		resp.setTs(ProjectUtil.getFormattedDate());
		Date startTime = new Date();
		try {
			if (searchString == null) {
				throw new Exception();
			}
			resp.put(Constants.RESPONSE, autoCompleteService.getSearchData(searchString));
			ProjectLogger.log("Fetching user Search data Service at  " + startTime,LoggerEnum.INFO);
			ProjectLogger.log("Ending user Search data Service at  " + new Date(),LoggerEnum.INFO);
		} 
		catch (InvalidDataInputException e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
			resp.put("error", e.getMessage());
			return new ResponseEntity<Response>(resp,HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			ResponseCode responseCode = ResponseCode.getResponse(ResponseCode.invalidRequestData.getErrorCode());
			resp.setResponseCode(responseCode);
			return new ResponseEntity<Response>(resp,HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<Response>(resp,HttpStatus.OK);
	}
}
