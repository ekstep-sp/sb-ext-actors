/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.helper;

import com.infosys.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class will handle the data cache.
 *
 * @author Amit Kumar
 */
public class DataCacheHandler implements Runnable {
    private static final String KEY_SPACE_NAME = "sunbird";
    /**
     * pageMap is the map of (orgId:pageName) and page Object (i.e map of string , object) sectionMap
     * is the map of section Id and section Object (i.e map of string , object)
     */
    private static Map<String, Map<String, Object>> pageMap = new ConcurrentHashMap<>();
    private static Map<String, Map<String, Object>> sectionMap = new ConcurrentHashMap<>();
    private static Map<String, Object> roleMap = new ConcurrentHashMap<>();
    private static Map<String, String> orgTypeMap = new ConcurrentHashMap<>();
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();

    /**
     * @return the pageMap
     */
    public static Map<String, Map<String, Object>> getPageMap() {
        return pageMap;
    }

    /**
     * @param pageMap the pageMap to set
     */
    public static void setPageMap(Map<String, Map<String, Object>> pageMap) {
        DataCacheHandler.pageMap = pageMap;
    }

    /**
     * @return the sectionMap
     */
    public static Map<String, Map<String, Object>> getSectionMap() {
        return sectionMap;
    }

    /**
     * @param sectionMap the sectionMap to set
     */
    public static void setSectionMap(Map<String, Map<String, Object>> sectionMap) {
        DataCacheHandler.sectionMap = sectionMap;
    }

    /**
     * @return the roleMap
     */
    public static Map<String, Object> getRoleMap() {
        return roleMap;
    }

    /**
     * @param roleMap the roleMap to set
     */
    public static void setRoleMap(Map<String, Object> roleMap) {
        DataCacheHandler.roleMap = roleMap;
    }

    /**
     * @return the orgTypeMap
     */
    public static Map<String, String> getOrgTypeMap() {
        return orgTypeMap;
    }

    /**
     * @param orgTypeMap the orgTypeMap to set
     */
    public static void setOrgTypeMap(Map<String, String> orgTypeMap) {
        DataCacheHandler.orgTypeMap = orgTypeMap;
    }

    @Override
    public void run() {
        ProjectLogger.log("Data cache started..");
        cache(pageMap, "page_management");
        cache(sectionMap, "page_section");
        roleCache(roleMap);
        orgTypeCache(orgTypeMap);
    }

    private void orgTypeCache(Map<String, String> orgTypeMap) {
        Response response =
                cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.ORG_TYPE_DB);
        List<Map<String, Object>> responseList =
                (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        if (null != responseList && !responseList.isEmpty()) {
            for (Map<String, Object> resultMap : responseList) {
                orgTypeMap.put(((String) resultMap.get(JsonKey.NAME)).toLowerCase(), (String) resultMap.get(JsonKey.ID));
            }
        }
    }

    private void roleCache(Map<String, Object> roleMap) {
        Response response =
                cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.ROLE_GROUP);
        List<Map<String, Object>> responseList =
                (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        if (null != responseList && !responseList.isEmpty()) {
            for (Map<String, Object> resultMap : responseList) {
                roleMap.put((String) resultMap.get(JsonKey.ID), resultMap.get(JsonKey.ID));
            }
        }
        Response response2 =
                cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.ROLE);
        List<Map<String, Object>> responseList2 =
                (List<Map<String, Object>>) response2.get(JsonKey.RESPONSE);
        if (null != responseList2 && !responseList2.isEmpty()) {
            for (Map<String, Object> resultMap2 : responseList2) {
                roleMap.put((String) resultMap2.get(JsonKey.ID), resultMap2.get(JsonKey.ID));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void cache(Map<String, Map<String, Object>> map, String tableName) {
        try {
            Response response =
                    cassandraOperation.getAllRecords(KEY_SPACE_NAME, tableName);
            List<Map<String, Object>> responseList =
                    (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
            if (null != responseList && !responseList.isEmpty()) {
                for (Map<String, Object> resultMap : responseList) {
                    if (tableName.equalsIgnoreCase(JsonKey.PAGE_SECTION)) {
                        map.put((String) resultMap.get(JsonKey.ID), resultMap);
                    } else {
                        String orgId = (((String) resultMap.get(JsonKey.ORGANISATION_ID)) == null ? "NA"
                                : (String) resultMap.get(JsonKey.ORGANISATION_ID));
                        map.put(orgId + ":" + ((String) resultMap.get(JsonKey.PAGE_NAME)), resultMap);
                    }
                }
            }
            ProjectLogger.log("pagemap size" + map.size());
            ProjectLogger.log("pagemap keyset " + map.keySet());
        } catch (Exception e) {
            ProjectLogger.log(e.getMessage(), e);
        }
    }

}

