/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/**
 * © 2017 - 2019 Infosys Limited, Bangalore, India. All Rights Reserved.
 * Version: 1.10
 * <p>
 * Except for any free or open source software components embedded in this Infosys proprietary software program (“Program”),
 * this Program is protected by copyright laws, international treaties and other pending or existing intellectual property rights in India,
 * the United States and other countries. Except as expressly permitted, any unauthorized reproduction, storage, transmission in any form or
 * by any means (including without limitation electronic, mechanical, printing, photocopying, recording or otherwise), or any distribution of
 * this Program, or any portion of it, may result in severe civil and criminal penalties, and will be prosecuted to the maximum extent possible
 * under the law.
 * <p>
 * Highly Confidential
 */
package com.infosys.search.decorators;

import com.infosys.model.cassandra.UserAccessPathsModel;
import com.infosys.search.validations.model.ValidatedSearchData;
import com.infosys.service.AdminAccessControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sunbird.common.models.response.Response;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Component
public class OrgsDecorator {

    @Autowired
    private AdminAccessControlService accessControlService;

    @SuppressWarnings("unchecked")
	public ValidatedSearchData decorate(ValidatedSearchData validatedSearchData) throws Exception {

        Response accessControlServiceResponse = accessControlService.getUserAccess(validatedSearchData.getUuid().toString(), validatedSearchData.getRootOrg());
        Map<String, Object> result = accessControlServiceResponse.getResult();

        HashSet<String> orgs = new HashSet<>();
        List<UserAccessPathsModel> special = (List<UserAccessPathsModel>) result.get("special");
        special.forEach(item -> orgs.add(item.getPrimaryKey().getOrg()));
        List<Map<String, Object>> groups = (List<Map<String, Object>>) result.get("groups");
        groups.forEach(item -> orgs.add(String.valueOf(item.get("org"))));
        validatedSearchData.setOrgs(new ArrayList<>(orgs));

        return validatedSearchData;
    }

}
