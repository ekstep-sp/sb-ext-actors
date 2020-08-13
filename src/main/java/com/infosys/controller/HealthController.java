/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class HealthController {

    @Autowired
    HealthService healthService;

    @GetMapping("/v1/application/check-connection")
    public ResponseEntity<Integer> getHealth() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/_health")
    public ResponseEntity<Map<String, Object>> generateBagdes() {
        Map<String, Object> ret = new HashMap<String, Object>();
        HttpStatus status = HttpStatus.OK;
        try {
            ret = healthService.checkHealth();
        } catch (Exception e) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<Map<String, Object>>(ret, status);
    }

}