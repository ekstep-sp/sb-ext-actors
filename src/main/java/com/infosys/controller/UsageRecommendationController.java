/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.exception.BadRequestException;
import com.infosys.model.CourseRecommendation;
import com.infosys.service.UsageRecommendationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;

import java.util.List;
import java.util.Map;


@RestController
@CrossOrigin(origins = "*")
public class UsageRecommendationController {

    @Autowired
    UsageRecommendationService usageRecService;

    @GetMapping("/v1/recommendation/{email}/usage")
    public ResponseEntity<Response> getUsageRecommendations(@PathVariable("email") String emailId, @RequestParam(value = "pageSize", defaultValue = "10") String pageSize,
    		@RequestParam(value = "pageNumber", defaultValue = "0") String pageNumber, @RequestParam(value = "contentIds", defaultValue = "") String contentIdsStr) {
    	Response resp = new Response();
        resp.setVer("v1");
        resp.setId("api.usageRecommendations");
        resp.setTs(ProjectUtil.getFormattedDate());
        try {
            if (!contentIdsStr.equals("")) {
                String[] contentIds = contentIdsStr.split(",");
                Map<String, List<CourseRecommendation>> courseRecsForIds = usageRecService.getUsageRecommendationsForCourses(contentIds);
                resp.put(Constants.RESPONSE, courseRecsForIds);
            } else {
                List<CourseRecommendation> courseList = usageRecService.getUsageRecommendations(emailId, pageSize, pageNumber);
                resp.put(Constants.RESPONSE, courseList);
            }
        } catch(BadRequestException ex) {
			resp.put("error", ex.getMessage());
        	return new ResponseEntity<Response>(resp, HttpStatus.BAD_REQUEST);
		} catch(Exception ex) {
			resp.put("error", ex.getMessage());
			return new ResponseEntity<Response>(resp, HttpStatus.INTERNAL_SERVER_ERROR);
		}
        
        return new ResponseEntity<Response>(resp, HttpStatus.OK);
    }
    
}
