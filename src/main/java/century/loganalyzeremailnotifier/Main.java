package century.loganalyzeremailnotifier;

import century.loganalyzeremailnotifier.listener.LogSysEventListenerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class Main {
    private final LogSysEventListenerService logSysEventListenerService;
    @Autowired
    public Main(LogSysEventListenerService logSysEventListenerService) {
        this.logSysEventListenerService = logSysEventListenerService;
    }
    public static void main(String[] args) {
        final AnnotationConfigApplicationContext annotationConfigApplicationContext =
                new AnnotationConfigApplicationContext("century.loganalyzeremailnotifier");
        Main main = annotationConfigApplicationContext.getBean(Main.class);
        main.startListenLogEvents();
    }

    private void startListenLogEvents() {
        logSysEventListenerService.listenNewEvent();
    }
}
