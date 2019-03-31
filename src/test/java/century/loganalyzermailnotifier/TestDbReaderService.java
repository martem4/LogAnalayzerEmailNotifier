package century.loganalyzermailnotifier;

import century.loganalyzeremailnotifier.db.DbReaderService;
import century.loganalyzeremailnotifier.model.ExcludeMailTemplate;
import century.loganalyzeremailnotifier.model.MailTemplate;
import century.loganalyzeremailnotifier.model.SmartMailTemplate;
import org.junit.Test;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestDbReaderService {

    @Test
    public void testGetMailTemplate() throws SQLException {
        DbReaderService dbReaderService = new DbReaderService();
        Map<String, List<MailTemplate>> mailTemplateMap = dbReaderService.getMailTemplate();
        ArrayList<SmartMailTemplate> smartMailTemplateList = dbReaderService.getSmartMailTemplateList();
        ArrayList<ExcludeMailTemplate> excludeMailTemplateList = dbReaderService.getExcludeMailTemplateList();

        System.out.println(mailTemplateMap);
    }
}
