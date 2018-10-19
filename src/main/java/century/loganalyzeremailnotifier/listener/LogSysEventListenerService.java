package century.loganalyzeremailnotifier.listener;

import century.loganalyzeremailnotifier.db.DbReaderService;
import century.loganalyzeremailnotifier.model.LogSysEventGroup;
import century.loganalyzeremailnotifier.model.LogSysEventMailDbTemplate;
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
    public LogSysEventListenerService(MailService mailService,
                                      DbReaderService dbReaderService) {
        this.dbReaderService = dbReaderService;
        this.mailService = mailService;
    }

    public void listenNewEvent() {
        List<MailTemplate> mailTemplatesXml = mailService.readMailTemplateXml();
        while (READ) {
            try {
                ArrayList<LogSysEvent> sysEventList = dbReaderService.
                        getLogSysEventList(TIMEOUT_READING_SECONDS);
                if (sysEventList != null) {
                    if (sysEventList.size() != 0) {
                        for (LogSysEvent logSysEvent : sysEventList) {
                            sendMailByTemplate(mailTemplatesXml, logSysEvent);
                        }
                    }
                }
                Thread.sleep(TIMEOUT_READING_SECONDS * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMailByTemplate(@NonNull List<MailTemplate> mailTemplateXmlList,
                                    @NonNull LogSysEvent logSysEvent) {

        for (MailTemplate mailTemplateXml : mailTemplateXmlList) {
            //1 check enable mail templates in xml
            if (isLogSysEventContainMailTemplateXml(logSysEvent, mailTemplateXml)) {
                ArrayList<LogSysEventMailDbTemplate> logSysEventMailDbTemplates;
                try {
                    //2 check enable xml template in db templates
                    logSysEventMailDbTemplates = dbReaderService.getLogSysEventMailDbTemplateList();
                    if (isLogSysEventContainMailDbTemplate(logSysEventMailDbTemplates, logSysEvent)) {
                        //3send mail by db template with hitting by percentage
                        sendMailByTemplateWithHittingPercentage(logSysEventMailDbTemplates,
                                logSysEvent, mailTemplateXml.getRecipients());
                    } else {
                        mailService.sendMail(mailTemplateXml.getRecipients(),
                                logSysEvent.getMessage(),
                                logSysEvent.getSysLogTag(),
                                logSysEvent.getId());
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendMailByTemplateWithHittingPercentage(List<LogSysEventMailDbTemplate> logSysEventMailDbTemplates,
                                                         LogSysEvent logSysEvent,
                                                         List<String> recipients) {

        for (LogSysEventMailDbTemplate logSysEventMailDbTemplate : logSysEventMailDbTemplates) {
            if (logSysEvent.getMessage().contains(logSysEventMailDbTemplate.getTemplateText())) {
                if (logSysEventMailDbTemplate.getHitPercentage() <=
                        getLogSysEventHittingPercentage(logSysEventMailDbTemplate, logSysEvent)) {
                    mailService.sendMail(recipients,
                            logSysEvent.getMessage(),
                            logSysEvent.getSysLogTag(),
                            logSysEvent.getId());
                }
            }
        }

    }

    private boolean isOverLimitHitting

    private boolean isLogSysEventContainMailTemplateXml(@NonNull LogSysEvent logSysEvent,
                                                        @NonNull MailTemplate mailTemplateXml) {
        return mailTemplateXml.getLogName().toLowerCase()
                .contains(logSysEvent.getSysLogTag().toLowerCase());
    }

    private boolean isLogSysEventContainMailDbTemplate(List<LogSysEventMailDbTemplate> logSysEventMailDbTemplates,
                                                       LogSysEvent logSysEvent) {
        if (logSysEventMailDbTemplates != null) {
            return logSysEventMailDbTemplates.stream().map(log ->
                    log.getSysLogTag()).collect(Collectors.toList()).contains(logSysEvent.getSysLogTag());
        }
        return false;
    }

    private byte getLogSysEventHittingPercentage(@NonNull LogSysEventMailDbTemplate logSysEventMailDbTemplate,
                                                 @NonNull LogSysEvent logSysEvent) {
        int step = logSysEventMailDbTemplate.getInterval() / logSysEventMailDbTemplate.getIntervalBits();
        int startInterval = logSysEventMailDbTemplate.getInterval();
        int endInterval = logSysEventMailDbTemplate.getInterval() - step;

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
                        if (logSysEventGroup.getCount() > 0) {
                            hitCount += 1;
                        }
                    }
                }
            }
        }
        return (byte) ((hitCount / logSysEventMailDbTemplate.getIntervalBits()) * 100);
    }

    public void stopListen() {
        READ = false;
    }
}
