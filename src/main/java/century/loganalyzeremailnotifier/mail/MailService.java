package century.loganalyzeremailnotifier.mail;

import century.loganalyzeremailnotifier.model.MailTemplate;
import century.loganalyzeremailnotifier.model.MailTemplates;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.List;
import java.util.Properties;

@Service
public class MailService {
    private static final String MAIL_SETTINGS_FILE = "app.properties";
    private static String LOGANALYZER_LOG_LINK_TEMPLATE="http://172.172.174.100/loganalyzer/details.php?uid=";
    private static final String MAIL_TEMPLATE_RECIPIENTS = "log_mail_recipient.xml";
    private static Properties mailProperties = new Properties();

     private Properties readEmailSettings() {
        FileInputStream inputStream = null;
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
        return mailProperties;
    }

    public void sendMail(List<String> mailRecipients, String messgage, String programName, int id) {
        mailProperties = readEmailSettings();
        Session session = Session.getDefaultInstance(mailProperties,
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(mailProperties
                                .getProperty("mail.smtp.user"), mailProperties
                                .getProperty("mail.smtp.password"));
                    }
                });

        MimeMessage mimeMessage = new MimeMessage(session);
        for (String recipient : mailRecipients) {
            try {
                mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                mimeMessage.setSubject(programName);
                mimeMessage.setText(LOGANALYZER_LOG_LINK_TEMPLATE + id + "\n" + messgage);
                Transport.send(mimeMessage);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }

    public List<MailTemplate> getMailTemplateXml() {
        List<MailTemplate> mailTemplateList = null;
        try {
            File mailTemplateXml = new File(MAIL_TEMPLATE_RECIPIENTS);
            JAXBContext jaxbContext = JAXBContext.newInstance(MailTemplates.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            mailTemplateList = ((MailTemplates)unmarshaller.unmarshal(mailTemplateXml)).getMailTemplate();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return mailTemplateList;
    }
}
