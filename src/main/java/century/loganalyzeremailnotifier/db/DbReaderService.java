package century.loganalyzeremailnotifier.db;

import century.loganalyzeremailnotifier.model.LogSysEventGroup;
import century.loganalyzeremailnotifier.model.LogSysEventMailDbTemplate;
import century.loganalyzeremailnotifier.model.MailTemplate;
import javafx.util.Pair;
import lombok.Cleanup;
import century.loganalyzeremailnotifier.model.LogSysEvent;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DbReaderService {
    private static final String JDBC_PROPERTIES_FILE = "app.properties";

    private Properties readDbConProperties() throws IOException {
        Properties propertiesDb = new Properties();
        @Cleanup InputStream inputStream = new FileInputStream(JDBC_PROPERTIES_FILE);
        propertiesDb.load(inputStream);
        return propertiesDb;
    }

    private Connection getConnectionToDb() {
        Properties propertiesDb = null;
        try {
            propertiesDb = readDbConProperties();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            return DriverManager.getConnection(propertiesDb.getProperty("db.url"),
                    propertiesDb.getProperty("db.login"),
                    propertiesDb.getProperty("db.password"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<LogSysEvent> getLogSysEventList(int timeOutReading) throws SQLException {
        ArrayList<LogSysEvent> logSysEventList = new ArrayList<LogSysEvent>();
        Statement statement = null;
        ResultSet rs;
        try {
            statement = getConnectionToDb().createStatement();
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
        } finally {
            statement.getConnection().close();
        }
        return logSysEventList;
    }

    public ArrayList<LogSysEventMailDbTemplate> getLogSysEventMailDbTemplateList() throws SQLException {
            ArrayList<LogSysEventMailDbTemplate> logSysEventMailDbTemplateList = new ArrayList<>();
            Statement statement = null;
            ResultSet rs;

        try {
            statement = getConnectionToDb().createStatement();
            String query = "select ID, `Interval`, IntervalBits, HitPercentage, TemplateText, SysLogTag" +
                    " from syslog.systemevents_mail_template t;";

            rs = statement.executeQuery(query);
            while (rs.next()) {
                logSysEventMailDbTemplateList.add(new LogSysEventMailDbTemplate(
                        rs.getInt("ID"),
                        rs.getInt("Interval"),
                        rs.getInt("IntervalBits"),
                        rs.getInt("HitPercentage"),
                        rs.getString("TemplateText"),
                        rs.getString("SysLogTag")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            statement.getConnection().close();
        }
        return logSysEventMailDbTemplateList;
    }

    public ArrayList<LogSysEventMailDbTemplate> getLogSysEventMailExcludeDbTemplateList() throws SQLException {
        ArrayList<LogSysEventMailDbTemplate> logSysEventMailDbTemplateList = new ArrayList<>();
        Statement statement = null;
        ResultSet rs;

        try {
            statement = getConnectionToDb().createStatement();
            String query = "select TemplateText, SysLogTag" +
                    " from syslog.systemevents_exclude_mail_template t;";

            rs = statement.executeQuery(query);
            while (rs.next()) {
                logSysEventMailDbTemplateList.add(new LogSysEventMailDbTemplate(
                        rs.getString("TemplateText"),
                        rs.getString("SysLogTag")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            statement.getConnection().close();
        }
        return logSysEventMailDbTemplateList;
    }

    public ArrayList<LogSysEventGroup> getLogSysEventGroupList(int startInterval,
                                                               int stopInterval) throws SQLException {
        ArrayList<LogSysEventGroup> logSysEventGroupList = new ArrayList<LogSysEventGroup>();
        Statement statement = null;
        ResultSet rs;

        try {
            statement = getConnectionToDb().createStatement();
            String query = "select\n" +
                    "  substring(Message, position('ERROR' in Message)) as msg ,\n" +
                    "  SysLogTag,\n" +
                    "  count(*) as Count\n" +
                    "from\n" +
                    "  syslog.systemevents t\n" +
                    "where\n" +
                    "  t.ReceivedAt >= date_sub(now(), interval "+startInterval+" second)\n" +
                    "  and t.ReceivedAt < date_sub(now(), interval "+stopInterval+" second)\n" +
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
        }
        finally {
            statement.getConnection().close();
        }
        return logSysEventGroupList;
    }

    public Map<String, List<String>> getMailTemplateList() throws SQLException {
        ArrayList<MailTemplate> mailTemplateList = new ArrayList<MailTemplate>();
        HashMap<String, List<String>> mailTemplateMap = new HashMap<>();
        Statement statement = null;
        ResultSet rs;

        try {
            statement = getConnectionToDb().createStatement();
            String query = "select p.name\n" +
                    "      ,group_concat(r.mail)\n" +
                    "from project p\n" +
                    "join project_recipient pr\n" +
                    "  on p.id = pr.project_id\n" +
                    "join recipient r on pr.recipient_id = r.id\n" +
                    "group by p.name;";

            rs = statement.executeQuery(query);
            while (rs.next()) {
                mailTemplateMap.put(
                        rs.getString("name"),
                        new ArrayList<String>(rs.getArray("mail")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            statement.getConnection().close();
        }
        return recordList.stream().collect(Collectors.groupingBy(
   }
}
