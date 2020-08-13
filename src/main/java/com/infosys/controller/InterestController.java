/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.exception.BadRequestException;
import com.infosys.exception.ResourceNotFoundException;
import com.infosys.model.CourseRecommendation;
import com.infosys.service.InterestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ProjectUtil;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class InterestController {
    @Autowired
    InterestService interestService;

    @GetMapping("/v1/interested/{uid}")
    public ResponseEntity<Response> getInterestedCourses(@PathVariable("uid") String userId, @RequestParam(value = "resourceCount", defaultValue = "5") String numberOfResources,
                                         @RequestParam(value = "pageNumber", defaultValue = "0") String pageNumber) {

        Response resp = new Response();
        resp.setVer("v1");
        resp.setId("api.interestedCourses");
        resp.setTs(ProjectUtil.getFormattedDate());

        try {
            List<CourseRecommendation> courseList = interestService.getInterestedCourses(userId, numberOfResources, pageNumber);
            resp.put(Constants.RESPONSE, courseList);

            return new ResponseEntity<Response>(resp, HttpStatus.OK);
        } catch (BadRequestException ex) {
        	resp.put("error", ex.getMessage());
			return new ResponseEntity<Response>(resp, HttpStatus.BAD_REQUEST);
        } catch (ResourceNotFoundException ex) {
        	resp.put("error", "User Id does not exist");
        	return new ResponseEntity<Response>(resp, HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
        	resp.put("error", ex.getMessage());
			return new ResponseEntity<Response>(resp, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}
