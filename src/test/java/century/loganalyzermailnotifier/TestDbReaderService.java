package century.loganalyzermailnotifier;

import century.loganalyzeremailnotifier.db.DbReaderService;
import century.loganalyzeremailnotifier.model.LogSysEventMailTemplate;
import org.junit.Test;
import java.sql.SQLException;
import java.util.ArrayList;

public class TestDbReaderService {

//    @Test
//    public void testGetSysEventGroupList() {
//        DbReaderService dbReaderService = new DbReaderService();
//        ArrayList<LogSysEventGroup> logSysEventGroups = dbReaderService.getLogSysEventGroupList()
//    }

    @Test
    public void testGetLogSysEventMailTemplateList() throws SQLException {
        DbReaderService dbReaderService = new DbReaderService();
                    ArrayList<LogSysEventMailTemplate> logSysEventMailTemplates =
                    dbReaderService.getLogSysEventTemplateMailList();

                    for (LogSysEventMailTemplate template : logSysEventMailTemplates) {
                        System.out.println(template);
                    }
                    assert logSysEventMailTemplates.size() == 1;
    }
}
