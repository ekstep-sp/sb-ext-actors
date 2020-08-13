/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Topic {
    private String id;
    private String date_created;
    private String date_modified;
    private String name;
    private String source;
    private int source_id;
    private String source_source;
    private String source_status;
    private String status;

    public Topic() {

    }

    public Topic(String id, String date_created, String date_modified, String name, String source,
                 int source_id, String source_source, String source_status, String status) {
        this.id = id;
        this.date_created = date_created;
        this.date_modified = date_modified;
        this.name = name;
        this.source = source;
        this.source_id = source_id;
        this.source_source = source_source;
        this.source_status = source_status;
        this.status = status;
    }

    public static Topic fromMap(Map<String, Object> map) {
        return new ObjectMapper().convertValue(map, Topic.class);
    }

    public Map<String, Object> toMap() {
        return new ObjectMapper().convertValue(this, Map.class);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDate_created() {
        return date_created;
    }

    public void setDate_created(String date_created) {
        this.date_created = date_created;
    }

    public String getDate_modified() {
        return date_modified;
    }

    public void setDate_modified(String date_modified) {
        this.date_modified = date_modified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getSource_id() {
        return source_id;
    }

    public void setSource_id(int source_id) {
        this.source_id = source_id;
    }

    public String getSource_source() {
        return source_source;
    }

    public void setSource_source(String source_source) {
        this.source_source = source_source;
    }

    public String getSource_status() {
        return source_status;
    }

    public void setSource_status(String source_status) {
        this.source_status = source_status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
