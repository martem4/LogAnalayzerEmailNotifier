package listener;

import db.DBReader;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import mail.MailService;
import model.LogSysEvent;
import model.MailTemplate;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SysEventListener implements EventListener {

    private static boolean READ = true;
    private static final int TIMEOUT_READING_SECONDS = 60;

    public void listenNewEvent() {
        List<MailTemplate> mailTemplates = new MailService().readMailTemplate();
        while(READ) {
            try {
                ArrayList<LogSysEvent> sysEventList = new DBReader().getSysEventList(TIMEOUT_READING_SECONDS);
                if (sysEventList.size() != 0) {
                    for (LogSysEvent logSysEvent : sysEventList) {
                        sendMailByTemplate(mailTemplates, logSysEvent);
                    }
                }
                Thread.sleep(TIMEOUT_READING_SECONDS*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    public void sendMailByTemplate(@NonNull List<MailTemplate> mailTemplateList, LogSysEvent logSysEvent) {
        for (MailTemplate mailTemplate : mailTemplateList) {
            if (mailTemplate.getLogName().toLowerCase().contains(logSysEvent.getSysLogTag().toLowerCase())) {
                for (String recipient : mailTemplate.getRecipients()) {
                    new MailService().sendMail(recipient, logSysEvent.getMessage(), logSysEvent.getSysLogTag(),
                            logSysEvent.getId());
                }
            }
        }
    }


    public void stopListen() {
        READ = false;
    }
}
