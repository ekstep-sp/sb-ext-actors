/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.infosys.service.CassandraMigrationService;

@RestController
@CrossOrigin(origins = "*")
public class CassandraMigrationController {
	
	@Autowired
	CassandraMigrationService migrationServiec;

	@PostMapping(consumes = { "multipart/form-data" }, value = "/v1/infyme/migrate")
	public ResponseEntity<Boolean> submitExercise(@RequestParam(value = "file", required = true) MultipartFile file) throws Exception {
		return new ResponseEntity<Boolean>(migrationServiec.migrateFile(file), HttpStatus.OK);
	}

}
