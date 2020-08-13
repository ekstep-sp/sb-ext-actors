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
package com.infosys.searchv6;

import com.infosys.searchv6.decorators.OrgsDecoratorv6;
import com.infosys.searchv6.validations.model.MultiLingualIntegratedSearchValidatorV6;
import com.infosys.searchv6.validations.model.ValidatedSearchData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("AdminMultiLingualIntegratedSearchServicev6")
//@Validated
class AdminMultiLingualIntegratedSearchServicev6 extends MultiLingualIntegratedSearchServicev6 {

	@Autowired
	private MultiLingualIntegratedSearchValidatorV6 validator;

    @Autowired
    private OrgsDecoratorv6 orgsDecorator;

//    @Validated
    private Map<String,Object> callSuperSearch(ValidatedSearchData validatedSearchData) throws Exception {
    	validatedSearchData = validator.doValidations(validatedSearchData);
        return super.callSearchService(validatedSearchData);
    }

    final public Map<String,Object> performSearch(ValidatedSearchData validatedSearchData) throws Exception {

        validatedSearchData = orgsDecorator.decorate(validatedSearchData);
        return callSuperSearch(validatedSearchData);
    }

}
