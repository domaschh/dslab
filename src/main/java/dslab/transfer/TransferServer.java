package dslab.transfer;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import dslab.ComponentFactory;
import dslab.data.Email;
import dslab.protocols.TransferProtocol;
import dslab.util.Config;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransferServer implements ITransferServer, Runnable {
    static int CORE_COUNT = Runtime.getRuntime().availableProcessors();

    private final String componentId;
    private final List<Socket> sockets;
    private final Shell shell;
    private final ServerSocket serverSocket;
    private final Config domains;
    private final Config config;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config      the component config
     * @param in          the input stream to read console input from
     * @param out         the output stream to write console output to
     */
    public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.sockets = new ArrayList<>();
        this.domains = new Config("domains");
        this.config = config;
        this.shell = new Shell(in, out);
        this.shell.register("shutdown", (input, context) -> this.shutdown());

        try {
            serverSocket = new ServerSocket(config.getInt("tcp.port"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        ExecutorService receiveExecutor = Executors.newFixedThreadPool(CORE_COUNT / 2);
        ExecutorService sendExecutor = Executors.newFixedThreadPool(CORE_COUNT / 2);
        receiveExecutor.execute(shell);
        try {
            Socket socket = serverSocket.accept();
            while (socket.isConnected()) {
                sockets.add(socket);
                receiveExecutor.execute(new TransferProtocol(componentId, socket, config, (Email completeMail) -> {
                    sendEmails(completeMail, sendExecutor);
                }));
                socket = serverSocket.accept();
            }
        } catch (IOException e) {
            shell.out().println("Sending mail failed");
        }
    }

    private void sendEmails(Email completeMail, ExecutorService sendExecutor) {
        for (String rcvrMail : completeMail.getTo()) {
            String domain = rcvrMail.split("@")[1];
            if (this.domains.containsKey(domain)) {//DOMAIN FOUND
                String[] split = this.domains.getString(domain).split(":");
                String address = split[0];
                int port = Integer.parseInt(split[1]);
                sendExecutor.execute(() -> {
                    try {
                        Socket s = new Socket(address, port);
                        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                        this.sendEmailToMailboxServer(completeMail, in, out);
                        shell.out().println("Mail sent");
                        s.close();
                    } catch (IOException e) {
                        shell.out().println("Sending mail failed");
                        throw new RuntimeException(e);
                    }
                });
                this.sendMailToMonitoringServer(completeMail);
            } else { //DOMAIN NOT FOUND
                Email domainNotFoundMail = new Email("mailer@127.0.0.1", List.of(completeMail.getFrom()), "mail transfer failed", completeMail.toString().replace("\n", ", "));
                sendExecutor.execute(() -> {
                    try {
                        Socket s = new Socket("127.0.0.1", config.getInt("tcp.port"));
                        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                        this.sendEmailToMailboxServer(domainNotFoundMail, in, out);
                        shell.out().println("Mail sent");
                        s.close();
                    } catch (IOException e) {
                        shell.out().println("Sending mail failed");
                        throw new RuntimeException(e);
                    }
                });
                this.sendMailToMonitoringServer(domainNotFoundMail);
            }
        }
    }

    private void sendMailToMonitoringServer(Email completeMail) {
        byte[] buffer = ("127.0.0.1:" + config.getString("tcp.port") + " " + completeMail.getFrom()).getBytes();
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.send(new DatagramPacket(buffer, buffer.length, InetAddress.getByName(config.getString("monitoring.host")), config.getInt("monitoring.port")));
            socket.close();
        } catch (IOException e) {
            shell.out().println(e.getMessage());
        }
    }

    private void sendEmailToMailboxServer(Email completeMail, BufferedReader in, PrintWriter out) throws IOException {
        String s = in.readLine();
        if (!s.equals("ok DMTP")) {
            this.shell.out().println("Transfer failed");
            throw new IOException("Transfer failed connecting expected: ok DMTP but got: " + s);
        }
        out.println("begin");
        s = in.readLine();
        if (!s.equals("ok")) {
            this.shell.out().println("Transfer failed" + s);
            throw new IOException("Transfer failed connecting expected: ok but got: " + s);
        }
        out.println("from " + completeMail.getFrom());
        s = in.readLine();
        if (!s.equals("ok")) {
            this.shell.out().println("Transfer failed" + s);
            throw new IOException("Transfer failed connecting expected: ok but got: " + s);
        }
        out.println("to " + String.join(",", completeMail.getTo()));
        s = in.readLine();
        if (!s.contains("ok")) {
            this.shell.out().println("Transfer failed" + s);
            throw new IOException("Transfer failed connecting expected: ok but got: " + s);
        }
        out.println("subject " + completeMail.getSubject());
        s = in.readLine();
        if (!s.equals("ok")) {
            this.shell.out().println("Transfer failed" + s);
            throw new IOException("Transfer failed connecting expected: ok but got: " + s);
        }
        out.println("data " + completeMail.getBody());
        s = in.readLine();
        if (!s.equals("ok")) {
            this.shell.out().println("Transfer failed" + s);
            throw new IOException("Transfer failed connecting expected: ok but got: " + s);
        }
        out.println("send ");
        s = in.readLine();
        if (!s.equals("ok")) {
            this.shell.out().println("Transfer failed" + s);
            throw new IOException("Transfer failed connecting expected: ok but got: " + s);
        }
    }

    @Override
    public void shutdown() {
        ArrayList<String> exceptions = new ArrayList<>();
        try {
            serverSocket.close();
            for (Socket socket : sockets) {
                socket.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(new IOException(String.join(" ", exceptions)));
        }

        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }
}
