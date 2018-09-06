package century.loganalyzeremailnotifier.db;

import century.loganalyzeremailnotifier.model.LogSysEventGroup;
import century.loganalyzeremailnotifier.model.LogSysEventMailTemplate;
import lombok.Cleanup;
import century.loganalyzeremailnotifier.model.LogSysEvent;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

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
            String query = "select ID ,ReceivedAt ,DeviceReportedTime ,Facility ,Priority ,FromHost ,Message" +
                    " ,SysLogTag" +
                    " from syslog.systemevents t" +
                    " where t.ReceivedAt >= date_sub(now(), interval " + timeOutReading + " second )\n" +
                    "  and t.ReceivedAt < now();";
            rs = statement.executeQuery(query);

            while (rs.next()) {
                logSysEventList.add(new LogSysEvent(rs.getInt("ID"),
                        rs.getDate("ReceivedAt"),
                        rs.getDate("DeviceReportedTime"),
                        rs.getInt("Facility"),
                        rs.getInt("Priority"),
                        rs.getString("FromHost"),
                        rs.getString("Message"),
                        rs.getString("SysLogTag")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            statement.getConnection().close();
        }
        return logSysEventList;
    }

    public ArrayList<LogSysEventMailTemplate> getLogSysEventTemplateMailList() throws SQLException {
            ArrayList<LogSysEventMailTemplate> logSysEventMailTemplateList = new ArrayList<LogSysEventMailTemplate>();
            Statement statement = null;
            ResultSet rs;

        try {
            statement = getConnectionToDb().createStatement();
            String query = "select ID, `Interval`, IntervalBits, HitPercentage, TemplateText, SysLogTag" +
                    " from syslog.systemevents_mail_template t;";

            rs = statement.executeQuery(query);
            while (rs.next()) {
                logSysEventMailTemplateList.add(new LogSysEventMailTemplate(
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
        return logSysEventMailTemplateList;
    }

    public ArrayList<LogSysEventGroup> getLogSysEventGroupList(int interval, int countSegment) throws SQLException {
        ArrayList<LogSysEventGroup> logSysEventGroupList = new ArrayList<LogSysEventGroup>();
        Statement statement = null;
        ResultSet rs;

        try {
            statement = getConnectionToDb().createStatement();
            String query = "select\n" +
                    "  substring(Message, position('ERROR' in Message)) as Message ,\n" +
                    "  SysLogTag,\n" +
                    "  count(*) as Count\n" +
                    "from\n" +
                    "  syslog.systemevents t\n" +
                    "where\n" +
                    "  t.ReceivedAt >= date_sub( now(),interval "+interval+" second )\n" +
                    "  and t.ReceivedAt < now()\n" +
                    "group by Message, SysLogTag\n" +
                    "order by  Count desc";

            rs = statement.executeQuery(query);
            while (rs.next()) {
                logSysEventGroupList.add(new LogSysEventGroup(
                        rs.getString("Message"),
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
}
