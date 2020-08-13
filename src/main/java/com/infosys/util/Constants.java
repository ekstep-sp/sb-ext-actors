/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.util;

import java.util.Arrays;
import java.util.List;

public class Constants {

	public static final Integer NEW_LEX_SEARCH_SLOP = 3;
	public static final String NEW_LEX_SEARCH_ANALYZER = "standard";
	public static final String DURATION_SHORT = "-inf-20";
	public static final String DURATION_MEDIUM = "20-60";
	public static final String DURATION_LONG = "60-180";
	public static final String DURATION_DETAILED = "180-inf";
	public static final Long DURATION_SHORT_MAX = 1200l;
	public static final Long DURATION_MEDIUM_MIN = 1200l;
	public static final Long DURATION_MEDIUM_MAX = 3600l;
	public static final Long DURATION_LONG_MIN = 3600l;
	public static final Long DURATION_LONG_MAX = 10800l;
	public static final Long DURATION_DETAILED_MIN = 10800l;
	public static final String QUERY = "query";
	public static final String FILTERS = "filters";
	public static final String IS_STAND_ALONE = "isStandAlone";
	public static final String FILTER_CONCEPTS_FIELD_KEY = "concepts";
	public static final String FILTER_CONCEPTS_IDENTIFIER_FIELD_KEY = "concepts.identifier";
	public static final String FILTER_CONCEPTS_NAME_FIELD_KEY = "concepts.name.raw";
	public static final String FILTER_SOURCE_SHORT_NAME_FIELD_KEY = "sourceShortName.raw";
	public static final String FILTER_RESOURCE_CATEGORY_FIELD_KEY = "resourceCategory";
	public static final String FILTER_CONTENT_TYPE_FIELD_KEY = "contentType";
	public static final String FILTER_FILE_TYPE_FIELD_KEY = "fileType";
	public static final String FILTER_DURATION_FIELD_KEY = "duration";
	public static final String FILTER_COMPLEXITY_LEVEL_FIELD_KEY = "complexityLevel";
	public static final String FILTER_TAGS_FIELD_KEY = "tags";
	public static final String FILTER_LAST_UPDATED_ON_FIELD_KEY = "lastUpdatedOn";
	public static final String FILTER_KEYWORDS_KEY = "keywords.raw";
	public static final String FILTER_COLLECTIONS_IDENTIFIER_FIELD_KEY = "collections.identifier";
	public static final String TAGS_AGGS_KEY = "tags_aggs";
	public static final List<String> TAGS_AGGS_FIELD_DISPLAYNAME_PAIR = Arrays.asList("tags", "Catalog");
	public static final String DURATION_AGGS_KEY = "duration_aggs";
	public static final List<String> DURATION_AGGS_FIELD_DISPLAYNAME_PAIR = Arrays.asList("duration", "Duration");
	public static final String SOURCE_SHORT_NAME_AGGS_KEY = "sourceShortName_aggs";
	public static final List<String> SOURCE_SHORT_NAME_AGGS_FIELD_DISPLAYNAME_PAIR = Arrays.asList("sourceShortName",
			"Source");
	public static final String COMPLEXITY_LEVEL_AGGS_KEY = "complexityLevel_aggs";
	public static final List<String> COMPLEXITY_LEVEL_AGGS_FIELD_DISPLAYNAME_PAIR = Arrays.asList("complexityLevel",
			"Level");
	public static final String FILE_TYPE_AGGS_KEY = "fileType_aggs";
	public static final List<String> FILE_TYPE_AGGS_FIELD_DISPLAYNAME_PAIR = Arrays.asList("fileType", "Type");
	public static final String CONTENT_TYPE_AGGS_KEY = "contentType_aggs";
	public static final List<String> CONTENT_TYPE_AGGS_FIELD_DISPLAYNAME_PAIR = Arrays.asList("contentType",
			"Content Type");
	public static final String KEYWORDS_AGGS_KEY = "keywords_aggs";
	public static final List<String> KEYWORDS_AGGS_FIELD_DISPLAYNAME_PAIR = Arrays.asList("keywords", "Keywords");
	public static final String CONCEPTS_AGGS_KEY = "concepts_aggs";
	public static final List<String> CONCEPTS_AGGS_FIELD_DISPLAYNAME_PAIR = Arrays.asList("concepts", "Concepts");
	public static final String LAST_UPDATED_ON_AGGS_KEY = "concepts_aggs";
	public static final List<String> LAST_UPDATED_ON_AGGS_FIELD_DISPLAYNAME_PAIR = Arrays.asList("lastUpdatedOn",
			"Published Date");
	public static final String DURATION_SHORT_DISPLAY_NAME = "Less than 20 mins";
	public static final String DURATION_MEDIUM_DISPLAY_NAME = "20 to 60 mins";
	public static final String DURATION_LONG_DISPLAY_NAME = "60 to 180 mins";
	public static final String DURATION_DETAILED_DISPLAY_NAME = "More than 180 mins";
	public static final String FILTER_RESOURCE_TYPE_FIELD_KEY = "resourceType";
	public static final String RESOURCE_TYPE_AGGS_KEY = "resourceType_aggs";
	public static final List<String> RESOURCE_TYPE_AGGS_FIELD_DISPLAYNAME_PAIR = Arrays.asList("resourceType",
			"Resource Type");
	public static final String RESOURCE_CATEGORY_AGGS_KEY = "resourceCategory_aggs";
	public static final List<String> RESOURCE_CATEGORY_AGGS_FIELD_DISPLAYNAME_PAIR = Arrays.asList("resourceCategory",
			"Category");
	public static final String CATALOG = "catalog";
	
	public static final String INFOSYS_ROOTORG = "infosys";

	public Constants() {
	}

}