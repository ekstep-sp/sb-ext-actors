/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.util;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

public class ErrorGenerator {
	public static Response generateErrorResponse(ResponseCode respCode, String id, String ver, String msg) {
		Response response = new Response();
		response.setId(id);
		response.setVer(ver);
		response.setTs(ProjectUtil.getFormattedDate());
		ResponseCode responseCode = ResponseCode.getResponse(respCode.getErrorCode());
		ResponseParams params = new ResponseParams();
		params.setErr(msg);
		response.setParams(params);
		response.setResponseCode(responseCode);
		return response;
	}
}
