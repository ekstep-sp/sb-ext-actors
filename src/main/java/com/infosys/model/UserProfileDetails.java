/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileDetails {

	String department = "";
	String jobTitle = "";
	String givenName = "";
	String surname = "";
	String imageUrl = "";
	String usageLocation = "";
	String onPremisesUserPrincipalName="";
	String mobilePhone="";
	String companyName = "";

    public static UserProfileDetails fromMap(Map<String, Object> map) {
        return new ObjectMapper().convertValue(map, UserProfileDetails.class);
    }

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

    public String getImageUrl() {
		return imageUrl;
	}

    public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

    public String getUsageLocation() {
		return usageLocation;
	}

    public void setUsageLocation(String usageLocation) {
		this.usageLocation = usageLocation;
	}

    public String getOnPremisesUserPrincipalName() {
		return onPremisesUserPrincipalName;
	}

    public void setOnPremisesUserPrincipalName(String onPremisesUserPrincipalName) {
		this.onPremisesUserPrincipalName = onPremisesUserPrincipalName;
	}

    public String getMobilePhone() {
		return mobilePhone;
	}

    public void setMobilePhone(String mobilePhone) {
		this.mobilePhone = mobilePhone;
	}

    public String getDepartment() {
		return department;
	}

    public void setDepartment(String department) {
		this.department = department;
	}

    public String getJobTitle() {
		return jobTitle;
	}

    public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}

    public String getGivenName() {
		return givenName;
	}

    public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

    public String getSurname() {
		return surname;
	}

    public void setSurname(String surname) {
		this.surname = surname;
	}

    public Map<String, Object> toMap() {
	        ObjectMapper mapper = new ObjectMapper();
	        return mapper.convertValue(this, Map.class);
	    }
}
