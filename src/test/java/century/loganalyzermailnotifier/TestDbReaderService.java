package century.loganalyzermailnotifier;

import century.loganalyzeremailnotifier.db.DbReaderService;
import century.loganalyzeremailnotifier.model.LogSysEventMailDbTemplate;
import org.junit.Ignore;
import org.junit.Test;
import java.sql.SQLException;
import java.util.ArrayList;

@Ignore
public class TestDbReaderService {

//    @Test
//    public void testGetSysEventGroupList() {
//        DbReaderService dbReaderService = new DbReaderService();
//        ArrayList<LogSysEventGroup> logSysEventGroups = dbReaderService.getLogSysEventGroupList()
//    }

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
