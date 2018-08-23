package century.loganalyzeremailnotifier;

import century.loganalyzeremailnotifier.listener.SysEventListenerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class Main {
    private final SysEventListenerService sysEventListenerService;
    @Autowired
    public Main(SysEventListenerService sysEventListenerService) {
        this.sysEventListenerService = sysEventListenerService;
    }
    public static void main(String[] args) {
        final AnnotationConfigApplicationContext annotationConfigApplicationContext =
                new AnnotationConfigApplicationContext("century.loganalyzeremailnotifier");
        Main main = annotationConfigApplicationContext.getBean(Main.class);
        main.startListenLogEvents();
    }

    private void startListenLogEvents() {
        sysEventListenerService.listenNewEvent();
    }
}
