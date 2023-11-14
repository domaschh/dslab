package dslab.protocols;

import dslab.data.Email;

public class EmailResponseFactory {
    public static String buildEmailWithResponse(String command, String commandValue, Email mail, boolean beginSet) {
        if (command == null) {
            return "No Command given";
        }
        if (!command.equals("begin") && !beginSet) {
            return "No begin has been set";
        }

        switch (command) {
            case "to":
                return mail.setTo(commandValue);
            case "from":
                return mail.setFrom(commandValue);
            case "subject":
                return mail.setSubject(commandValue);
            case "data":
                return mail.setBody(commandValue);
            default:
                return "ERROR in building email";
        }
    }
}
