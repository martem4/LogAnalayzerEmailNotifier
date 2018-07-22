package DB;

import Model.LogSysEvent;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DBReader {
    private static final String JDBC_PROPERTIES_FILE = "jdbc.properties";
    private static Properties propertiesDb = new Properties();
    private static InputStream inputStream;
    private static Connection connection;
    private static Statement statement;

    private static Properties readDbConProperties() throws IOException {
        inputStream = new FileInputStream(JDBC_PROPERTIES_FILE);
        propertiesDb.load(inputStream);
        return propertiesDb;
    }

    public static ArrayList<LogSysEvent> readSysEvent() throws IOException {
        readDbConProperties();
        try {
            connection =  DriverManager.getConnection(propertiesDb.getProperty("url"),
                    propertiesDb.getProperty("login"),
                    propertiesDb.getProperty("password"));

            statement = connection.createStatement();
            String query = "select ...";
            statement.execute(query);

        }catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
