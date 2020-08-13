/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.sunbird.common.models.response.Response;

public interface UserService {

	public Map<String, Object> findUUID(String email) throws Exception;

	public Response createUser(Map<String, Object> userData) throws IOException;

	public Map<String, Object> getSeparationDate(String Email) throws Exception;

	public Map<String, Object> findUuids(List<String> emails);

	public Map<String, String> toEmails(List<String> ids) throws Exception;

	Map<String, Object> verifyUUIDs(List<String> ids) throws Exception;
}
