package century.loganalyzeremailnotifier.listener;

import century.loganalyzeremailnotifier.db.DbReaderService;
import century.loganalyzeremailnotifier.model.*;
import lombok.NonNull;
import century.loganalyzeremailnotifier.mail.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
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
                ArrayList<LogSysEvent> logSysEventList = dbReaderService.getLogSysEventList(periodTimeout);
                //get mail templates
                Map<String, List<MailTemplate>> mailTemplateMap = dbReaderService.getMailTemplate();
                //get smart mail templates
                ArrayList<SmartMailTemplate> smartMailTemplateList = dbReaderService.getSmartMailTemplateList();
                //get exclude mail templates
                ArrayList<ExcludeMailTemplate> excludeMailTemplateList = dbReaderService.getExcludeMailTemplateList();

                if (logSysEventList != null) {
                    sendMailByTemplate(mailTemplateMap, logSysEventList, smartMailTemplateList,
                            excludeMailTemplateList);
                }
                Thread.sleep(1000 * periodTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private List<String> getRecipients(LogSysEvent logSysEvent, Map<String, List<MailTemplate>> mailTemplateMap) {
        return mailTemplateMap.get(logSysEvent.getSysLogTag()).stream().map(MailTemplate::getRecipient).
                collect(Collectors.toList());
/*            return mailTemplatesXml.stream().filter(mailTemplate ->
                    logSysEvent.getSysLogTag().equals(mailTemplate.getLogName()))
                    .findAny().orElse(null);
        }*/
    }

    private void sendMailByTemplate(@NonNull Map<String, List<MailTemplate>> mailTemplateMap,
                                    @NonNull List<LogSysEvent> logSysEventList,
                                    List<SmartMailTemplate> smartMailTemplateList,
                                    List<ExcludeMailTemplate> excludeMailTemplateList) throws SQLException {

        for (LogSysEvent logSysEvent: logSysEventList) {
            if(checkLogSysEventContainMailTemplate(logSysEvent, mailTemplateMap)) {
                List<String> recipients = getRecipients(logSysEvent, mailTemplateMap);
                if (checkLogSysEventContainExcludeMailTemplate(excludeMailTemplateList, logSysEvent)) { continue; }
                if (checkLogSysEventContainSmartMailTemplate(smartMailTemplateList, logSysEvent)) {
                    sendMailBySmartTemplateWithHittingPercentage(smartMailTemplateList, logSysEvent, recipients);
                } else {
                    log.info("Sending message without calculation percentage for " + logSysEvent.getSysLogTag());
                    mailService.sendMail(recipients,
                            logSysEvent.getMessage(),
                            logSysEvent.getSysLogTag(),
                            logSysEvent.getId());
                }
            }
        }
    }

    private void sendMailBySmartTemplateWithHittingPercentage(List<SmartMailTemplate> smartMailTemplateList,
                                                         LogSysEvent logSysEvent,
                                                         List<String> recipients) throws SQLException {

        for (SmartMailTemplate smartMailTemplate : smartMailTemplateList) {
            if ((smartMailTemplate.getTemplateText().matches(logSysEvent.getMessage()))
                    && (smartMailTemplate.getSysLogTag().matches(logSysEvent.getSysLogTag()))) {
                if (isOverLimitHitting(logSysEvent, smartMailTemplate)) {
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

    private boolean isOverLimitHitting(LogSysEvent logSysEvent, SmartMailTemplate smartMailTemplate)
            throws SQLException {
        int logSysEventHitPercentage = getLogSysEventHittingPercentage(smartMailTemplate, logSysEvent);
        int logSysEventHitPercentageLimit = smartMailTemplate.getHitPercentage();

        return logSysEventHitPercentage >= logSysEventHitPercentageLimit;
    }
     private boolean checkLogSysEventContainMailTemplate(@NonNull LogSysEvent logSysEvent,
                                                        @NonNull Map<String, List<MailTemplate>> mailTemplateMap) {
        return mailTemplateMap.containsKey(logSysEvent.getSysLogTag());
    }

    private boolean checkLogSysEventContainSmartMailTemplate(List<SmartMailTemplate> smartMailTemplateList,
                                                       LogSysEvent logSysEvent) {
        if (smartMailTemplateList != null) {
            for (SmartMailTemplate smartMailTemplate : smartMailTemplateList) {
                return checkLogSysEventMatchSmartMailTemplate(logSysEvent, smartMailTemplate);
            }

        }
        return false;
    }

    private boolean checkLogSysEventMatchSmartMailTemplate(LogSysEvent logSysEvent,
                                                           SmartMailTemplate smartMailTemplate) {
        if ((logSysEvent.getSysLogTag().toLowerCase().matches(smartMailTemplate.getSysLogTag().toLowerCase()))
                && (logSysEvent.getMessage().toLowerCase().
                matches(smartMailTemplate.getTemplateText().toLowerCase()))) {
            return true;
        }
        return false;
    }

    private boolean checkLogSysEventGroupMatchSmartMailTemplate(LogSysEventGroup logSysEventGroup,
                                                           SmartMailTemplate smartMailTemplate) {
        if ((logSysEventGroup.getSysLogTag().toLowerCase().matches(smartMailTemplate.getSysLogTag().toLowerCase()))
                && (logSysEventGroup.getMessage().toLowerCase().
                matches(smartMailTemplate.getTemplateText().toLowerCase()))) {
            return true;
        }
        return false;
    }

    private boolean checkLogSysEventContainExcludeMailTemplate(List<ExcludeMailTemplate> excludeMailTemplateList,
                                                              LogSysEvent logSysEvent) {
        if (excludeMailTemplateList != null) {
            List<String> templateList = excludeMailTemplateList.stream().map(ExcludeMailTemplate::getTemplateText).
                    collect(Collectors.toList());
            for (String template : templateList) {
                if (logSysEvent.getMessage().toLowerCase().matches(template.toLowerCase()))
                    return true;
            }
        }
        return false;
    }

    private int getLogSysEventHittingPercentage(@NonNull SmartMailTemplate smartMailTemplate,
                                                 @NonNull LogSysEvent logSysEvent) throws SQLException {
        int step = smartMailTemplate.getInterval() / smartMailTemplate.getIntervalBits();
        int startInterval = smartMailTemplate.getInterval();
        int endInterval = smartMailTemplate.getInterval() - step;
        int hitCount = 0;

        while (endInterval >= 0) {
            //get batch of grouped logs, grouped by msg and syslogtag
            ArrayList<LogSysEventGroup> logSysEventGroups = dbReaderService.
                    getLogSysEventGroupList(startInterval, endInterval);
            if (logSysEventGroups != null) {
                for (LogSysEventGroup logSysEventGroup : logSysEventGroups) {
                    if (checkLogSysEventGroupMatchSmartMailTemplate(logSysEventGroup, smartMailTemplate)) {
                            hitCount += 1;
                    }
                }
            }
            //refresh interval params
            startInterval = endInterval;
            endInterval = startInterval - step;
        }
        int hittingPercentage = ((hitCount / smartMailTemplate.getIntervalBits()) * 100);
        log.info("Hit count for " + smartMailTemplate.getSysLogTag() + " = " );
        log.info("Hit count for " + smartMailTemplate.getSysLogTag() + " = " );
        return  hittingPercentage;
    }

    public void stopListen() {
        READ = false;
    }
}
