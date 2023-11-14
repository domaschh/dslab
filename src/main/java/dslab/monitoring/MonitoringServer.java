package dslab.monitoring;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;
import dslab.util.MonitoringConsumer;

public class MonitoringServer implements IMonitoringServer {
    private final Config config;
    private final Shell shell;
    private final Map<String, Integer> serverHostAndPort;
    private final Map<String, Integer> senderEmail;
    private DatagramSocket socket;
    private MonitoringConsumer consumer;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config      the component config
     * @param in          the input stream to read console input from
     * @param out         the output stream to write console output to
     */
    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt("");

        //Are passed to filled by the consumer
        this.serverHostAndPort = new HashMap<>();
        this.senderEmail = new HashMap<>();
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(config.getInt("udp.port"));
            consumer = new MonitoringConsumer(shell, socket, serverHostAndPort, senderEmail);
            consumer.start();
        } catch (IOException e) {
            throw new RuntimeException("Cannot listen on UDP port.", e);
        }

        shell.run();
    }

    @Override
    @Command
    public void addresses() {
        if (senderEmail.isEmpty()) {
            shell.out().println("no addresses records yet");
        } else {
            for (Map.Entry<String, Integer> entry : this.senderEmail.entrySet()) {
                shell.out().println(entry.getKey() + " " + entry.getValue());
            }
        }
    }

    @Override
    @Command
    public void servers() {
        if (serverHostAndPort.isEmpty()) {
            shell.out().println("no server records yet");
        } else {
            for (Map.Entry<String, Integer> entry : this.serverHostAndPort.entrySet()) {
                shell.out().println(entry.getKey() + " " + entry.getValue());
            }
        }
    }

    @Override
    @Command
    public void shutdown() {
        consumer.stopConsumer();
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

}
