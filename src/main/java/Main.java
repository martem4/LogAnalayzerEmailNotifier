import listener.SysEventListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class Main {
    public static void main(String[] args) {
        SysEventListener listener = new SysEventListener();
        listener.listenNewEvent();
    }
}
