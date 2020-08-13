/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.infosys.service.UserUtilityService;

@RestController
@CrossOrigin(origins = "*")
public class TestController {

	@Autowired
	UserUtilityService svc;

	@GetMapping("/v1/users/validate/{user_id}")
	public ResponseEntity<?> validateUser(@RequestHeader(name = "rootOrg") String rootOrg,
			@PathVariable("user_id") String userId) {

		return new ResponseEntity<>(svc.validateUser(rootOrg, userId), HttpStatus.OK);
	}

	@SuppressWarnings("unchecked")
	@GetMapping("/v1/users/validate")
	public ResponseEntity<?> validateUsers(@RequestHeader(name = "rootOrg") String rootOrg,
			@RequestBody Map<String, Object> userIds) {

		return new ResponseEntity<>(svc.validateUsers(rootOrg, (List<String>) userIds.get("users")), HttpStatus.OK);
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/v1/users/details")
	public ResponseEntity<?> getUsersDataFromUserIds(@RequestHeader(name = "rootOrg") String rootOrg,
			@RequestBody Map<String, Object> data) {

		return new ResponseEntity<>(svc.getUsersDataFromUserIds(rootOrg, (List<String>) data.get("users"),
				(List<String>) data.get("sources")), HttpStatus.OK);
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/v1/users/{user_id}/details")
	public ResponseEntity<?> getUserDataFromUserId(@RequestHeader(name = "rootOrg") String rootOrg,
			@RequestBody Map<String, Object> data, @PathVariable("user_id") String userId) {

		return new ResponseEntity<>(svc.getUserDataFromUserId(rootOrg, userId, (List<String>) data.get("sources")),
				HttpStatus.OK);
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/v1/users/emails")
	public ResponseEntity<?> getUserEmailsFromUserIds(@RequestHeader(name = "rootOrg") String rootOrg,
			@RequestBody Map<String, Object> data) {

		return new ResponseEntity<>(svc.getUserEmailsFromUserIds(rootOrg, (List<String>) data.get("users")),
				HttpStatus.OK);
	}

	@GetMapping("/v1/users/{user_id}/email")
	public ResponseEntity<?> getUserEmailFromUserId(@RequestHeader(name = "rootOrg") String rootOrg,
			@PathVariable("user_id") String userId) {

		return new ResponseEntity<>(svc.getUserEmailFromUserId(rootOrg, userId), HttpStatus.OK);
	}
}
