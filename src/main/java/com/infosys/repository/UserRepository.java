/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.repository;

import java.util.List;
import java.util.Map;

public interface UserRepository {

	public Map<String, Object> getUUIDFromEmail(String email) throws Exception;

	public Map<String, Object> getEmailFromUUID(String uuid) throws Exception;

	public Map<String, Object> getUserDetails(String idValue, String idType) throws Exception;

	Map<String, Object> getUUIDsFromEmails(List<String> emails) throws Exception;

	List<Map<String, Object>> getAutoCompleteEmails(String queyString) throws Exception;

	Map<String, String> getEmailsFromUUIDS(List<String> uuids) throws Exception;

	List<String> findUUIDs(List<String> uuids) throws Exception;
}
