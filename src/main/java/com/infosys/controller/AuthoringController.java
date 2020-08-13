/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.models.response.Response;

@RestController
@CrossOrigin(origins = "*")
public class AuthoringController {

	/**
	 * Invoked when the author needs to see all the reviewers present in the system
	 * Contract
	 *
	 * @return Response [ { "id": "id-1", "email": "user01@" }, {
	 *         "id": "id-2", "email": "user02@" }, ]
	 */
	@GetMapping("/v1/reviewers/read")
	public Response getAllReviewers() {
		return null;
	}
}
