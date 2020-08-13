/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import java.io.IOException;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.Request;

public interface SearchTemplateService {
    public Response generalSearchService(Request request, boolean accessControlEnabled, String rootOrg) throws IOException;

	public Response adminSearchService(Request request, boolean accessControlEnabled, String rootOrg) throws IOException;

	Response authoringSearchService(Request request, boolean accessControlEnabled, String rootOrg) throws IOException;
}
