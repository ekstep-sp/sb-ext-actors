/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.sunbird.common.models.util.ProjectLogger;

@Component
public class CachingService {

    @Autowired
    CachingServiceManager cachingServiceManager;

    @Scheduled(fixedRate = 86400000)
    public void clearCache() {
        ProjectLogger.log("Evicting cache");
        cachingServiceManager.evictAllCacheValues(LexConstants.DOMAIL_CACHE);
    }
}
