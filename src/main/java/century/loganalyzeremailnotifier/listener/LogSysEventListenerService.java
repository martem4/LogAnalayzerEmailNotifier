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
    private void sendMailByTemplate(@NonNull List<MailTemplate> mailTemplateList,
                                    LogSysEvent logSysEvent) {
        for (MailTemplate mailTemplate : mailTemplateList) {
            //coincidence recepient mail template with LogSysEvent
            if (mailTemplate.getLogName().toLowerCase().
                    contains(logSysEvent.getSysLogTag().toLowerCase())) {
                ArrayList<LogSysEventMailTemplate> logSysEventMailTemplates;
                try {
                    logSysEventMailTemplates = dbReaderService.
                            getLogSysEventTemplateMailList();

                    if (isLogSysEventTemplateExist(logSysEventMailTemplates,
                            logSysEvent)) {
                        for(LogSysEventMailTemplate logSysEventMailTemplate :
                                logSysEventMailTemplates) {
                            if(logSysEvent.getMessage().contains(logSysEventMailTemplate.
                                    getTemplateText())) {
                                
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

    private byte getLogSysEventHittingPercentage(@NonNull LogSysEventMailTemplate logSysEventMailTemplate,
                                                 @NonNull LogSysEvent logSysEvent) {
        int step = logSysEventMailTemplate.getInterval() / logSysEventMailTemplate.getIntervalBits();
        int startInterval = logSysEventMailTemplate.getInterval();
        int endInterval = logSysEventMailTemplate.getInterval() - step;

        int hitCount = 0;
        while (endInterval >= 0) {
            ArrayList<LogSysEventGroup> logSysEventGroups = null;
            try {
                logSysEventGroups = dbReaderService.
                        getLogSysEventGroupList(startInterval, endInterval);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            startInterval = endInterval;
            endInterval = startInterval - step;
            if (logSysEventGroups != null) {
                for (LogSysEventGroup logSysEventGroup : logSysEventGroups) {
                    if (logSysEventGroup.getMessage().contains(logSysEvent.getMessage())) {
                        if(logSysEventGroup.getCount() > 0) {
                            hitCount +=1;
                        }
                    }
                }
            }
        }
        return (byte) ((hitCount/logSysEventMailTemplate.getIntervalBits())*100);
    }

    public void stopListen() {
        READ = false;
    }
}
