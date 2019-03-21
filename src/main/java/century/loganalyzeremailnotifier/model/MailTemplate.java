package century.loganalyzeremailnotifier.model;

import lombok.Data;

import java.sql.Array;
import java.util.List;

//@XmlRootElement(name = "mailTemplate")
@Data
public class MailTemplate {

    private final String logName;
    private final String recipient;
//    @XmlElementWrapper
//    @XmlElement(name = "recipient")
}
