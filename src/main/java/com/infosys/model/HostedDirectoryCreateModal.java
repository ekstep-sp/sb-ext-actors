/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HostedDirectoryCreateModal {

	Integer code;
	String message;
    String webHostPath;

	public Integer getCode() {
		return code;
	}

    public void setCode(Integer code) {
		this.code = code;
	}

    public String getMesssage() {
		return message;
	}

    public void setMesssage(String messsage) {
		this.message = messsage;
	}

    public String getWebPath() {
		return webHostPath;
	}

    public void setWebPath(String webHostPath) {
		this.webHostPath = webHostPath;
	}
	
	public String toString() {
		return "{code : " + this.code + ", message : " + this.message + ", webHostPath: " + this.webHostPath + "}";
	}
}
