/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import com.infosys.model.UserProfileDetails;

import java.util.List;
import java.util.Map;


public interface ProfileService {

    public UserProfileDetails getUserData(String userEmail) throws Exception;

    public List<Map<String, Object>> getMultipleUserData(List<String> userEmails) throws Exception;
	
	public byte[] getUserPhoto(String userEmail);

    public List<String> getRole(String userEmail) throws Exception;

    UserProfileDetails getUserDataByUUID(String userUUID) throws Exception;

    byte[] getUserPhotoByUUID(String userUUID) throws Exception;

    String getEmailByUUID(String userUUID) throws Exception;

    public Map<String, Object> fetchUserData(String idValue, String idType) throws Exception;

}
