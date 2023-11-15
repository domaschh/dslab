package dslab.monitoring;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import dslab.ComponentFactory;
import dslab.util.Config;
import dslab.util.MonitoringConsumer;

public class MonitoringServer implements IMonitoringServer {
    private final Config config;
    private final Shell serverShell;
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
        serverShell = new Shell(in, out);
        serverShell.setPrompt("");

        //Are passed to filled by the consumer
        this.serverHostAndPort = new HashMap<>();
        this.senderEmail = new HashMap<>();

        this.setupShellCallbacks();

    }

    private void setupShellCallbacks() {
        serverShell.register("addresses", (input, context) -> {
            this.addresses();
        });
        serverShell.register("servers", (input, context) -> {
            this.servers();
        });
        serverShell.register("shutdown", (input, context) -> {
            this.shutdown();
        });
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(config.getInt("udp.port"));
            consumer = new MonitoringConsumer(serverShell, socket, serverHostAndPort, senderEmail);
            consumer.start();
        } catch (IOException e) {
            throw new RuntimeException("Cannot listen on UDP port.", e);
        }

        serverShell.run();
    }

    @Override
    public void addresses() {
        if (senderEmail.isEmpty()) {
            serverShell.out().println("no addresses records yet");
        } else {
            for (Map.Entry<String, Integer> entry : this.senderEmail.entrySet()) {
                serverShell.out().println(entry.getKey() + " " + entry.getValue());
            }
        }
    }

    @Override
    public void servers() {
        if (serverHostAndPort.isEmpty()) {
            serverShell.out().println("no server records yet");
        } else {
            for (Map.Entry<String, Integer> entry : this.serverHostAndPort.entrySet()) {
                serverShell.out().println(entry.getKey() + " " + entry.getValue());
            }
        }
    }

    @Override
    public void shutdown() {
        consumer.stopConsumer();
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

}
