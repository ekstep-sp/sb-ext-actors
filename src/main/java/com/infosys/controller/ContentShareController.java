package com.infosys.controller;

import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.BadRequestException;
import com.infosys.exception.NoContentException;
import com.infosys.exception.PropertiesNotFoundException;
import com.infosys.service.ContentShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.common.Constants;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ContentShareController {

    @Autowired
    private ContentShareService contentShareService;

    @PostMapping("/v1/content/share")
    public ResponseEntity<Map<String, Object>> createContentPublicUrl(@RequestBody Request requestBody,
                                               @RequestHeader(required = false, name = "rootOrg") String rootOrg,
                                               @RequestHeader(required = false, name = "org") String org,
                                               @RequestHeader(required = false, name = "wid") String wid) {
        HttpStatus httpStatus = null;

        Map<String, Object> reqMap = requestBody.getRequest();
        reqMap.put("rootOrg", rootOrg);
        reqMap.put("org", org);
        reqMap.put("wid", wid);
        Date startTime = new Date();
        Map<String, Object> response = new HashMap<>();

        try {
            response = contentShareService.createContentPublicUrl(reqMap);
            ProjectLogger.log("Creation of Content Public Url Started at " + startTime, LoggerEnum.INFO);
            httpStatus = HttpStatus.OK;
        } catch (BadRequestException e) {
            ProjectLogger.log("Creation of Content Public Url Failed due to Bad Request.", e);
            response.put("errmsg", e.getMessage());
            httpStatus = HttpStatus.BAD_REQUEST;
        } catch (PropertiesNotFoundException e) {
            ProjectLogger.log("Creation of Content Public Url Failed due to Internal Error.", e);
            response.put("errmsg", e.getMessage());
            httpStatus = HttpStatus.BAD_REQUEST;
        } catch (ApplicationLogicError e) {
            ProjectLogger.log("Creation of Content Public Url Failed due to Internal Error.", e);
            response.put("errmsg", e.getMessage());
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        } catch (Exception e) {
            ProjectLogger.log("Creation of Content Public Url Failed due to Internal Error.", e);
            response.put("errmsg", "Controller Error");
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ResponseEntity<Map<String, Object>>(response, httpStatus);
    }

    @GetMapping("/v1/content/share/validate/{shareableToken}")
    public ResponseEntity<Map<String, Object>> getAssessmentByContentUser(@RequestHeader(required = false, name = "rootOrg") String rootOrg,
                                                               @RequestHeader(required = false, name = "org") String org,
                                                               @RequestParam(required = false, name = "pageType") String pageType,
                                                               @PathVariable("shareableToken") String publicToken) {
        Map<String, Object> response = new HashMap<>();
        HttpStatus httpStatus = null;
        Date startTime = new Date();

        try {
            response = contentShareService.verifyContentPublicToken(publicToken, rootOrg, org, pageType);
            ProjectLogger.log("Fetching of Public Content from ShareableToken Started at " + startTime, LoggerEnum.INFO);
            httpStatus = HttpStatus.OK;
        } catch (BadRequestException e) {
            ProjectLogger.log("Validation of Content Shareable Token Failed due to Bad Request.", e);
            response.put("errmsg", e.getMessage());
            httpStatus = HttpStatus.BAD_REQUEST;
        } catch (PropertiesNotFoundException e) {
            ProjectLogger.log("Validation of Content Shareable Token Failed due to Internal Error.", e);
            response.put("errmsg", e.getMessage());
            httpStatus = HttpStatus.BAD_REQUEST;
        } catch (ApplicationLogicError e) {
            ProjectLogger.log("Validation of Content Shareable Token Failed due to Internal Error.", e);
            response.put("errmsg", e.getMessage());
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }  catch (NoContentException e) {
            ProjectLogger.log("Np content found with the provided Token.", e);
            response.put("errmsg", e.getMessage());
            httpStatus = HttpStatus.NOT_FOUND;
        } catch (Exception e) {
            ProjectLogger.log("Validation of Content Shareable Token Failed due to Internal Error.", e);
            response.put("errmsg", "Controller Error");
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ResponseEntity<Map<String, Object>>(response, httpStatus);
    }
}
