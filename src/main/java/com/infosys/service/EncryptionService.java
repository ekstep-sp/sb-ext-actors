package com.infosys.service;

public interface EncryptionService {

	String decrypt(String data, String key);

	String encrypt(String value, String key);
}
