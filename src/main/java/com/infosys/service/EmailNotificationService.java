/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.mail.MessagingException;

public interface EmailNotificationService {

    public Map<String, Object> Notify(Map<String, Object> data);

    public Map<String, Object> NotifyGroup(Map<String, Object> data);

    public Map<String, Object> PlainMail(Map<String, Object> data);
    
    public Map<String, Object> NotifyReview(Map<String, Object> data);

    void VerifyForOneMail(Map<String, Object> data);

    void VerifyForReview(Map<String, Object> data);

    void VerifyForGroup(Map<String, Object> data);

	void PDFMail() throws UnsupportedEncodingException, MessagingException, IOException;

	void convertUUIDtoEmail(Map<String, Object> requestBody);

}
