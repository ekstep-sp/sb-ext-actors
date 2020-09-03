package com.infosys.service;

import com.infosys.model.cassandra.SmtpConfig;
import org.springframework.stereotype.Service;

public interface SmtpConfigService {
    SmtpConfig getSmtpConfig(String rootOrg, String org);
}
