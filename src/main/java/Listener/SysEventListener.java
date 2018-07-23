package Listener;

import DB.DBReader;
import Model.LogSysEvent;
import Model.MailTemplate;
import Model.MailTemplates;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SysEventListener implements EventListener {

    private static boolean READ = true;
    private static final int TIMEOUT_READING_SECONDS = 60;
    private static final String MAIL_TEMPLATE_RECEPIENT = "log_mail_recepient.xml";

    ArrayList<LogSysEvent> logList;

    public void listen() {
        List<MailTemplate> mailTemplates = readMailTemplate();
        while(READ) {
            try {
                logList = DBReader.readSysEventList(TIMEOUT_READING_SECONDS);
                if (logList.size() != 0) {
                    System.out.println(Arrays.toString(logList.toArray()));
                }
                Thread.sleep(TIMEOUT_READING_SECONDS*1000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private List<MailTemplate> readMailTemplate() {
        List<MailTemplate> mailTemplateList = null;
        try {
            File mailTemplateXml = new File(MAIL_TEMPLATE_RECEPIENT);
            JAXBContext jaxbContext = JAXBContext.newInstance(MailTemplates.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            mailTemplateList = ((MailTemplates)unmarshaller.unmarshal(mailTemplateXml)).getMailTemplates();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return mailTemplateList;
    }

    public void stopListen() {
        READ = false;
    }

    public void sendMail() {
    }
}
