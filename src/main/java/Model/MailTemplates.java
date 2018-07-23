package Model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "mailTemplates")
public class MailTemplates {
    public MailTemplates() { }

    List<MailTemplate> mailTemplates;

    public MailTemplates(List<MailTemplate> mailTemplates) {
        super();
        this.mailTemplates = mailTemplates;
    }

    @XmlElement
    public List<MailTemplate> getMailTemplates() {
        return mailTemplates;
    }

    public void setMailTemplates(List<MailTemplate> mailTemplates) {
        this.mailTemplates = mailTemplates;
    }
}
