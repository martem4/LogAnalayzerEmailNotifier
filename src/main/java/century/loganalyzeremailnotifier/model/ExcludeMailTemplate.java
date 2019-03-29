package century.loganalyzeremailnotifier.model;

import lombok.Data;

@Data
public class ExcludeMailTemplate {
    private final String templateText;
    private final String logName;
}
