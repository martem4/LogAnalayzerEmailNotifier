package MailSender;

import Model.LogSysEvent;
import Model.MailTemplate;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class MailSender {

    private static final String MAIL_SETTINGS_FILE = "app.properties";
    private static Properties mailProperties;

    public MailSender(){
        super();
        readEmailSettings();
    }

    public static void sendMailToRecipient(List<MailTemplate> mailTemplateList, LogSysEvent logSysEvent) {
        if (mailTemplateList != null) {
            for (MailTemplate mailTemplate : mailTemplateList) {
                for (String recipient : mailTemplate.getRecipients()) {
                    sendMail(recipient, logSysEvent.getMessage(), logSysEvent.getSysLogTag());
                }
            }
        }
    }

    private void readEmailSettings() {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(MAIL_SETTINGS_FILE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            mailProperties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendMail(String recipientMail, String messgage, String programName) {
        Session session = Session.getDefaultInstance(mailProperties);
        MimeMessage mimeMessage = new MimeMessage(session);
        try {
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientMail));
            mimeMessage.setSubject(programName);
            mimeMessage.setText(messgage);
            Transport.send(mimeMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
