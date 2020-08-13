/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.io.IOException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;

import com.infosys.service.ConceptsService;
import com.infosys.util.LexConstants;
import com.infosys.util.LexProjectUtil;

@RestController
@CrossOrigin(origins = "*")
public class ConceptsController {
	
	@Autowired
	ConceptsService conceptsService;
	
	@GetMapping("/concepts")
	public Response getByIds(@RequestParam("ids") String ids) throws IOException {
		String url = "/concepts";
		Response resp = new Response();
		Long start = System.currentTimeMillis();      
		resp = conceptsService.getByIds(ids.split(","));
		//ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
		return resp;
	}
}
