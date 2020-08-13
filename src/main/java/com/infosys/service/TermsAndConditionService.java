/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface TermsAndConditionService {

    boolean addUserTermsAcceptance(String userId, String userType, List<Map<String, Object>> termsAccepted) throws IOException;

    Map<String, Object> addUserTermsAcceptanceV2(String userId, String userType, List<Map<String, Object>> termsAccepted) throws IOException;

    Map<String, Object> checkVersionChange(String userId, String userType);

    Map<String, Object> checkVersionChangeV2(String userId, String userType);
    
    void userPostProcessing(String rootOrg, String org, String userId, String userType);
}
