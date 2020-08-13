/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model.cassandra;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;


@Table("user_history")
public class UserHistoryModel {

    @PrimaryKey
    private UserHistoryPrimaryKeyModel primaryKey;

    @Column("last_accessed_on")
    private Date lastAccessedOn;

    public UserHistoryModel(UserHistoryPrimaryKeyModel primaryKey, Date lastAccessedOn) {
        super();
        this.primaryKey = primaryKey;
        this.lastAccessedOn = lastAccessedOn;
    }

    public UserHistoryModel() {
        super();
    }

    public UserHistoryPrimaryKeyModel getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(UserHistoryPrimaryKeyModel primaryKey) {
        this.primaryKey = primaryKey;
    }

    public Date getLastAccessedOn() {
        return lastAccessedOn;
    }

    public void setLastAccessedOn(Date lastAccessedOn) {
        this.lastAccessedOn = lastAccessedOn;
    }


}
