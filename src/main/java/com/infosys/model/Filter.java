/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
@JsonIgnoreProperties(ignoreUnknown = true)
public class Filter {

	String contentType;
	String resourceType;
	Topic[] concepts;
	Track[] tracks;
	String query;

    public static Filter fromMap(Map<String, Object> map) {
        return new ObjectMapper().convertValue(map, Filter.class);
    }

    public static Filter fromJson(String json) throws IOException {
        return new ObjectMapper().readValue(json, Filter.class);
    }

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

    public String getContentType() {
		return contentType;
	}

    public void setContentType(String contentType) {
		this.contentType = contentType;
	}

    public String getResourceType() {
		return resourceType;
	}

    public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

    public Topic[] getConcepts() {
		return concepts;
	}

    public void setConcepts(Topic[] concepts) {
		this.concepts = concepts;
	}

    public Track[] getTracks() {
		return tracks;
	}

    public void setTrack(Track[] tracks) {
		this.tracks = tracks;
	}

	    public Map<String, Object> toMap() {
	        return new ObjectMapper().convertValue(this, Map.class);
	    }
	
}
