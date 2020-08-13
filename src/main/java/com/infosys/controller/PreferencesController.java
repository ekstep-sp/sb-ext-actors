/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.BadRequestException;
import com.infosys.exception.NoContentException;
import com.infosys.exception.ResourceNotFoundException;
import com.infosys.service.PreferencesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class PreferencesController {

	@Autowired
	PreferencesService preferencesService;

	@GetMapping("/v1/users/{identifier}/preferences")
	public ResponseEntity<?> getUserPrefereces(@PathVariable("identifier") String identifier,
											   @RequestParam(name = "user_id_type", defaultValue = "uuid", required = false) String idType)
			throws JsonParseException, JsonMappingException, IOException {
		Map<String, Object> responseMap = new HashMap<>();
		try {
			responseMap = preferencesService.getPreferences(idType, identifier);
		} catch (BadRequestException bre) {
			responseMap.put("error", bre.getMessage());
			return new ResponseEntity<Map<String, Object>>(responseMap, HttpStatus.BAD_REQUEST);
		} catch (NoContentException nce) {
			responseMap.put("error", nce.getMessage());
			return new ResponseEntity<Map<String, Object>>(responseMap, HttpStatus.NO_CONTENT);
		} catch (ResourceNotFoundException rnfe) {
			responseMap.put("error", rnfe.getMessage());
			return new ResponseEntity<Map<String, Object>>(responseMap, HttpStatus.NOT_FOUND);
		} catch (ApplicationLogicError exception) {
			responseMap.put("error", exception.getMessage());
			return new ResponseEntity<Map<String, Object>>(responseMap, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<Map<String, Object>>(responseMap, HttpStatus.OK);
	}

	@PutMapping("/v1/users/{identifier}/preferences")
	public ResponseEntity<Map<String, Object>> updateUserPrefereces(@PathVariable("identifier") String identifier,
			@RequestBody String preferencesData,
			@RequestParam(name = "user_id_type", defaultValue = "uuid", required = false) String idType) {
		Map<String, Object> responseMap = new HashMap<>();
		try {
			preferencesService.updatePreferences(idType, identifier, preferencesData);
		} catch (BadRequestException bre) {
			responseMap.put("error", bre.getMessage());
			return new ResponseEntity<Map<String, Object>>(responseMap, HttpStatus.BAD_REQUEST);
		} catch (ResourceNotFoundException rnfe) {
			responseMap.put("error", rnfe.getMessage());
			return new ResponseEntity<Map<String, Object>>(responseMap, HttpStatus.NOT_FOUND);
		} catch (ApplicationLogicError exception) {
			responseMap.put("error", exception.getMessage());
			return new ResponseEntity<Map<String, Object>>(responseMap, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
