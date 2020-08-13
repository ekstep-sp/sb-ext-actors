/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infosys.service.UserStartupService;

@RestController
public class UserStartupController {

	@Autowired
	UserStartupService userService;

	@GetMapping("/v0/user/{userId}/startup")
	public Map<String, Object> getUserStartupDetails(@PathVariable String userId,@RequestParam(required = false,defaultValue="false", name = "isAuthor") boolean isAuthor) {
		Map<String, Object> resultMap = new HashMap<>();

		try {
			resultMap = userService.getUserStartupDetails(userId,isAuthor);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultMap;
	}
}
