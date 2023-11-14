package dslab.util;

import dslab.data.Email;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class ResponseTask implements Runnable {

    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final ExecutorService forwarderPool;
    private final Config config;
    private final Config domains;

    public ResponseTask(InputStream inputStream, OutputStream outputStream, Config config, Config domains, ExecutorService forwardingExecutor) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.config = config;
        this.forwarderPool = forwardingExecutor;
        this.domains = domains;
    }

    @Override
    public void run() {
        BufferedReader requestReader = new BufferedReader(new InputStreamReader(this.inputStream));
        PrintWriter responseWriter = new PrintWriter(this.outputStream);

        responseWriter.println("ok DMTP");
        try {
            String line;
            Email mail = new Email();
            String responseString;

            var beginSet = false;

            while ((line = requestReader.readLine()) != null) {
                String[] parts = line.split(" ", 2);

                String command = parts[0];
                String commandValue = parts[1];

                if (command.equals("quit")) break;

                if (command.equals("begin")) {
                    beginSet = true;
                    responseString = "ok";
                } else if (Objects.equals(command, "send") && mail.isReady()) {
                    responseString = sendMail(mail, config,domains);
                } else if (Objects.equals(command, "send") && mail.isReady()) {
                    responseString = "error " + mail.whatIsMissing();
                } else {
                    responseString = EmailResponseFactory.buildEmailWithResponse(command, commandValue, mail, beginSet);
                }

                responseWriter.println(responseString);
                responseWriter.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String sendMail(Email mail, Config config, Config domains) {
        mail.getTo().stream().filter(this::isValidRecipient).forEach(to -> {
            var forwardTask = new ForwardTask(this.recipDomain(to), mail, forwarderPool, config);
            forwarderPool.execute(forwardTask);
        });
        return "ok";
    }

    private boolean isValidRecipient(String recipient) {
        String[] split = recipient.split("@");
        var domain = split[1];
        return this.domains.containsKey(domain);
    }

    private String recipDomain(String to) {
        if (!this.domains.containsKey(to)) {
            return null;
        }
        return this.domains.getString(to);
    }
}
