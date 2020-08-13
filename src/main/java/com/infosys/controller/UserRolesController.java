/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.exception.BadRequestException;
import com.infosys.service.UserRolesService;
import com.infosys.util.LexProjectUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.sunbird.common.Constants;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.List;
import java.util.Map;

@RestController
public class UserRolesController {

    @Autowired
    UserRolesService userRolesService;


    @GetMapping("v0/user/roles")
    public Response getUserRoles(@RequestParam(value="emailid",defaultValue="undefined") String userId) {
        Response resp = new Response();
        String url = "v0/user/roles";
        resp.setVer("v0");
        resp.setId("user.roles");
        resp.setTs(ProjectUtil.getFormattedDate());
        ResponseCode responseCode = resp.getResponseCode();
        List<String> roleList = null;
        
        if(userId.equals("bug")) {
        String s = null;
        System.out.println(s.charAt(0));		
        }
        
        if(userId.equals("undefined"))
        	throw new BadRequestException("Send a valid request email-id");
        Long start = System.currentTimeMillis();
        try {
            roleList = userRolesService.getUserRoles(userId);
            //System.out.println(roleList.get(0));
        } catch (ProjectCommonException e) {
            responseCode.setErrorCode(ResponseCode.cassandraConnectionEstablishmentFailed.getErrorCode());
            responseCode.setErrorMessage(ResponseCode.cassandraConnectionEstablishmentFailed.getErrorMessage());
            responseCode.setResponseCode(ResponseCode.cassandraConnectionEstablishmentFailed.getResponseCode());
            resp.setResponseCode(responseCode);
        }


        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(responseCode.getResponseCode()).name());
        if (responseCode.getResponseCode() != 200)
            params.setErr("Internal Server problem");
        resp.setParams(params);

        resp.put(Constants.RESPONSE, roleList);
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return resp;
    }
    
    
    
    @PatchMapping("/v1/update/roles")
    public Response getUserRoles(@RequestBody Request request) {
        Response resp = new Response();
        String url = "/v1/update/roles";
        resp.setVer("v1");
        resp.setId("add.publishers");
        resp.setTs(ProjectUtil.getFormattedDate());
        ResponseCode responseCode = resp.getResponseCode();
        List<String> roleList = null;
        
        Map<String,Object> requestMap = request.getRequest();
        
        List<String> userIds = (List<String>) requestMap.get("userIds");
        String operation = (String) requestMap.get("operation");
        List<String> roles = (List<String>) requestMap.get("roles");
        
        
        if(!operation.toLowerCase().trim().equals("add") && !operation.toLowerCase().trim().equals("remove"))
           throw new BadRequestException("Unsupported Operation");
        	
        if(userIds==null || userIds.isEmpty())
        	throw new BadRequestException("Send a valid email-Id");
        
       if(roles==null || roles.isEmpty())
    	   throw new BadRequestException("Send non empty roles");
       Long start = System.currentTimeMillis();
        try {
        	for(int i=0;i<userIds.size();i++)
        	{
        	if(operation.trim().toLowerCase().equals("add"))
               userRolesService.addPublishers(userIds.get(i), roles);
        	else if(operation.trim().toLowerCase().equals("remove"))
        	   userRolesService.removePublishers(userIds.get(i), roles);
        	else
        		 throw new BadRequestException("Unsupported Operation");
        	}
            //System.out.println(roleList.get(0));
        }
        catch (ProjectCommonException e) {
            responseCode.setErrorCode(ResponseCode.cassandraConnectionEstablishmentFailed.getErrorCode());
            responseCode.setErrorMessage(ResponseCode.cassandraConnectionEstablishmentFailed.getErrorMessage());
            responseCode.setResponseCode(ResponseCode.cassandraConnectionEstablishmentFailed.getResponseCode());
            resp.setResponseCode(responseCode);
        }


        ResponseParams params = new ResponseParams();
        params.setMsgid(ExecutionContext.getRequestId());
        params.setStatus(ResponseCode.getHeaderResponseCode(responseCode.getResponseCode()).name());
        if (responseCode.getResponseCode() != 200)
            params.setErr("Internal Server problem");
        resp.setParams(params);

        //resp.put(Constants.RESPONSE, roleList);
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return resp;
    }
}
