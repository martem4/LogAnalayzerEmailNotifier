package century.loganalyzeremailnotifier.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class LogSysEventMailTemplate {
    private int interval;
    private int intervalBits;
    private int hitPercentage;
    private String templateText;
    private String sysLogTag;

    public LogSysEventMailTemplate(int id, int interval, int intervalBits, int hitPercentage, String templateText, String sysLogTag) {
        this.interval = interval;
        this.intervalBits = intervalBits;
        this.hitPercentage = hitPercentage;
        this.templateText = templateText;
        this.sysLogTag = sysLogTag;
    }
 }
