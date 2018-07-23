package Listener;

import DB.DBReader;
import Model.LogSysEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class SysEventListener implements EventListener {

    private static boolean READ = true;
    private static final int TIMEOUT_READING_SECONDS = 60;

    ArrayList<LogSysEvent> logList;

    public void listen() {
        while(READ) {
            try {
                logList = DBReader.readSysEventList(TIMEOUT_READING_SECONDS);
                if (logList.size() != 0) {
                    System.out.println(Arrays.toString(logList.toArray()));
                }
                Thread.sleep(TIMEOUT_READING_SECONDS*1000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopListen() {
        READ = false;
    }

    public void sendMail() {
    }
}
