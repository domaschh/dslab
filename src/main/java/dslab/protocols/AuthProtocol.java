package dslab.protocols;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import dslab.data.Email;
import dslab.util.Config;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class AuthProtocol implements Runnable {
    private final Config users;
    private Shell shell;
    private final String componentId;
    private final ConcurrentMap<String, ConcurrentLinkedQueue<Email>> database;
    private String loggedInUsername;

    public AuthProtocol(String componentId, Socket socket, Config config, ConcurrentMap<String, ConcurrentLinkedQueue<Email>> database) throws IOException {
        this.shell = new Shell(socket.getInputStream(), new PrintStream(socket.getOutputStream()));
        this.componentId = componentId;
        this.users = new Config(config.getString("users.config"));
        this.database = database;
        this.setupShellCallbacks();
    }

    @Override
    public void run() {
        shell.out().println("ok DMAP");
        shell.setPrompt("");
        try {
            shell.run();
        } catch (UncheckedIOException ignored) {
        }
    }

    private void setupShellCallbacks() {
        shell.register("login", (input, context) -> {
            if (input.getArguments().size() == 2) {
                String username = input.getArguments().get(0);
                String password = input.getArguments().get(1);
                if (users.containsKey(username) && users.getString(username).equals(password)) {
                    this.loggedInUsername = username;
                    shell.out().println("ok");
                } else {
                    shell.out().println("error wrong credentials");
                }
            } else {
                shell.out().println("error wrong number of arguments");
            }

        });
        shell.register("list", (input, context) -> {
            if (loggedInUsername == null) {
                shell.out().println("must be logged in");
                return;
            }
            if (input.getArguments().isEmpty()) {
                int i = 1;
                ConcurrentLinkedQueue<Email> usermails = this.database.get(loggedInUsername);
                if (usermails == null) {
                    shell.out().println("no mails for this users");
                } else {
                    usermails.forEach(email -> {
                        shell.out().println(i + " " + email.getFrom() + " " + email.getSubject());
                    });
                }

            } else {
                shell.out().println("error wrong number of arguments");
            }
        });
        shell.register("show", (input, context) -> {
            if (loggedInUsername == null) {
                shell.out().println("must be logged in");
                return;
            }

            if (input.getArguments().size() == 1 && Integer.parseInt(input.getArguments().get(0)) >= 1) {
                int indexToFind = Integer.parseInt(input.getArguments().get(0));
                int i = 1;
                ConcurrentLinkedQueue<Email> savedMailsForUser = this.database.get(loggedInUsername);
                if (savedMailsForUser == null) {
                    shell.out().println("error no mail found for given id");
                } else {
                    for (Email e : savedMailsForUser) {
                        if (i++ == indexToFind) {
                            shell.out().println(e.toString());
                            return;
                        }
                    }
                    shell.out().println("error no mail found for given id");
                }
            } else {
                shell.out().println("error wrong number of arguments");
            }
        });
        shell.register("delete", (input, context) -> {
            if (loggedInUsername == null) {
                shell.out().println("must be logged in");
                return;
            }
            if (input.getArguments().size() == 1 && Integer.parseInt(input.getArguments().get(0)) >= 1) {
                int indexToFind = Integer.parseInt(input.getArguments().get(0));
                int i = 1;
                ConcurrentLinkedQueue<Email> savedMailsForUser = this.database.get(loggedInUsername);
                if (savedMailsForUser == null) {
                    shell.out().println("error no mail found for given id");
                    return;
                } else {
                    for (Email e : savedMailsForUser) {
                        if (i++ == indexToFind) {
                            savedMailsForUser.remove(e);
                            shell.out().println("ok");
                            return;
                        }
                    }
                }
                shell.out().println("error unknown message id");
            } else {
                shell.out().println("error wrong number of arguments");
            }
        });
        shell.register("logout", (input, context) -> {
            if (loggedInUsername == null) {
                shell.out().println("error not logged in");
            } else {
                this.loggedInUsername = null;
                shell.out().println("ok");
            }
        });
        shell.register("quit", (input, context) -> {
            loggedInUsername = null;
            shell.out().println("ok bye");
            throw new StopShellException();
        });
    }
}
