/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.controller;

import com.infosys.cassandra.CassandraOperation;
import com.infosys.helper.ServiceFactory;
import com.infosys.util.LexProjectUtil;
import com.infosys.util.Util;
import com.infosys.util.Util.DbInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.datasecurity.EncryptionService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


@RestController
public class BatchListController {

    private EncryptionService encryptionService = org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getEncryptionServiceInstance(null);
    //private CassandraOperation cassandraOperation = new CassandraOperationImpl("", "9042", "", "","sunbird");
    private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private PropertiesCache properties = PropertiesCache.getInstance();
    private String keyspace = properties.getProperty(JsonKey.DB_KEYSPACE);

    public BatchListController() {
        Util.checkCassandraDbConnections(keyspace);
    }


    @GetMapping("/v1/getBatchIDList/{courseId}")
    public List<Map<String, Object>> getAllBatchIds(@PathVariable("courseId") String courseId) {
    	Long start = System.currentTimeMillis();
    	String url = "/v1/getBatchIDList/" + courseId;
    	DbInfo courseBatchDbInfo = Util.dbInfoMap.get(JsonKey.COURSE_BATCH_DB);
        Response response = cassandraOperation.getAllRecords(courseBatchDbInfo.getKeySpace(), courseBatchDbInfo.getTableName());
        Map<String, Object> responseMap = response.getResult();
        List<Map<String, Object>> list = (List<Map<String, Object>>) responseMap.get("response");
        List<Map<String, Object>> courseBatches = new ArrayList<Map<String, Object>>();
        
        for (Map<String, Object> batch : list) {
            if (batch.get("courseId").equals(courseId))
                courseBatches.add(batch);
        }
        //ProjectLogger.log(url +":" + LexProjectUtil.getFormattedTime(System.currentTimeMillis() - start),LoggerEnum.INFO);
        return courseBatches;
    }


}

