package century.loganalyzeremailnotifier.model;

import lombok.Data;

import java.sql.Date;

@Data public class LogSysEvent {

    private int id;
    private Date receivedAt;
    private Date deviceReportedTime;
    private int facility;
    private int priority;
    private String fromHost;
    private String message;
    private String sysLogTag;


    public LogSysEvent(int id, Date receivedAt, Date deviceReportedTime, int facility, int priority, String fromHost,
                       String message, String sysLogTag) {
        this.id = id;
        this.receivedAt = receivedAt;
        this.deviceReportedTime = deviceReportedTime;
        this.facility = facility;
        this.priority = priority;
        this.fromHost = fromHost;
        this.message = message;
        this.sysLogTag = sysLogTag;
    }
}
