package com.infosys.model.cassandra;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;

@Data
@Table("smtp_config")
@AllArgsConstructor
@NoArgsConstructor
public class SmtpConfig {

	@PrimaryKey
	private SmtpConfigPrimaryKeyModel smtpConfigPrimaryKeyModel;

	@Column("host")
	String host;

	@Column("user_name")
	String userName;

	@Column("password")
	String password;

	@Column("sign_email")
	boolean signEmail;

	@Column("port")
	int port;

	@Column("last_updated_on")
	Date lastUpdatedOn;

	@Column("last_updated_by")
	String lastUpdatedBy;

	@Column("sender_id")
	String senderId;

	@Column("chunk_size")
	int chunkSize;

}
