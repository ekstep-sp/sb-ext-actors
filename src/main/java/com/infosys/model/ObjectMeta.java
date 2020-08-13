/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectMeta {

    private String identifier;
    private String name;
    private String objectType;
    private String relation;
    private String description;
    private String index;
    private String status;
    private String depth;
    private String mimeType;
    private String visibility;
    private String compatibilityLevel;

    public static ObjectMeta fromContentMeta(ContentMeta content, String index, String visibility) {
        ObjectMeta obj = new ObjectMeta();
        obj.setIdentifier(content.getIdentifier());
        obj.setName(content.getName());
        obj.setObjectType(content.getContentType());
        obj.setRelation("hasSequenceMember");
        obj.setDescription(content.getDescription());
        obj.setIndex(index);
        obj.setStatus(content.getStatus());
        obj.setDepth(null);
        obj.setMimeType(content.getMimeType());
        obj.setVisibility(visibility);
        obj.setCompatibilityLevel(null);
        return obj;
    }

    public static ObjectMeta fromMap(Map<String, Object> map) {
        return new ObjectMapper().convertValue(map, ObjectMeta.class);
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDepth() {
        return depth;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getCompatibilityLevel() {
        return compatibilityLevel;
    }

    public void setCompatibilityLevel(String compatibilityLevel) {
        this.compatibilityLevel = compatibilityLevel;
    }
    
    public Map<String, Object> toMap(boolean keepNulls) {
        ObjectMapper mapper = new ObjectMapper();
        if (!keepNulls) {
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
        return mapper.convertValue(this, Map.class);
    }
}
