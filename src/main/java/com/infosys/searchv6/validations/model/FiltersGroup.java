/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
/**
 * © 2017 - 2019 Infosys Limited, Bangalore, India. All Rights Reserved.
 * Version: 1.10
 * <p>
 * Except for any free or open source software components embedded in this Infosys proprietary software program (“Program”),
 * this Program is protected by copyright laws, international treaties and other pending or existing intellectual property rights in India,
 * the United States and other countries. Except as expressly permitted, any unauthorized reproduction, storage, transmission in any form or
 * by any means (including without limitation electronic, mechanical, printing, photocopying, recording or otherwise), or any distribution of
 * this Program, or any portion of it, may result in severe civil and criminal penalties, and will be prosecuted to the maximum extent possible
 * under the law.
 * <p>
 * Highly Confidential
 */
package com.infosys.searchv6.validations.model;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FiltersGroup implements Serializable {

    @Valid
    private List<AndFilters> andFilters = new ArrayList<>();
    @Valid
    private List<NotFilters> notFilters = new ArrayList<>();

    public FiltersGroup(List<AndFilters> andFilters, List<NotFilters> notFilters) {
        this.andFilters = andFilters;
        this.notFilters = notFilters;
    }

    public FiltersGroup() {
    }

    public List<AndFilters> getAndFilters() {
        return andFilters;
    }

    public void setAndFilters(List<AndFilters> andFilters) {
        this.andFilters = andFilters;
    }

    public List<NotFilters> getNotFilters() {
        return notFilters;
    }

    public void setNotFilters(List<NotFilters> notFilters) {
        this.notFilters = notFilters;
    }
}
