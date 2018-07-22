package Model;

import java.sql.Date;

public class LogSysEvent {

    public Date getReceivedAt() {
        return receivedAt;
    }

    public Date getDeviceReportedTime() {
        return deviceReportedTime;
    }

    public int getFacility() {
        return facility;
    }

    public int getPriority() {
        return priority;
    }

    public String getFromHost() {
        return fromHost;
    }

    public String getMessage() {
        return message;
    }

    public String getSysLogTag() {
        return sysLogTag;
    }

    private Date receivedAt;
    private Date deviceReportedTime;
    private int facility;
    private int priority;
    private String fromHost;
    private String message;
    private String sysLogTag;

    public LogSysEvent(Date receivedAt, Date deviceReportedTime, int facility, int priority, String fromHost,
                       String message, String sysLogTag) {
        this.receivedAt = receivedAt;
        this.deviceReportedTime = deviceReportedTime;
        this.facility = facility;
        this.priority = priority;
        this.fromHost = fromHost;
        this.message = message;
        this.sysLogTag = sysLogTag;
    }
}
