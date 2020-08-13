/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;

public class HeaderMapRequestWrapper extends HttpServletRequestWrapper{
    private Map<String, String> headerMap = new HashMap<String, String>();

	 public HeaderMapRequestWrapper(HttpServletRequest request) {
	        super(request);
	    }

	    public void addHeader(String name, String value) {
	        headerMap.put(name, value);
	    }

	   
	    public String getHeader(String name) {
	        String headerValue = super.getHeader(name);
	        if (headerMap.containsKey(name)) {
	            headerValue = headerMap.get(name);
	        }
	        return headerValue;
	    }

	    public Enumeration<String> getHeaderNames() {
	        List<String> names = Collections.list(super.getHeaderNames());
	        for (String name : headerMap.keySet()) {
	            names.add(name);
	        }
	        return Collections.enumeration(names);
	    }

	  
	    public Enumeration<String> getHeaders(String name) {
	        List<String> values = Collections.list(super.getHeaders(name));
	        if (headerMap.containsKey(name)) {
	            values.add(headerMap.get(name));
	        }
	        return Collections.enumeration(values);
	    }
}
