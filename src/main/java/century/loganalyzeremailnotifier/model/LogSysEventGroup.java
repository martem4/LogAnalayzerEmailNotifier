package century.loganalyzeremailnotifier.model;

import lombok.Data;

@Data
public class LogSysEventGroup {

    public LogSysEventGroup(String message, String sysLogTag, int count) {
        this.message = message;
        this.sysLogTag = sysLogTag;
        this.count = count;
    }

    private String message;
    private String sysLogTag;
    private int count;


}
