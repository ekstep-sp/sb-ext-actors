/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseProgress {
    public String identifier;
    public String name;
    
    public CourseProgress() {
    	
    }

    public CourseProgress(String identifier, String name) {
		super();
		this.identifier = identifier;
		this.name = name;
	}


	@Override
    public String toString() {
        return "CourseProgress [identifier=" + identifier + ", name=" + name + "]";
    }
}
