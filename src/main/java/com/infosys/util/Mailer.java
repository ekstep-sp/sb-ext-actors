/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.util;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sunbird.common.models.util.*;
import org.sunbird.common.models.util.mail.GMailAuthenticator;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.StringWriter;
import java.util.Properties;

public class Mailer {
	private static Properties props = null;
	private static String host;
	private static String port;
	private static String userName;
	private static String password;
	private static String fromEmail;
	static {
		// collecting setup value from ENV
		host = System.getenv(JsonKey.EMAIL_SERVER_HOST);
		port = System.getenv(JsonKey.EMAIL_SERVER_PORT);
		userName = System.getenv(JsonKey.EMAIL_SERVER_USERNAME);
		password = System.getenv(JsonKey.EMAIL_SERVER_PASSWORD);
		fromEmail = System.getenv(JsonKey.EMAIL_SERVER_FROM);
		if (ProjectUtil.isStringNullOREmpty(host) || ProjectUtil.isStringNullOREmpty(port)
				|| ProjectUtil.isStringNullOREmpty(userName) || ProjectUtil.isStringNullOREmpty(password)
				|| ProjectUtil.isStringNullOREmpty(fromEmail)) {
			ProjectLogger.log(
					"Email setting value is not provided by Env variable==" + host + " " + port + " " + fromEmail,
					LoggerEnum.INFO.name());
			initialiseFromProperty();
		}
		props = System.getProperties();
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.socketFactory.port", port);
		/*
		 * props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		 */
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", port);
	}

	/**
	 * This method will initialize values from property files.
	 */
	public static void initialiseFromProperty() {
		host = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_HOST);
		port = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_PORT);
		userName = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_USERNAME);
		password = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_PASSWORD);
		fromEmail = PropertiesCache.getInstance().getProperty(JsonKey.EMAIL_SERVER_FROM);
	}

	/**
	 * this method is used to send email.
	 * 
	 * @param receipent
	 *            email to whom we send mail
	 * @param context
	 *            VelocityContext
	 * @param templateName
	 *            String
	 * @param subject
	 *            subject
	 */
	public static boolean sendMail(String[] receipent, String subject, VelocityContext context, String templateName) {
		Transport transport = null;
		boolean flag = true;
		if (context != null) {
			context.put(JsonKey.FROM_EMAIL, fromEmail);
		}
		try {
			Session session = Session.getInstance(props, new GMailAuthenticator(userName, password));
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(fromEmail));
			int size = receipent.length;
			int i = 0;
			while (size > 0) {
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(receipent[i]));
				i++;
				size--;
			}
			message.setSubject(subject);
			VelocityEngine engine = new VelocityEngine();
			Properties p = new Properties();
			p.setProperty("resource.loader", "class");
			p.setProperty("class.resource.loader.class",
					"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
			engine.init(p);
			Template template = engine.getTemplate(templateName);
			StringWriter writer = new StringWriter();
			template.merge(context, writer);
			message.setContent(writer.toString(), "text/html");
			transport = session.getTransport("smtp");
			transport.connect(host, userName, password);
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
		} catch (Exception e) {
			flag = false;
			ProjectLogger.log(e.toString(), e);
		} finally {
			if (transport != null) {
				try {
					transport.close();
				} catch (MessagingException e) {
					ProjectLogger.log(e.toString(), e);
				}
			}
		}
		return flag;
	}

	/**
	 * this method is used to send email along with CC Recipients list.
	 * 
	 * @param receipent
	 *            email to whom we send mail
	 * @param context
	 *            VelocityContext
	 * @param templateName
	 *            String
	 * @param subject
	 *            subject
	 * @param ccList
	 *            String
	 */
	public static void sendMail(String[] receipent, String subject, VelocityContext context, String templateName,
			String[] ccList) {
		Transport transport = null;
		try {
			Session session = Session.getInstance(props, new GMailAuthenticator(userName, password));
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(fromEmail));
			int size = receipent.length;
			int i = 0;
			while (size > 0) {
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(receipent[i]));
				i++;
				size--;
			}
			size = ccList.length;
			i = 0;
			while (size > 0) {
				message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccList[i]));
				i++;
				size--;
			}
			message.setSubject(subject);
			VelocityEngine engine = new VelocityEngine();
			Properties p = new Properties();
			p.setProperty("resource.loader", "class");
			p.setProperty("class.resource.loader.class",
					"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
			engine.init(p);
			Template template = engine.getTemplate(templateName);
			StringWriter writer = new StringWriter();
			template.merge(context, writer);
			message.setContent(writer.toString(), "text/html");
			transport = session.getTransport("smtp");
			transport.connect(host, userName, password);
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
		} catch (Exception e) {
			ProjectLogger.log(e.toString(), e);
		} finally {
			if (transport != null) {
				try {
					transport.close();
				} catch (MessagingException e) {
					ProjectLogger.log(e.toString(), e);
				}
			}
		}
	}

	/**
	 * this method is used to send email as an attachment.
	 * 
	 * @param receipent
	 *            email to whom we send mail
	 * @param mail
	 *            mail body.
	 * @param subject
	 *            subject
	 * @param filePath
	 *            String
	 */
	public static void sendAttachment(String[] receipent, String mail, String subject, String filePath) {
		Transport transport = null;
		try {
			Session session = Session.getInstance(props, new GMailAuthenticator(userName, password));
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(fromEmail));
			int size = receipent.length;
			int i = 0;
			while (size > 0) {
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(receipent[i]));
				i++;
				size--;
			}
			message.setSubject(subject);
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(mail, "text/html");
			// messageBodyPart.setText(mail);
			// Create a multipar message
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);
			DataSource source = new FileDataSource(filePath);
			messageBodyPart = null;
			messageBodyPart = new MimeBodyPart();
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(filePath);
			multipart.addBodyPart(messageBodyPart);
			message.setSubject(subject);
			message.setContent(multipart);
			transport = session.getTransport("smtp");
			transport.connect(host, userName, password);
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
		} catch (Exception e) {
			ProjectLogger.log(e.toString(), e);
		} finally {
			if (transport != null) {
				try {
					transport.close();
				} catch (MessagingException e) {
					ProjectLogger.log(e.toString(), e);
				}
			}
		}
	}
}
