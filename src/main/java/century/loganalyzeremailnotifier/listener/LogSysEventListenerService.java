package century.loganalyzeremailnotifier.listener;

import century.loganalyzeremailnotifier.db.DbReaderService;
import century.loganalyzeremailnotifier.model.LogSysEventGroup;
import century.loganalyzeremailnotifier.model.LogSysEventMailTemplate;
import lombok.NonNull;
import century.loganalyzeremailnotifier.mail.MailService;
import century.loganalyzeremailnotifier.model.LogSysEvent;
import century.loganalyzeremailnotifier.model.MailTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogSysEventListenerService implements EventListener {

    private static boolean READ = true;
    private static final int TIMEOUT_READING_SECONDS = 60;

    private final MailService mailService;
    private final DbReaderService dbReaderService;

    @Autowired
    public LogSysEventListenerService(MailService mailService, DbReaderService dbReaderService) {
        this.dbReaderService = dbReaderService;
        this.mailService = mailService;
    }

    public void listenNewEvent() {
        List<MailTemplate> mailTemplates = mailService.readMailTemplate();
        while(READ) {
            try {
                ArrayList<LogSysEvent> sysEventList = dbReaderService.getLogSysEventList(TIMEOUT_READING_SECONDS);
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
            //coincidence recepient mail template with LogSysEvent
            if (mailTemplate.getLogName().toLowerCase().contains(logSysEvent.getSysLogTag().toLowerCase())) {
                ArrayList<LogSysEventMailTemplate> logSysEventMailTemplates = null;
                try {
                    logSysEventMailTemplates = dbReaderService.getLogSysEventTemplateMailList();
                    //check existing template in sysevent table
                    if (isLogSysEventTemplateExist(logSysEventMailTemplates, logSysEvent)) {

                        //find by message template
                        for(LogSysEventMailTemplate logSysEventMailTemplate : logSysEventMailTemplates) {
                            if(logSysEvent.getMessage().contains(logSysEventMailTemplate.getTemplateText())) {
                                //get data from db about frequency error
                                ArrayList<LogSysEventGroup> logSysEventGroups = null;
                                logSysEventGroups = dbReaderService.getLogSysEventGroupList(logSysEventMailTemplate.getInterval());

                            }
                        }
                    }
                    else {
                        mailService.sendMail(mailTemplate.getRecipients(), logSysEvent.getMessage(), logSysEvent.getSysLogTag(),
                                logSysEvent.getId());
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isLogSysEventTemplateExist(ArrayList<LogSysEventMailTemplate> logSysEventMailTemplates,
                                               LogSysEvent logSysEvent) {
        if (logSysEventMailTemplates != null) {
            return logSysEventMailTemplates.stream().map(log ->
                    log.getSysLogTag()).collect(Collectors.toList()).contains(logSysEvent.getSysLogTag());
        }
        return false;
    }

    public void stopListen() {
        READ = false;
    }
}
