package century.loganalyzeremailnotifier.listener;

import century.loganalyzeremailnotifier.db.DbReaderService;
import century.loganalyzeremailnotifier.model.LogSysEventGroup;
import century.loganalyzeremailnotifier.model.LogSysEventMailDbTemplate;
import lombok.NonNull;
import century.loganalyzeremailnotifier.mail.MailService;
import century.loganalyzeremailnotifier.model.LogSysEvent;
import century.loganalyzeremailnotifier.model.MailTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LogSysEventListenerService implements EventListener {

    private  boolean READ = true;
    private  final String APP_SETTINGS_FILE = "app.properties";
    private Properties appProperties = new Properties();
    private final MailService mailService;
    private final DbReaderService dbReaderService;

    @Autowired
    public LogSysEventListenerService(MailService mailService,
                                      DbReaderService dbReaderService) {
        this.dbReaderService = dbReaderService;
        this.mailService = mailService;
    }

    private Properties getConfig() {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(APP_SETTINGS_FILE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            appProperties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return appProperties;

    }

    public void listenNewEvent() {
        int periodTimeout = Integer.parseInt((getConfig().getProperty("period.timeout")));
        while (READ) {
            try {
                //get incoming events from  mysql db
                ArrayList<LogSysEvent> sysEventList = dbReaderService.
                        getLogSysEventList(periodTimeout);

                if (sysEventList != null) {
                    //sending message but before filter by templates
                    //get templates from xml file (what to send)
                    List<MailTemplate> xmlMailTemplates = mailService.getMailTemplateXml();
                    for (LogSysEvent logSysEvent : sysEventList) {
                        sendMailByTemplate(xmlMailTemplates, logSysEvent);
                    }
                }
                Thread.sleep(1000 * periodTimeout);
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

        //get  templates with delays for sending for knowing events
        ArrayList<LogSysEventMailDbTemplate> logSysEventMailDbTemplates =
                dbReaderService.getLogSysEventMailDbTemplateList();

        //get templates for excluding to send
        ArrayList<LogSysEventMailDbTemplate> logSysEventMailDbExcludeTemplates =
                dbReaderService.getLogSysEventMailExcludeDbTemplateList();

        //consider all templates
        for (MailTemplate mailTemplateXml : mailTemplateXmlList) {
            if (logSysEventContainMailTemplateXml(logSysEvent, mailTemplateXml)) {
                if (logSysEventContainMailDbExcludeTemplate(logSysEventMailDbExcludeTemplates, logSysEvent)) {
                    continue;
                }
                //if event is hitting with special templates
                if (logSysEventContainMailDbTemplate(logSysEventMailDbTemplates, logSysEvent)) {
                    sendMailByTemplateWithHittingPercentage(logSysEventMailDbTemplates,
                            logSysEvent, mailTemplateXml.getRecipients());
                } else {
                    log.info("Sending message without calculation percentage for " +  logSysEvent.getSysLogTag());
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
                    log.info("Sending message with calculation percentage for " +  logSysEvent.getSysLogTag());
                    mailService.sendMail(recipients,
                            logSysEvent.getMessage(),
                            logSysEvent.getSysLogTag(),
                            logSysEvent.getId());
                }
                else {
                    log.info("The message was not sended because percentage is lower then needed!");
                }
            }
        }
    }

    private boolean isOverLimitHitting(LogSysEvent logSysEvent, LogSysEventMailDbTemplate logSysEventMailDbTemplate)
            throws SQLException {
        int logSysEventHitPercentage = getLogSysEventHittingPercentage(logSysEventMailDbTemplate, logSysEvent);
        int logSysEventHitPercentageLimit = logSysEventMailDbTemplate.getHitPercentage();

        return logSysEventHitPercentage >= logSysEventHitPercentageLimit;
    }

     private boolean logSysEventContainMailTemplateXml(@NonNull LogSysEvent logSysEvent,
                                                        @NonNull MailTemplate mailTemplateXml) {
        return mailTemplateXml.getLogName().toLowerCase()
                .contains(logSysEvent.getSysLogTag().toLowerCase());
    }

    private boolean logSysEventContainMailDbTemplate(List<LogSysEventMailDbTemplate> logSysEventMailDbTemplates,
                                                       LogSysEvent logSysEvent) {
        if (logSysEventMailDbTemplates != null) {
            return logSysEventMailDbTemplates.stream().map(LogSysEventMailDbTemplate::getSysLogTag).
                    collect(Collectors.toList()).contains(logSysEvent.getSysLogTag());
        }
        return false;
    }

    private boolean logSysEventContainMailDbExcludeTemplate(List<LogSysEventMailDbTemplate> logSysEventMailDbExludeTemplates,
                                                              LogSysEvent logSysEvent) {
        if (logSysEventMailDbExludeTemplates != null) {
            List<String> excludeTemplateList = logSysEventMailDbExludeTemplates.stream().map(LogSysEventMailDbTemplate::getTemplateText).
                    collect(Collectors.toList());
            for (String template : excludeTemplateList) {
                if (logSysEvent.getMessage().toLowerCase().contains(template.toLowerCase()))
                    return true;
            }
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
        log.info("Hit count for " + logSysEventMailDbTemplate.getSysLogTag() + " = " );
        log.info("Hit count for " + logSysEventMailDbTemplate.getSysLogTag() + " = " );

        return  hittingPercentage;
    }

    public void stopListen() {
        READ = false;
    }
}
