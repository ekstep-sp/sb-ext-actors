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
package com.infosys.search;

import com.infosys.search.decorators.AccessPathsAndOrgsDecorator;
import com.infosys.search.validations.model.MultiLingualIntegratedSearchValidator;
import com.infosys.search.validations.model.ValidatedSearchData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sunbird.common.models.response.Response;

@Component("AuthoringToolMultiLingualIntegratedSearchService")
//@Validated
class AuthoringToolMultiLingualIntegratedSearchService extends MultiLingualIntegratedSearchService {

	@Autowired
	private MultiLingualIntegratedSearchValidator validator;
	
    @Autowired
    private AccessPathsAndOrgsDecorator accessPathsAndOrgsDecorator;

//    @Validated
    private Response callSuperSearch(ValidatedSearchData validatedSearchData) throws Exception {
    	validatedSearchData = validator.doAuthoringSearchValidations(validatedSearchData);
        return super.callSearchService(validatedSearchData);
    }

    final public Response performSearch(ValidatedSearchData validatedSearchData) throws Exception {

        validatedSearchData = accessPathsAndOrgsDecorator.decorate(validatedSearchData);
        return callSuperSearch(validatedSearchData);
    }

}
