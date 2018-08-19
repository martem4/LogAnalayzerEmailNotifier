package db;

import model.LogSysEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

public class DBReader {
    private static final String JDBC_PROPERTIES_FILE = "app.properties";

    private Properties readDbConProperties() throws IOException {
        Properties propertiesDb = new Properties();
        InputStream inputStream = new FileInputStream(JDBC_PROPERTIES_FILE);
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

    public ArrayList<LogSysEvent> getSysEventList(int timeOutReading) throws SQLException {
        ArrayList<LogSysEvent> sysEventList = new ArrayList<LogSysEvent>();
        Statement statement = null;
        ResultSet rs;
        try {
            statement = getConnectionToDb().createStatement();
            String query = "select ID" +
                    " ,ReceivedAt" +
                    " ,DeviceReportedTime" +
                    " ,Facility" +
                    " ,Priority" +
                    " ,FromHost" +
                    " ,Message" +
                    " ,SysLogTag" +
                    " from syslog.systemevents t" +
                    " where t.ReceivedAt >= date_sub(now(), interval " + timeOutReading + " second )\n" +
                    "  and t.ReceivedAt < now();";
            rs = statement.executeQuery(query);

            while (rs.next()) {
                sysEventList.add(new LogSysEvent(rs.getInt("ID"),
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
        }
        finally {
            statement.getConnection().close();
        }
        return sysEventList;
    }

}
