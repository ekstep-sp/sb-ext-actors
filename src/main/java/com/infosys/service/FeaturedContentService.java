/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;
import java.util.Map;

public interface FeaturedContentService {
  
	public Map<String,Object> getFeaturedCourseContent() throws JsonParseException, JsonMappingException, IOException;
	
	public Map<String,Object> getFeaturedResourceContent() throws JsonParseException, JsonMappingException, IOException;
}
