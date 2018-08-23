package century.loganalyzeremailnotifier.model;

import lombok.Data;

@Data public class LogSysEventTemplateMail {
    private int interval;
    private int intervalBits;
    private int hitPercentage;
    private String templateText;
    private String sysLogTag;
}
