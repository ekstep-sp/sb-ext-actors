package com.infosys.serviceImpl;

import com.infosys.exception.ApplicationLogicError;
import com.infosys.model.cassandra.SmtpConfig;
import com.infosys.model.cassandra.SmtpConfigPrimaryKeyModel;
import com.infosys.repository.SmtpConfigRepository;
import com.infosys.service.EncryptionService;
import com.infosys.service.SmtpConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SmtpConfigServiceImpl implements SmtpConfigService {
    @Autowired
    SmtpConfigRepository smtpRepo;

    @Autowired
    EncryptionService encryptionService;

    @Autowired
    Environment env;

    public SmtpConfig getSmtpConfig(String rootOrg, String org) {
        if (rootOrg == null || rootOrg.isEmpty())
            throw new ApplicationLogicError("root org is not present");
        Optional<SmtpConfig> optionalSmtpConfig;
        optionalSmtpConfig = smtpRepo.findById(new SmtpConfigPrimaryKeyModel(rootOrg, org));
        SmtpConfig validConfig = optionalSmtpConfig.orElse(null);

        if (validConfig == null)
            throw new ApplicationLogicError("smtp config not found for " + rootOrg);
        // de-crypt stored password in db
        if (!validConfig.getPassword().isEmpty())
            validConfig.setPassword(
                    encryptionService.decrypt(validConfig.getPassword(), env.getProperty(rootOrg + ".smtpKey")));
        return validConfig;

    }
}
