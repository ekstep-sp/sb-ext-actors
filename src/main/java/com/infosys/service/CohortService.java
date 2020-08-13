/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/**
 * 
 */
package com.infosys.service;

import org.sunbird.common.models.response.Response;

/**
 * @author Gaurav_Kumar48
 *
 */

/**
 * this interface contains all methods relating to user goals and cohorts
 * feature
 */
public interface CohortService {
	Response getActiveUsers(String resourceId, String userEmail, int count);

	Response getListofSMEs(String resourceId, String userEmail, int count);
}
