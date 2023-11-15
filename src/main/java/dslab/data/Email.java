package dslab.data;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Email {
    private static String VALIDATION_STRING = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";
    private String from;
    private List<String> to;
    private String subject;
    private String body;
    public boolean undefined = true;

    public Email(String from, List<String> to, String subject, String body) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public Email() {}

    public String getFrom() {
        return from;
    }

    public String setFrom(String from) {
        if (this.isValidMail(from)) {
            this.from = from;
            return "ok";
        } else {
            return "error in email formatting";
        }
    }

    public List<String> getTo() {
        return to;
    }

    public String setTo(String to, String domain) {
        String[] recipients = to.split(",");
        recipients = domain != null ? Arrays.stream(recipients).filter(r -> r.contains(domain)).toArray(String[]::new): recipients;
        if (recipients.length > 0 && Arrays.stream(recipients).allMatch(this::isValidMail)) {
            this.to = Arrays.stream(recipients).collect(Collectors.toList());
            return "ok " + this.to.size();
        } else {
            return "error wrong email recipient format provided";
        }
    }

    public String getSubject() {
        return subject;
    }

    public String setSubject(String subject) {
        this.subject = subject;
        return "ok";
    }

    public String getBody() {
        return body;
    }

    public String setBody(String body) {
        this.body = body;
        return "ok";
    }

    public boolean isReady() {
        return (this.from != null && this.body!=null && this.subject != null && this.to != null);
    }

    @Override
    public String toString() {
        return
                "from " + from + '\n' +
                "to " + String.join(",", to) + '\n' +
                "subject " + subject + '\n' +
                "data " + body + '\n';
    }

    private boolean isValidMail(String input) {
        return Pattern.matches(VALIDATION_STRING, input);
    }

    public String whatIsMissing() {
        StringBuilder sb = new StringBuilder();
        sb.append("error no");
        if (this.to == null) {
            sb.append(" recipients");
        }
        if (this.from == null) {
            sb.append(" from");
        }

        if (this.subject == null) {
            sb.append(" subject");
        }

        if (this.body == null) {
            sb.append(" body");
        }

        return sb.toString();
    }
}
