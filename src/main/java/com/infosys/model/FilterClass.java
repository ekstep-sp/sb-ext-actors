/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilterClass {
    String[] keywords;
    String[] concepts;
    String[] categories;
    String[] tracks;
    String[] subTracks;
    String[] subSubTracks;
    String[] subSubSubTracks;
	String[] contentType;
	String[] resourceType;
    String[] resourceCategory;
    String[] sourceName;
    String[] sourceShortName;
    String[] complexityLevel;
    String[] audience;
    String[] unit;
	String[] duration;
    String[] mimeType;
    String[] fileType;
    String[] language;
    String[] tags;
    String[] collections;
    String[] status;
    String createdBy;
    String[] lastUpdatedOn;
    

	public String[] getComplexityLevel() {
		return complexityLevel;
	}

	public void setComplexityLevel(String[] complexityLevel) {
		this.complexityLevel = complexityLevel;
	}

	public String[] getDuration() {
		return duration;
	}

	public void setDuration(String[] duration) {
		this.duration = duration;
	}

	

	public String[] getAudience() {
		return audience;
	}

	public void setAudience(String[] audience) {
		this.audience = audience;
	}

	public String[] getUnit() {
		return unit;
	}

	public void setUnit(String[] unit) {
		this.unit = unit;
	}

	public String[] getKeywords() {
		return keywords;
	}

	public void setKeywords(String[] keywords) {
		this.keywords = keywords;
	}

	public String[] getLastUpdatedOn() {
		return lastUpdatedOn;
	}

	public void setLastUpdatedOn(String[] lastUpdatedOn) {
		this.lastUpdatedOn = lastUpdatedOn;
	}

	public static FilterClass fromMap(Map<String, Object> map) {
        return new ObjectMapper().convertValue(map, FilterClass.class);
    }

    public static FilterClass fromJson(String json) throws IOException {
        return new ObjectMapper().readValue(json, FilterClass.class);
    }

    public String[] getSourceName() {
		return sourceName;
	}

	public String[] getSourceShortName() {
		return sourceShortName;
	}

	public void setSourceShortName(String[] sourceShortName) {
		this.sourceShortName = sourceShortName;
	}

	public void setSourceName(String[] sourceName) {
		this.sourceName = sourceName;
	}

	public String[] getMimeType() {
		return mimeType;
	}

	public void setMimeType(String[] mimeType) {
		this.mimeType = mimeType;
	}

	public String[] getResourceCategory() {
		return resourceCategory;
	}

	public void setResourceCategory(String[] resourceCategory) {
		this.resourceCategory = resourceCategory;
	}

    public String[] getStatus() {
		return status;
	}

	public String[] getFileType() {
		return fileType;
	}

	public void setFileType(String[] fileType) {
		this.fileType = fileType;
	}

	public void setStatus(String[] status) {
		this.status = status;
	}

    public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

    public String[] getSubSubTracks() {
        return subSubTracks;
    }

    public void setSubSubTracks(String[] subSubTracks) {
        this.subSubTracks = subSubTracks;
    }

    public String[] getSubSubSubTracks() {
        return subSubSubTracks;
    }

    public void setSubSubSubTracks(String[] subSubSubTracks) {
        this.subSubSubTracks = subSubSubTracks;
    }

    public String[] getCategories() {
        return categories;
    }

    public void setCategories(String[] categories) {
        this.categories = categories;
    }

    public String[] getSubTracks() {
        return subTracks;
    }

    public void setSubTracks(String[] subTracks) {
        this.subTracks = subTracks;
    }

	public String[] getContentType() {
		return contentType;
	}

	public void setContentType(String[] contentType) {
		this.contentType = contentType;
	}

	public String[] getResourceType() {
		return resourceType;
	}

	public void setResourceType(String[] resourceType) {
		this.resourceType = resourceType;
	}

	public String[] getConcepts() {
		return concepts;
	}

	public void setConcepts(String[] concepts) {
		this.concepts = concepts;
	}

	public String[] getTracks() {
		return tracks;
	}

	public void setTracks(String[] tracks) {
		this.tracks = tracks;
	}

	    

	

		public String[] getLanguage() {
		return language;
	}

	public void setLanguage(String[] language) {
		this.language = language;
	}

	public String[] getTags() {
		return tags;
	}

	public void setTags(String[] tags) {
		this.tags = tags;
	}

	public String[] getCollections() {
		return collections;
	}

	public void setCollections(String[] collections) {
		this.collections = collections;
	}

		public Map<String, Object> toMap() {
	        return new ObjectMapper().convertValue(this, Map.class);
	    }
	    
	    public boolean checkNull() throws IllegalAccessException {
		    for (Field f : getClass().getDeclaredFields())
		        if (f.get(this) != null)
		            return false;
		    return true;            
		}


}
