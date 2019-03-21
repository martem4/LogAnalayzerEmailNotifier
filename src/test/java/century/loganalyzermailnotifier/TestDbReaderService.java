package century.loganalyzermailnotifier;

import century.loganalyzeremailnotifier.db.DbReaderService;
import century.loganalyzeremailnotifier.model.LogSysEventMailDbTemplate;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Ignore
public class TestDbReaderService {

//    @Test
//    public void testGetSysEventGroupList() {
//        DbReaderService dbReaderService = new DbReaderService();
//        ArrayList<LogSysEventGroup> logSysEventGroups = dbReaderService.getLogSysEventGroupList()
//    }


    @Test
    public void testGetMailTemplate() throws SQLException {
        DbReaderService dbReaderService = new DbReaderService();
        Map<String, Array> mailTemplateMap = dbReaderService.getMailTemplateMap();
        System.out.println(mailTemplateMap);
    }

    @Test
    public void testGetLogSysEventMailTemplateList() throws SQLException {
        DbReaderService dbReaderService = new DbReaderService();
                    ArrayList<LogSysEventMailDbTemplate> logSysEventMailTemplates =
                    dbReaderService.getLogSysEventMailDbTemplateList();

                    for (LogSysEventMailDbTemplate template : logSysEventMailTemplates) {
                        System.out.println(template);
                    }
                    assert logSysEventMailTemplates.size() == 1;
    }
}
