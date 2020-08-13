/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.serviceImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.infosys.repository.BatchExecutionRepository;
import com.infosys.service.HealthService;
import com.infosys.service.UserUtilityService;
import com.infosys.util.LexJsonKey;
import com.infosys.util.Util;

@Service
public class HealthServiceImpl implements HealthService {

	@Autowired
	BatchExecutionRepository batchExecutionRepo;

	@Autowired
	UserUtilityService userUtilService;

	@Value("${content.service.host}")
	private String contentServiceHost;

	@Value("${bodhi_content_port}")
	private String contentPort;

	private PropertiesCache properties = PropertiesCache.getInstance();
	private String bodhiKeyspace = LexJsonKey.BODHI_DB_KEYSPACE;
	private String badgeMeta = properties.getProperty(LexJsonKey.AllBadges);
	private CassandraConnectionManager connectionManager;
	private PreparedStatement getBadgeMeta = null;

	public HealthServiceImpl() {
		Util.checkCassandraDbConnections(bodhiKeyspace);
		String cassandraMode = properties.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
		connectionManager = CassandraConnectionMngrFactory.getObject(cassandraMode);
	}

	public PreparedStatement getStatement() {
		PreparedStatement ret = null;
		if (getBadgeMeta == null)
			getBadgeMeta = connectionManager.getSession(bodhiKeyspace)
					.prepare("select badge_id,threshold from " + badgeMeta + " where badge_id='NewUser'");
		ret = getBadgeMeta;
		return ret;
	}

	@Override
	public Map<String, Object> checkHealth() throws Exception {
		Map<String, Object> healthMap = new HashMap<String, Object>();
//		String SMTPHOST = System.getenv(LexJsonKey.SMTP_HOST);
//		String SMTPPORT = System.getenv(LexJsonKey.SMTP_PORT);

//		if (ProjectUtil.isStringNullOREmpty(SMTPHOST) || ProjectUtil.isStringNullOREmpty(SMTPPORT)) {
//			SMTPHOST = properties.getProperty(LexJsonKey.SMTP_HOST);
//			SMTPPORT = properties.getProperty(LexJsonKey.SMTP_PORT);
//		}

		healthMap.put("cassandra", this.checkBodhiCassandra());
		healthMap.put("mongo", this.checkMongo());
		String es = this.checkElastic();
		healthMap.put("elastic", es.equals("1") ? true : es.equals("0") ? false : es);
		healthMap.put("content_store", this.checkContentStore(contentServiceHost, contentPort));
//		healthMap.put("mail", this.checkMail(SMTPHOST, SMTPPORT));
		healthMap.put("application", true);
		return healthMap;
	}

	private Boolean checkBodhiCassandra() {
		try {
			ResultSet result = connectionManager.getSession(bodhiKeyspace)
					.execute(this.getStatement().getQueryString());
			return result.all().size() > 0 ? true : false;
		} catch (Exception e) {
			return false;
		}
	}

	private Boolean checkMongo() {
		try {
			batchExecutionRepo.findByBatchName("badge_batch3",
					PageRequest.of(0, 1, new Sort(Sort.Direction.DESC, "batch_started_on")));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private String checkElastic() {
		try {
			return userUtilService.checkElasticSearch();
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	private Boolean checkContentStore(String contentHost, String contentPort) {
		try {
			return userUtilService.checkContentStore(contentHost, contentPort);
		} catch (Exception e) {
			return false;
		}
	}

	private Boolean checkMail(String SMTPHOST, String SMTPPORT) {
		try {
			Map<String, Object> mailData = userUtilService.getMailData();
			String sender = mailData.get("senderId").toString();
			String senderApplicationName = mailData.get("senderName").toString();

			Properties props = new Properties();
			props.put("mail.smtp.host", SMTPHOST);
			props.put("mail.smtp.port", SMTPPORT);

			Session session = Session.getDefaultInstance(props, null);

			Multipart multipart = new MimeMultipart();
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(sender, senderApplicationName + " Platform"));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(sender));

			BodyPart messageBodyPart = new MimeBodyPart();
			message.setSubject("Health Test Mail");

			messageBodyPart.setContent("Health Test Mail.", "text/html; charset=utf-8");
			multipart.addBodyPart(messageBodyPart);

			message.setContent(multipart);
			Transport.send(message);

		} catch (Exception e) {
			return false;
		}
		return true;
	}

}
