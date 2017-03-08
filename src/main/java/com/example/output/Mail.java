package com.example.output;

import com.ctrip.ops.sysdev.baseplugin.BaseOutput;
import com.ctrip.ops.sysdev.render.TemplateRender;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;


import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.*;


public class Mail extends BaseOutput {
    private static final Logger logger = Logger.getLogger(com.example.output.Mail.class.getName());

    private String mailHost, from, login_user, login_password, subject, contentType;
    private String[] toList;
    private boolean auth;
    private int tryTime, interval;
    private TemplateRender format;

    public Mail(Map config) {
        super(config);
    }

    @Override
    protected void prepare() {
        this.mailHost = (String) this.config.get("mailhost");
        this.from = (String) this.config.get("from_addr");
        ArrayList<String> t = (ArrayList<String>) this.config.get("to_list");
        String[] _toList = new String[]{};
        _toList = t.toArray(_toList);
        this.toList = _toList;
        this.subject = (String) this.config.get("subject");

        if (this.config.containsKey("content_type"))
            this.contentType = (String) this.config.get("contentType");
        else
            this.contentType = "text/plain";

        if (this.config.containsKey("auth")) {
            this.auth = (Boolean) this.config.get("auth");
        } else {
            this.auth = true;
        }

        this.login_user = (String) this.config.get("login_user");
        this.login_password = (String) this.config.get("login_password");

        if (this.config.containsKey("try_time")) {
            this.tryTime = (int) this.config.get("try_time");
        } else {
            this.tryTime = 1;
        }

        if (this.config.containsKey("interval")) {
            this.interval = (int) this.config.get("interval");
        } else {
            this.interval = 5;
        }

        if (this.config.containsKey("format")) {
            String _format = (String) this.config.get("format");
            try {
                this.format = TemplateRender.getRender(_format);
            } catch (IOException e) {
                logger.fatal("could not build template render from " + _format);
                System.exit(1);
            }
        } else {
            this.format = null;
        }
    }

    private void sendEmail(final String sender, String[] receivers,
                           String subject, String mailContent) throws Exception {
        Properties props = new Properties();

        props.put("mail.smtp.host", this.mailHost);

        Session session = null;
        if (this.auth == true) {
            props.put("mail.smtp.auth", "true");
            Authenticator authenticator = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(login_user, login_password);
                }
            };
            session = Session.getDefaultInstance(props, authenticator);
        } else {
            props.put("mail.smtp.auth", "false");
            session = Session.getDefaultInstance(props);
        }

        MimeMessage mimeMessage = new MimeMessage(session);

        mimeMessage.setFrom(new InternetAddress(sender));

        InternetAddress[] receiver = new InternetAddress[receivers.length];
        for (int i = 0; i < receivers.length; i++) {
            receiver[i] = new InternetAddress(receivers[i]);
        }

        mimeMessage.setRecipients(Message.RecipientType.TO, receiver);
        mimeMessage.setSentDate(new Date());
        mimeMessage.setSubject(subject);
        mimeMessage.setText(mailContent);

        Transport.send(mimeMessage);
    }

    @Override
    protected void emit(Map event) {
        String msg;
        if (this.format != null) {
            Object o = this.format.render(event);
            if (o == null) {
                msg = JSONObject.toJSONString(event);
            } else {
                msg = o.toString();
            }
        } else {
            msg = JSONObject.toJSONString(event);
        }

        int try_count = 0;

        while (try_count < this.tryTime) {
            try {
                sendEmail(this.from, this.toList, this.subject, msg);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(String.format("failed to send mail:%s. try after %d seconds", e.getMessage(), this.interval));
                try_count++;
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e1) {

                }
            }
        }
        logger.error(String.format("could not send mail for %d times.", this.tryTime));
    }
}
