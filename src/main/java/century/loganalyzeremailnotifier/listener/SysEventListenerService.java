package century.loganalyzeremailnotifier.listener;

import century.loganalyzeremailnotifier.db.DbReaderService;
import lombok.NonNull;
import century.loganalyzeremailnotifier.mail.MailService;
import century.loganalyzeremailnotifier.model.LogSysEvent;
import century.loganalyzeremailnotifier.model.MailTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SysEventListenerService implements EventListener {

    private static boolean READ = true;
    private static final int TIMEOUT_READING_SECONDS = 60;

    private final MailService mailService;
    private final DbReaderService dbReaderService;

    @Autowired
    public SysEventListenerService(MailService mailService, DbReaderService dbReaderService) {
        this.dbReaderService = dbReaderService;
        this.mailService = mailService;
    }

    public void listenNewEvent() {
        List<MailTemplate> mailTemplates = mailService.readMailTemplate();
        while(READ) {
            try {
                ArrayList<LogSysEvent> sysEventList = dbReaderService.getSysEventList(TIMEOUT_READING_SECONDS);
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
    private void sendMailByTemplate(@NonNull List<MailTemplate> mailTemplateList, LogSysEvent logSysEvent) {
        for (MailTemplate mailTemplate : mailTemplateList) {
            if (mailTemplate.getLogName().toLowerCase().contains(logSysEvent.getSysLogTag().toLowerCase())) {
                for (String recipient : mailTemplate.getRecipients()) {
                    mailService.sendMail(recipient, logSysEvent.getMessage(), logSysEvent.getSysLogTag(),
                            logSysEvent.getId());
                }
            }
        }
    }


    public void stopListen() {
        READ = false;
    }
}
