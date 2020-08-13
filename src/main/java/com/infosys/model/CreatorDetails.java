/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatorDetails extends Details {

    private String about;

    public CreatorDetails() {

    }

    public CreatorDetails(String id, String name, String email, String about) {
        super(id, name, email);
        this.about = about;
    }

    public Details toDetails() {
        return new Details(this.getId(), this.getName(), this.getEmail());
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }
}
