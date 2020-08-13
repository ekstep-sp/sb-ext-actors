/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.model.CourseRecommendation;
import com.infosys.service.TrendingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;
import java.util.List;
import java.util.ArrayList;

import java.util.List;

import javax.ws.rs.BadRequestException;

@RestController
@CrossOrigin(origins = "*")
public class TrendingController {
	@Autowired
	TrendingService trendingService;

	@GetMapping("/v1/course/trending/{userId}")
	public ResponseEntity<Response> getTrendingCourses(@PathVariable("userId") String userId,
			@RequestParam(value = "pageSize", defaultValue = "10") String pageSizeStr,
			@RequestParam(value = "pageNumber", defaultValue = "0") String pageNumberStr, 
			@RequestParam(value = "rootOrg", defaultValue = "Infosys") String rootOrg) {
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.trendingCourses");
		resp.setTs(ProjectUtil.getFormattedDate());

		try {
		    List<String> langCodes = new ArrayList<String>();
		    langCodes.add("en");
		    
		    
		    List<String> fields = new ArrayList<String>();
		    fields.add("status");
		    fields.add("identifier");
		    
			List<CourseRecommendation> courseList = trendingService.getTrendingCourses(rootOrg,userId, pageNumberStr,
					pageSizeStr,langCodes, fields);
			resp.put(Constants.RESPONSE, courseList);

		} catch(BadRequestException ex) {
			resp.put("error", ex.getMessage());
        	return new ResponseEntity<Response>(resp, HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<Response>(resp, HttpStatus.OK);
	}

	@GetMapping("/v1/course/trending")
	public ResponseEntity<Response> getTrendingCoursesOrg(@RequestParam(value = "pageSize", defaultValue = "10") String pageSizeStr,
			@RequestParam(value = "pageNumber", defaultValue = "0") String pageNumberStr) {
		Response resp = new Response();
		resp.setVer("v1");
		resp.setId("api.trendingCourses");
		resp.setTs(ProjectUtil.getFormattedDate());

		try {
			List<CourseRecommendation> courseList = trendingService.getTrendingCourses(null, pageNumberStr, pageSizeStr);			
			resp.put(Constants.RESPONSE, courseList);			
		} catch(BadRequestException ex) {
			resp.put("error", "pageSize and pageNumber must be an integer");
        	return new ResponseEntity<Response>(resp, HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<Response>(resp, HttpStatus.OK);
	}
}
