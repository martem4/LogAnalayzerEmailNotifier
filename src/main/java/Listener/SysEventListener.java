package Listener;

import DB.DBReader;
import Model.LogSysEvent;
import java.io.IOException;
import java.util.ArrayList;

public class SysEventListener implements EventListener {

    private static boolean READ = true;
    private static final int TIMEOUT_READING = 30; //seconds

    ArrayList<LogSysEvent> logList;

    public void listen() {
        while(READ) {
            try {
                logList = DBReader.readSysEvent();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMail() {

    }
}
