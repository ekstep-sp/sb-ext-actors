/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/**
© 2017 - 2019 Infosys Limited, Bangalore, India. All Rights Reserved. 
Version: 1.10

Except for any free or open source software components embedded in this Infosys proprietary software program (“Program”),
this Program is protected by copyright laws, international treaties and other pending or existing intellectual property rights in India,
the United States and other countries. Except as expressly permitted, any unauthorized reproduction, storage, transmission in any form or
by any means (including without limitation electronic, mechanical, printing, photocopying, recording or otherwise), or any distribution of 
this Program, or any portion of it, may result in severe civil and criminal penalties, and will be prosecuted to the maximum extent possible
under the law.

Highly Confidential
 
*/

package com.infosys.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infosys.service.TodoService;

@RestController
@CrossOrigin(origins = "*")
public class TodoController {

	@Autowired
	TodoService todoServ;

	/*
	 * get the page dropdown data
	 */
	@GetMapping("/v1/users/{user_id}/task_groups/{group_id}/tasks")
	public ResponseEntity<List<Map<String, Object>>> getTodoData(@PathVariable("user_id") String userId,
			@PathVariable("group_id") String groupId,
			@RequestParam(required = false, defaultValue = "uuid", value = "user_id_type") String uidType)
			throws Exception {
		return new ResponseEntity<List<Map<String, Object>>>(todoServ.getTodoInfo(userId, groupId, uidType),
				HttpStatus.OK);

	}

	
	/*
	 * get the todo data using content progress in cassandra
	 */
	@GetMapping("/v2/users/{user_id}/task_groups/{group_id}/tasks")
	public ResponseEntity<List<Map<String, Object>>> getTodoDataUsingCassandra(@PathVariable("user_id") String userId,
			@PathVariable("group_id") String groupId)
			throws Exception {
		return new ResponseEntity<List<Map<String, Object>>>(todoServ.getTodoInfoUsingNewContentProgress(userId, groupId),
				HttpStatus.OK);

	}

}
