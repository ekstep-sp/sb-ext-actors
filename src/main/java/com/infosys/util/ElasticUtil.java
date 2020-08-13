/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.util;

import com.infosys.exception.ApplicationLogicError;
import com.infosys.exception.ElasticSearchException;
import com.infosys.exception.PropertiesNotFoundException;
import org.sunbird.common.models.util.JsonKey;

public class ElasticUtil {

	public static String getEndPointForES(String indexName, String typeName, String propertiesFile) {

		if (indexName == null || typeName == null || propertiesFile == null) {
			throw new ApplicationLogicError("IndexName or TypeName or propertiesFile name is missing");
		}

		try {
            String ips = PropertiesUtil.getIPAndPort(JsonKey.SUNBIRD_ES_IP, LexJsonKey.SUNBIRD_ES_REST_PORT, propertiesFile)[0];
            String port = PropertiesUtil.getIPAndPort(JsonKey.SUNBIRD_ES_IP, LexJsonKey.SUNBIRD_ES_REST_PORT,
                    propertiesFile)[1];
            return LexJsonKey.HTTP_PROTOCOL + "://" + ips + ":" + port + "/" + indexName + "/" + typeName + "/";
        } catch (PropertiesNotFoundException pnfe) {
            pnfe.printStackTrace();
			throw new ElasticSearchException("elastic search update failed");
		}

	}

}
