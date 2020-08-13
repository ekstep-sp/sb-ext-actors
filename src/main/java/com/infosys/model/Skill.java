/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Skill {

    String identifier;
    String category;
    // String level1;
    //String level2;
    // String level3;
    String skill;
    String name;
    //Integer taxonomyId;
    //String[] tags;


    /*public Integer getTaxonomyId() {
        return taxonomyId;
    }
    public void setTaxonomyId(Integer taxonomyId) {
        this.taxonomyId = taxonomyId;
    }*/
/*public String[] getTags() {
    return tags;
}
public void setTags(String[] tags) {
	this.tags = tags;
}*/
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /*ublic String getLevel1() {
        return level1;
    }
    public void setLevel1(String level1) {
        this.level1 = level1;
    }
    public String getLevel2() {
        return level2;
    }
    public void setLevel2(String level2) {
        this.level2 = level2;
    }
    public String getLevel3() {
        return level3;
    }
    public void setLevel3(String level3) {
        this.level3 = level3;
    }*/
    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
