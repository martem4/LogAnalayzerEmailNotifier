package century.loganalyzeremailnotifier.db;

import century.loganalyzeremailnotifier.model.*;
import org.springframework.stereotype.Service;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DbReaderService {
    private static final String APP_PROPERTIES = "app.properties";
    private Connection connection;

    public DbReaderService() {
        try {
            connection = getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Properties getProperties() throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = null;
        inputStream = new FileInputStream(APP_PROPERTIES);
        properties.load(inputStream);
        return properties;
    }

    private Connection getConnection() throws SQLException, IOException {
        Properties propertiesDb = getProperties();
        return DriverManager.getConnection(propertiesDb.getProperty("db.url"),
                propertiesDb.getProperty("db.login"),
                propertiesDb.getProperty("db.password"));
    }

    public void closeConnection() throws SQLException {
        this.connection.close();
    }

    public ArrayList<LogSysEvent> getLogSysEventList(int timeOutReading)
            throws SQLException {
        ArrayList<LogSysEvent> logSysEventList = new ArrayList<LogSysEvent>();
        Statement statement = null;
        ResultSet rs;
        try {
            statement = connection.createStatement();
            String query = "select min(id) as minId,\n" +
                    "       substring(Message, position('ERROR' in Message)) as Msg,\n" +
                    "       min(date_format(ReceivedAt, '%Y-%m-%d %H:%i')) as RcvAt,\n" +
                    "       min(date_format(DeviceReportedTime, '%Y-%m-%d %H:%i')) as devRepTime,\n" +
                    "       Facility,\n" +
                    "       Priority,\n" +
                    "       FromHost,\n" +
                    "       SysLogTag\n" +
                    "from syslog.systemevents\n" +
                    "where ReceivedAt >= date_sub(now(), interval " + timeOutReading + " second)\n" +
                    " and ReceivedAt < now()\n" +
                    "group by msg , Facility, Priority, FromHost, SysLogTag;";
            rs = statement.executeQuery(query);

            while (rs.next()) {
                logSysEventList.add(new LogSysEvent(rs.getInt("minId"),
                        rs.getDate("RcvAt"),
                        rs.getDate("devRepTime"),
                        rs.getInt("Facility"),
                        rs.getInt("Priority"),
                        rs.getString("FromHost"),
                        rs.getString("Msg"),
                        rs.getString("SysLogTag")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return logSysEventList;
        }
        return null;
    }

    public ArrayList<SmartMailTemplate> getSmartMailTemplateList()
            throws SQLException {
        ArrayList<SmartMailTemplate> logSysEventMailDbTemplateList = new ArrayList<>();
        Statement statement = null;
        ResultSet rs;

        statement = this.connection.createStatement();
        String query = "SELECT ID, `Interval`, IntervalBits, HitPercentage, TemplateText, SysLogTag" +
                " FROM syslog.systemevents_mail_template t;";

        rs = statement.executeQuery(query);
        while (rs.next()) {
            logSysEventMailDbTemplateList.add(new SmartMailTemplate(
                    rs.getInt("ID"),
                    rs.getInt("Interval"),
                    rs.getInt("IntervalBits"),
                    rs.getInt("HitPercentage"),
                    rs.getString("TemplateText"),
                    rs.getString("SysLogTag")));
        }
        return logSysEventMailDbTemplateList;
    }

    public ArrayList<ExcludeMailTemplate> getExcludeMailTemplateList() throws SQLException {
        ArrayList<ExcludeMailTemplate> excludeMailTemplateList = new ArrayList<>();
        Statement statement;
        ResultSet rs;

            statement = this.connection.createStatement();
            String query = "select TemplateText, SysLogTag" +
                    " from syslog.systemevents_exclude_mail_template t;";

            rs = statement.executeQuery(query);
            while (rs.next()) {
                excludeMailTemplateList.add(new ExcludeMailTemplate(
                        rs.getString("TemplateText"),
                        rs.getString("SysLogTag")));
            }
        return excludeMailTemplateList;
    }
    public ArrayList<LogSysEventGroup> getLogSysEventGroupList(int startInterval,
                                                               int stopInterval) throws SQLException {
        ArrayList<LogSysEventGroup> logSysEventGroupList = new ArrayList<LogSysEventGroup>();
        Statement statement = null;
        ResultSet rs;

        try {
            statement = this.connection.createStatement();
            String query = "select\n" +
                    "  substring(Message, position('ERROR' in Message)) as msg ,\n" +
                    "  SysLogTag,\n" +
                    "  count(*) as Count\n" +
                    "from\n" +
                    "  syslog.systemevents t\n" +
                    "where\n" +
                    "  t.ReceivedAt >= date_sub(now(), interval " + startInterval + " second)\n" +
                    "  and t.ReceivedAt < date_sub(now(), interval " + stopInterval + " second)\n" +
                    "group by msg, SysLogTag\n" +
                    "order by  Count desc";

            rs = statement.executeQuery(query);
            while (rs.next()) {
                logSysEventGroupList.add(new LogSysEventGroup(
                        rs.getString("msg"),
                        rs.getString("SysLogTag"),
                        rs.getInt("Count")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            statement.getConnection().close();
        }
        return logSysEventGroupList;
    }

    public Map<String, List<MailTemplate>> getMailTemplate() throws SQLException {
        HashMap<String, List<String>> mailTemplateMap = new HashMap<>();
        List<MailTemplate> mailTemplateList = new ArrayList<>();
        ResultSet rs;

        Statement statement = connection.createStatement();
        String query = "select p.name as name\n" +
                ",r.mail as mail\n" +
                "from project p\n" +
                "join project_recipient pr\n" +
                "on p.id = pr.project_id\n" +
                "join recipient r on pr.recipient_id = r.id\n" +
                "order by name";

        rs = statement.executeQuery(query);
        while (rs.next()) {
            mailTemplateList.add(
                    new MailTemplate(
                            rs.getString("name"),
                            rs.getString("mail")));
        }

        if (mailTemplateList != null) {
            return mailTemplateList.stream().collect(Collectors.groupingBy(MailTemplate::getLogName));
        }
        else return null;
    }
}
