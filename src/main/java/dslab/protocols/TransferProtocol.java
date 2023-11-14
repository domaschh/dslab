package dslab.protocols;

import at.ac.tuwien.dsg.orvell.Input;
import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import dslab.data.Email;
import dslab.util.Config;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.function.Function;

public class TransferProtocol implements Runnable {
    private Config domainsConfig;
    private Shell shell;
    private final String componentId;
    private Email email;
    Consumer<Email> sendCallback;

    public TransferProtocol(String componentId, Socket socket, Consumer<Email> sendCallback) throws IOException {
        this.shell = new Shell(socket.getInputStream(), new PrintStream(socket.getOutputStream()));
        this.domainsConfig = new Config("domains");
        this.componentId = componentId;
        this.email = new Email();
        this.sendCallback = sendCallback;
        this.setupShellCallbacks();
    }

    @Override
    public void run() {
        shell.setPrompt(componentId + "> ");
        shell.out().println("ok DMTP");
        try {
            shell.run();
        } catch (UncheckedIOException ignored) {
        }
    }

    private void setupShellCallbacks() {
        shell.register("begin", (input, context) -> {
            if (this.email.undefined) {
                shell.out().println("ok");
                email.undefined = false;
            } else {
                shell.out().println("already composing email");
            }
        });
        shell.register("to", (input, context) -> {
            this.emailSetter(input, this.email::setTo);
        });
        shell.register("from", (input, context) -> {
            this.emailSetter(input, this.email::setFrom);
        });
        shell.register("subject", (input, context) -> {
            this.emailSetter(input, this.email::setSubject);
        });
        shell.register("data", (input, context) -> {
            this.emailSetter(input, this.email::setBody);
        });
        shell.register("send", (input, context) -> {
            if (this.email.isReady()) {
                sendCallback.accept(this.email);
                shell.out().println("ok");
                this.email = new Email();
            } else {
                shell.out().println(this.email.whatIsMissing());
            }
        });
        shell.register("quit", (input, context) -> {
            shell.out().println("ok bye");
            throw new StopShellException();
        });
    }

    public void emailSetter(Input input, Function<String, String> cb) {
        if (!email.undefined) {
            String argumentString = String.join("", input.getArguments());
            shell.out().println(cb.apply(argumentString));
        } else {
            shell.out().println("error must begin");
        }
    }
}
