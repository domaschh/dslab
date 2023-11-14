package dslab.mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import dslab.ComponentFactory;
import dslab.data.Email;
import dslab.protocols.AuthProtocol;
import dslab.protocols.TransferProtocol;
import dslab.util.Config;

public class MailboxServer implements IMailboxServer, Runnable {
    static int POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private final String componentId;
    private final List<Socket> sockets;
    private final Shell shell;
    private final Config config;
    private final ServerSocket transferSocket;
    private final ServerSocket authSocket;
    private final ConcurrentMap<String, ConcurrentSkipListSet<Email>> database = new ConcurrentHashMap<>();

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.sockets = new ArrayList<>();
        this.config = config;
        this.shell = new Shell(in, out);
        this.shell.setPrompt(componentId + "> ");
        this.shell.register("shutdown", (input, context) -> this.shutdown());
        try {
            transferSocket = new ServerSocket(config.getInt("dmtp.tcp.port"));
            authSocket = new ServerSocket(config.getInt("dmap.tcp.port"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
        new Thread(shell).start();
        this.startAthListening(executor);
        this.startTransferListening(executor);
    }

    private void startTransferListening(ExecutorService executor) {
        executor.execute(() -> {
            try {
                Socket socket = authSocket.accept();
                while (socket.isConnected()) {
                    sockets.add(socket);
                    executor.execute(new AuthProtocol(componentId, socket, config, database));
                    socket = authSocket.accept();
                }
            } catch (IOException e) {
            }
        });
    }

    private void startAthListening(ExecutorService executor) {
        executor.execute(() -> {
            try {
                Socket socket = transferSocket.accept();
                while (socket.isConnected()) {
                    sockets.add(socket);
                    executor.execute(new TransferProtocol(componentId, socket, config));
                    socket = transferSocket.accept();
                }
            } catch (IOException e) {
            }
        });
    }

    @Override
    public void shutdown() {
        ArrayList<String> exceptions = new ArrayList<>();
        try {
            transferSocket.close();
            authSocket.close();
            for (Socket socket : sockets) {
                socket.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(new IOException(String.join(" ", exceptions)));
        }

        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run();
    }
}
