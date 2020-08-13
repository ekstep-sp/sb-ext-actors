/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.search;

import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.BadRequestException;
import com.infosys.exception.NoContentException;
import com.infosys.search.validations.model.SearchRequest;
import com.infosys.util.ErrorGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

import javax.annotation.PostConstruct;
import javax.validation.Valid;

@RestController
public class MultiLingualIntegratedSearchController {


    public static boolean isAccessControlEnabled;
    
    @Autowired
//    @Qualifier("GeneralMultiLingualIntegratedSearchService")
    private GeneralMultiLingualIntegratedSearchService generalSearchService;
    
    @Autowired
    private AdminMultiLingualIntegratedSearchService adminSearchService;
    
    @Autowired
    private AuthoringToolMultiLingualIntegratedSearchService authoringToolSearchService;
    
    @Value("${com.infosys.enable-access-control}")
    private boolean ace;

    @PostConstruct
    private void init() {
        isAccessControlEnabled = ace;
    }

    @PostMapping("/search5")
    public ResponseEntity<Response> generalSearchTemplateAPI(@Valid @RequestBody SearchRequest request) throws Exception {

        Response resp = new Response();
        try {
            if (null == request.getRequest()) {
                throw new BadRequestException("API Contract not adhered.");
            }
			resp = generalSearchService.performSearch(request.getRequest());

        } catch (BadRequestException e) {
            ProjectLogger.log("Search.v5", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData, "Search.v5", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            ProjectLogger.log("Search.v5", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError, "Search.v5", "1", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NoContentException e) {
            ProjectLogger.log("Search.v5", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.noDataForConsumption, "Search.v5", "1", e.getMessage()), HttpStatus.NO_CONTENT);
        }
        resp.setVer("v5");
        resp.setId("api.search.v5");
        resp.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(resp, HttpStatus.OK);
    }
    
    @PostMapping("/authsearch5")
    public ResponseEntity<Response> authoringToolSearchTemplateAPI(@Valid @RequestBody SearchRequest request) {

        Response resp = new Response();
        try {
            if (null == request.getRequest()) {
                throw new BadRequestException("API Contract not adhered.");
            }
			resp = authoringToolSearchService.performSearch(request.getRequest());

        } catch (BadRequestException e) {
            ProjectLogger.log("Search.auth.v5", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData, "Search.auth.v5", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            ProjectLogger.log("Search.auth.v5", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError, "Search.auth.v5", "1", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NoContentException e) {
            ProjectLogger.log("Search.auth.v5", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.noDataForConsumption, "Search.auth.v5", "1", e.getMessage()), HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("Search.auth.v5", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError, "Search.auth.v5", "1", "controller error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        resp.setVer("v5");
        resp.setId("api.search.auth.v5");
        resp.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(resp, HttpStatus.OK);
    }
    
    @PostMapping("/adminsearch5")
    public ResponseEntity<Response> adminSearchTemplateAPI(@Valid @RequestBody SearchRequest request) {

        Response resp = new Response();
        try {
            if (null == request.getRequest()) {
                throw new BadRequestException("API Contract not adhered.");
            }
			resp = adminSearchService.performSearch(request.getRequest());

        } catch (BadRequestException e) {
            ProjectLogger.log("Search.admin.v5", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.invalidRequestData, "Search.admin.v5", "1", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ApplicationLogicError e) {
            ProjectLogger.log("Search.admin.v5", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError, "Search.admin.v5", "1", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NoContentException e) {
            ProjectLogger.log("Search.admin.v5", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.noDataForConsumption, "Search.admin.v5", "1", e.getMessage()), HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            e.printStackTrace();
            ProjectLogger.log("Search.admin.v5", e);
            return new ResponseEntity<Response>(ErrorGenerator.generateErrorResponse(ResponseCode.internalError, "Search.admin.v5", "1", "controller error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        resp.setVer("v5");
        resp.setId("api.search.admin.v5");
        resp.setTs(ProjectUtil.getFormattedDate());

        return new ResponseEntity<Response>(resp, HttpStatus.OK);
    }
}
