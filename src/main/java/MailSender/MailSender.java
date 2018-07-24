package MailSender;

import Model.MailTemplate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class MailSender {

    private static final String MAIL_SETTINGS_FILE = "app.properties";
    private static Properties mailProperties;
    
    public void sendMailToRecipient(List<MailTemplate> mailTemplateList) {

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
}
