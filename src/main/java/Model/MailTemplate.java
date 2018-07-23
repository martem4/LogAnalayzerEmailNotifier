package Model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "mailTemplate")
public class MailTemplate {
    public MailTemplate() {}

    public MailTemplate(String logName, List<String> recepients) {
        this.logName = logName;
        this.recepients = recepients;
    }

    String logName;
    List<String> recepients;

    public String getLogName() {
        return logName;
    }

    public void setLogName(String logName) {
        this.logName = logName;
    }

    public List<String> getRecepients() {
        return recepients;
    }

    public void setRecepients(List<String> recepients) {
        this.recepients = recepients;
    }
}
