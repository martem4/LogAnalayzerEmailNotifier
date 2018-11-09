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

        while (READ) {
            try {
                ArrayList<LogSysEvent> sysEventList = dbReaderService.
                        getLogSysEventList(TIMEOUT_READING_SECONDS);

                if (sysEventList != null) {
                    for (LogSysEvent logSysEvent : sysEventList) {
                        sendMailByTemplate(mailService.getMailTemplateXml(), logSysEvent);
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

    private MailTemplate getMailTemplateXml(LogSysEvent logSysEvent) {
        List<MailTemplate> mailTemplatesXml = mailService.getMailTemplateXml();
        if (mailTemplatesXml != null) {
            return mailTemplatesXml.stream().filter(mailTemplate ->
                    logSysEvent.getSysLogTag().equals(mailTemplate.getLogName()))
                    .findAny().orElse(null);
        }
        return null;
    }

    private void sendMailByTemplate(@NonNull List<MailTemplate> mailTemplateXmlList,
                                    @NonNull LogSysEvent logSysEvent) throws SQLException {

        ArrayList<LogSysEventMailDbTemplate> logSysEventMailDbTemplates =
                dbReaderService.getLogSysEventMailDbTemplateList();

        ArrayList<LogSysEventMailDbTemplate> logSysEventMailDbExcludeTemplates =
                dbReaderService.getLogSysEventMailExcludeDbTemplateList();

        for (MailTemplate mailTemplateXml : mailTemplateXmlList) {
            if (isLogSysEventContainMailTemplateXml(logSysEvent, mailTemplateXml)) {
                if (isLogSysEventContainMailDbExcludeTemplate(logSysEventMailDbExcludeTemplates, logSysEvent)) {
                    continue;
                }
                if (isLogSysEventContainMailDbTemplate(logSysEventMailDbTemplates, logSysEvent)) {
                    sendMailByTemplateWithHittingPercentage(logSysEventMailDbTemplates,
                            logSysEvent, mailTemplateXml.getRecipients());
                } else {
                    System.out.println("Sending message without calculation percentage for " +  logSysEvent.getSysLogTag());
                    mailService.sendMail(mailTemplateXml.getRecipients(),
                            logSysEvent.getMessage(),
                            logSysEvent.getSysLogTag(),
                            logSysEvent.getId());
                }
            }

        }
    }

    private void sendMailByTemplateWithHittingPercentage(List<LogSysEventMailDbTemplate> logSysEventMailDbTemplates,
                                                         LogSysEvent logSysEvent,
                                                         List<String> recipients) throws SQLException {

        for (LogSysEventMailDbTemplate logSysEventMailDbTemplate : logSysEventMailDbTemplates) {
            if (logSysEvent.getMessage().contains(logSysEventMailDbTemplate.getTemplateText())) {
                if (isOverLimitHitting(logSysEvent, logSysEventMailDbTemplate)) {
                    System.out.println("Sending message with calculation percentage for " +  logSysEvent.getSysLogTag());
                    mailService.sendMail(recipients,
                            logSysEvent.getMessage(),
                            logSysEvent.getSysLogTag(),
                            logSysEvent.getId());
                }
            }
        }
    }

    private boolean isOverLimitHitting(LogSysEvent logSysEvent, LogSysEventMailDbTemplate logSysEventMailDbTemplate)
            throws SQLException {
        int logSysEventHitPercentage = getLogSysEventHittingPercentage(logSysEventMailDbTemplate, logSysEvent);
        int logSysEventHitPercentageLimit = logSysEventMailDbTemplate.getHitPercentage();

        return (logSysEventHitPercentage >= logSysEventHitPercentageLimit) ? true : false;
    }
    //private boolean isOverLimitHitting

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

    private boolean isLogSysEventContainMailDbExcludeTemplate(List<LogSysEventMailDbTemplate> logSysEventMailDbExludeTemplates,
                                                              LogSysEvent logSysEvent) {
        if (logSysEventMailDbExludeTemplates != null) {
            return logSysEventMailDbExludeTemplates.stream().map(log ->
                    log.getTemplateText()).collect(Collectors.toList()).contains(logSysEvent.getMessage());
        }
        return false;
    }

    private int getLogSysEventHittingPercentage(@NonNull LogSysEventMailDbTemplate logSysEventMailDbTemplate,
                                                 @NonNull LogSysEvent logSysEvent) throws SQLException {
        int step = logSysEventMailDbTemplate.getInterval() / logSysEventMailDbTemplate.getIntervalBits();
        int startInterval = logSysEventMailDbTemplate.getInterval();
        int endInterval = logSysEventMailDbTemplate.getInterval() - step;
        int hitCount = 0;

        while (endInterval >= 0) {
            ArrayList<LogSysEventGroup> logSysEventGroups;
            logSysEventGroups = dbReaderService.getLogSysEventGroupList(startInterval, endInterval);
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
        int hittingPercentage = ((hitCount / logSysEventMailDbTemplate.getIntervalBits()) * 100);
        System.out.println("Hit count for " + logSysEventMailDbTemplate.getSysLogTag() + " = " );

        return  hittingPercentage;
    }

    public void stopListen() {
        READ = false;
    }
}
