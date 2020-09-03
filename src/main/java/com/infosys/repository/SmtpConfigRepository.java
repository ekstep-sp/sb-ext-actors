package com.infosys.repository;

import com.infosys.model.cassandra.SmtpConfig;
import com.infosys.model.cassandra.SmtpConfigPrimaryKeyModel;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmtpConfigRepository extends CassandraRepository<SmtpConfig, SmtpConfigPrimaryKeyModel> {

}
