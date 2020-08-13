/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import com.infosys.model.ContentMeta;
import com.infosys.util.LexProjectUtil;
import org.springframework.core.io.ClassPathResource;
import org.sunbird.common.models.util.JsonKey;
import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.elastic.dto.SearchDTO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class TestElasticSearchClass {

    private static final String STARTS_WITH = "startsWith";
    private static final String ENDS_WITH = "endsWith";

    public static void main(String args[]) {
        testFile();
    }

    public static void complexSearch() throws IOException {
        /*
         * Map<String,Object> mappedObject;// = new HashMap<String,Object>();
		 *
		 * mappedObject =
		 * ElasticSearchUtil.getDataByIdentifier(LexProjectUtil.EsIndex.bodhi.getIndexName(
		 * ), LexProjectUtil.EsType.resource.getTypeName(),"do_2123958259606241281182");
		 *
		 * ContentMeta contentMeta = ContentMeta.fromMap(mappedObject);
		 *
		 * System.out.println(contentMeta.getDescription());
		 */

        SearchDTO searchDTO = new SearchDTO();

        Map<String, String> map = new HashMap<String, String>();
        map.put("versionDate", "DES");

        searchDTO.setSortBy(map);

        Map<String, Object> mappedObjects = ElasticSearchUtil.complexSearch(searchDTO,
                LexProjectUtil.EsIndex.bodhi.getIndexName(), LexProjectUtil.EsType.resource.getTypeName());

        Set<String> keySet = mappedObjects.keySet();

        List<String> identifierList = new ArrayList<String>();
        List<ContentMeta> contentMetaList = new ArrayList<ContentMeta>();

        for (String key : keySet) {
            if (key.equals("content")) {
                List<Map<String, Object>> listOfEntries = (List<Map<String, Object>>) mappedObjects.get(key);
                for (Map object : listOfEntries) {
                    object.remove("keywords");
                    ContentMeta contentMeta = ContentMeta.fromMap(object);
                    Map<String, Object> contentMetaMap = getCourseMeta(map.get("identifier"));
                    contentMeta = ContentMeta.fromMap(contentMetaMap);
                    contentMetaList.add(contentMeta);
                }
            }
        }


    }

    public static Map<String, Object> getCourseMeta(String courseId) throws IOException {

        Map<String, Object> mappedObject = ElasticSearchUtil.getDataByIdentifier(LexProjectUtil.EsIndex.bodhi.getIndexName(), LexProjectUtil.EsType.resource.getTypeName(), courseId);
        return mappedObject;

    }

    public static void testFile() {
        String aggregator = "concepts.name";
        String fileName = "newTopics.txt";
        List<Map<String, Object>> resp = new ArrayList<>();
        try {
            File file = new ClassPathResource(fileName).getFile();
            //Read File Content
            List<String> content = Files.readAllLines(file.toPath());
            for (String line : content) {
                Map<String, Object> topic = new HashMap<String, Object>() {
                    {
                        put(aggregator, line.trim());
                        put(JsonKey.COUNT, 0);
                        put(JsonKey.ID, "");
                    }
                };
                resp.add(topic);
            }
            System.out.println(resp);
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public static void testData() throws IOException {

        String index = LexProjectUtil.EsIndex.lexTopic.getIndexName();
        String type = LexProjectUtil.EsType.content.getTypeName();
        String key = "name";
        String aggregator = "concepts.name";
        String[] values = {"Quality assurance", "Time complexity", "Document comparison"};

        Map<String, Object> m1 = new HashMap<>();

        List<Map<String, Object>> resp = ElasticSearchUtil.searchMatchedData(LexProjectUtil.EsIndex.bodhi.getIndexName(),
                LexProjectUtil.EsType.resource.getTypeName(), m1, aggregator);

        System.out.println(resp);
        System.out.println(resp.size());

        List<String> topics = new ArrayList<>();
        for (Map<String, Object> data : resp) {
            topics.add((String) data.get(aggregator));
        }

        List<Map<String, Object>> resp1 = ElasticSearchUtil.searchDataByValues(index, type, key, topics, topics.size());

        for (Map<String, Object> topic : resp) {
            for (Map<String, Object> topic1 : resp1) {
                if (((String) topic.get(aggregator)).equals((String) topic1.get(key))) {
                    topic.put(JsonKey.ID, topic1.get(JsonKey.ID));
                }
            }
        }

        System.out.println(resp);
    }
}
