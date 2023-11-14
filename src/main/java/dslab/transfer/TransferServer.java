package dslab.transfer;

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
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.protocols.TransferProtocol;
import dslab.util.Config;

public class TransferServer implements ITransferServer, Runnable {
    static int POOL_SIZE = 50 * Runtime.getRuntime().availableProcessors();

    private final String componentId;
    private final List<Socket> sockets;
    private final Shell shell;
    private final ServerSocket serverSocket;
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
        this.config = config;
        shell = new Shell(in, out);
        shell.setPrompt(componentId + "> ");
        shell.register("shutdown", (input, context) -> this.shutdown());

        try {
            serverSocket = new ServerSocket(config.getInt("tcp.port"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
        executor.execute(shell);
        try {
            Socket socket = serverSocket.accept();
            while (socket.isConnected()) {
                sockets.add(socket);
                executor.execute(new TransferProtocol(componentId, socket, config));
                socket = serverSocket.accept();
            }
        } catch (IOException e) {
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
