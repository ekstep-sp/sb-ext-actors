/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/*package com.infosys.controller;

import java.util.List;
import java.util.Map;

import com.infosys.cassandra.CassandraOperation;
import com.infosys.cassandraimpl.CassandraOperationImpl;
import com.infosys.elastic.ElasticSearchUtil;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;import com.infosys.util.LexJsonKey;
import org.sunbird.common.models.util.PropertiesCache;
import com.infosys.helper.ServiceFactory;

public class cassandraToElastic {
    public static void main(String args[])
    {
    	CassandraOperation cassandraOperation = new CassandraOperationImpl("","9042","","","bodhi");
        String keyspace = "bodhi";
        Response response = cassandraOperation.getAllRecords(keyspace, "skills");
        List<Map<String,Object>> resultMap = (List<Map<String, Object>>) response.get("response");
        //System.out.println(resultMap.toString());
        for(Map<String,Object>result: resultMap)
        {
        	System.out.println(result.toString());
        	ElasticSearchUtil.createData("lexskillsindex", "skills", result.get("taxonomy_id").toString(), result);
        }
        
        //ElasticSearchUtil.bulkInsertData("lexskillsindex", "skills", resultMap);
        System.out.println("finished");
        	
    }
}
*/