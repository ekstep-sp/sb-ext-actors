/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import com.infosys.elastic.common.ElasticSearchUtil;
import com.infosys.model.ContentMeta;
import com.infosys.model.ObjectMeta;
import com.infosys.service.ParentService;
import com.infosys.util.LexProjectUtil;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ParentsServiceImpl implements ParentService {

    private List<ContentMeta> parentHierarchy(String resourceId, boolean self) {
        List<ContentMeta> ret = new ArrayList<>();
        try {
            Map<String, Object> searchData = new HashMap<>();
            searchData.put(JsonKey.IDENTIFIER, resourceId);
            searchData.put(JsonKey.STATUS, LexProjectUtil.Status.LIVE.getValue());
            List<Map<String, Object>> content = ElasticSearchUtil.searchMatchedData(
                    LexProjectUtil.EsIndex.bodhi.getIndexName(),
                    LexProjectUtil.EsType.resource.getTypeName(), searchData, null, 1);
            if (content == null || content.isEmpty()) {
                return ret;
            }
            ContentMeta resource = ContentMeta.fromMap(content.get(0));
            // System.out.println("ResourceFound: " + resource.getIdentifier() + " " + resource.getContentType());
            ObjectMeta[] parents = resource.getCollections();
            if (parents == null || parents.length == 0) {
                // System.out.println("ParentNotFound");
                if (!self) {
                    // System.out.println("Adding Resource");
                    ret.add(resource);
                }
                // System.out.println("Returning " + ret);
                return ret;
            } else {
                if (!self && resource.getContentType().equals(LexProjectUtil.ContentType.course.get())) {
                    // System.out.println("Adding Course");
                    ret.add(resource);
                }
                // System.out.println("ParentFound");
                for (ObjectMeta parent : parents) {
                    ret.addAll(parentHierarchy(parent.getIdentifier(), false));
                }
                // System.out.println("Returning " + ret);
                return ret;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            ProjectLogger.log("Exception occured when converting map to ContentMeta", ex);
            return ret;
        }
    }

    private List<ContentMeta> courseHierarchy(String resourceId, boolean self) {
        List<ContentMeta> ret = new ArrayList<>();
        try {
            Map<String, Object> searchData = new HashMap<>();
            searchData.put(JsonKey.IDENTIFIER, resourceId);
            searchData.put(JsonKey.STATUS, LexProjectUtil.Status.LIVE.getValue());
            List<Map<String, Object>> content = ElasticSearchUtil.searchMatchedData(
                    LexProjectUtil.EsIndex.bodhi.getIndexName(),
                    LexProjectUtil.EsType.resource.getTypeName(), searchData, null, 1);
            if (content.isEmpty()) {
                return ret;
            }
            ContentMeta resource = ContentMeta.fromMap(content.get(0));

            ObjectMeta[] parents = resource.getCollections();
            if (parents != null && parents.length > 0) {
                if (!resource.getContentType().equals(
                        LexProjectUtil.ContentType.course.get())) {
                    for (ObjectMeta parent : parents) {
                        List<ContentMeta> returnedParents =
                                courseHierarchy(parent.getIdentifier(), false);
                        ret.addAll(returnedParents);
                    }
                }
            }
            if (!self) {
                ret.add(resource);
            }
            return ret;
        } catch (Exception ex) {
            ex.printStackTrace();
            ProjectLogger.log("Exception occured when converting map to ContentMeta", ex);
            return ret;
        }

    }

    @Override
    public Map<String, Object> getAllParents(String resourceId) {
        Map<String, Object> parents = new HashMap<>();
        List<ContentMeta> learningPaths = new ArrayList<>();
        List<ContentMeta> courses = new ArrayList<>();
        List<ContentMeta> modules = new ArrayList<>();

        // System.out.println("Parent hierachy request for resource " + resourseId);
        List<ContentMeta> ret = parentHierarchy(resourceId, true);
        for (ContentMeta data : ret) {
            if (data != null && data.getIdentifier() != null && data.getContentType() != null) {
                if (data.getContentType().equals(LexProjectUtil.ContentType.collection.get())) {
                    boolean notFound = true;
                    for (ContentMeta x : modules) {
                        if (data.getIdentifier().equals(x.getIdentifier())) {
                            notFound = false;
                            break;
                        }
                    }
                    if (notFound)
                        modules.add(data);
                } else if (data.getContentType().equals(LexProjectUtil.ContentType.course.get())) {
                    boolean notFound = true;
                    for (ContentMeta x : courses) {
                        if (data.getIdentifier().equals(x.getIdentifier())) {
                            notFound = false;
                            break;
                        }
                    }
                    if (notFound)
                        courses.add(data);
                } else if (data.getContentType().equals(LexProjectUtil.ContentType.learningPath.get())) {
                    boolean notFound = true;
                    for (ContentMeta x : learningPaths) {
                        if (data.getIdentifier().equals(x.getIdentifier())) {
                            notFound = false;
                            break;
                        }
                    }
                    if (notFound)
                        learningPaths.add(data);
                }
            }
        }
        parents.put("learningPaths", learningPaths);
        parents.put("courses", courses);
        parents.put("modules", modules);

        return parents;
    }

    @Override
    public Map<String, Object> getCourseParents(String resourceId) {
        Map<String, Object> parents = new HashMap<>();

        List<ContentMeta> courses = new ArrayList<>();
        List<ContentMeta> modules = new ArrayList<>();

        // System.out.println("Parent hierachy request for resource " + resourseId);
        List<ContentMeta> ret = courseHierarchy(resourceId, true);
        for (ContentMeta data : ret) {
            if (data != null && data.getIdentifier() != null && data.getContentType() != null) {
                if (data.getContentType().equals(LexProjectUtil.ContentType.collection.get())) {
                    boolean notFound = true;
                    for (ContentMeta x : modules) {
                        if (data.getIdentifier().equals(x.getIdentifier())) {
                            notFound = false;
                            break;
                        }
                    }
                    if (notFound)
                        modules.add(data);
                } else if (data.getContentType().equals(LexProjectUtil.ContentType.course.get())) {
                    boolean notFound = true;
                    for (ContentMeta x : courses) {
                        if (data.getIdentifier().equals(x.getIdentifier())) {
                            notFound = false;
                            break;
                        }
                    }
                    if (notFound)
                        courses.add(data);
                }
            }
        }

        parents.put("courses", courses);
        parents.put("modules", modules);

        return parents;
    }
}
