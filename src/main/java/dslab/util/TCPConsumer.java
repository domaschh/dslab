package dslab.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPConsumer extends Thread {
    static int POOL_SIZE = 50 * Runtime.getRuntime().availableProcessors();
    private ServerSocket socket;
    private Config domains;
    private final Config config;

    public TCPConsumer(ServerSocket serverSocket, Config domains, Config config) {
        this.socket = serverSocket;
        this.domains = domains;
        this.config = config;
    }

    @Override
    public void run() {
        //Main Executor pool
        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE * 7/10);
        //Mail-forwarding executor
        ExecutorService forwardingExecutor = Executors.newFixedThreadPool(POOL_SIZE * 3/10);

        try {
            while (true) {
                Socket socket = this.socket.accept();
                var inputStream = socket.getInputStream();
                var outputStream = socket.getOutputStream();

                Runnable worker = new ResponseTask(inputStream, outputStream, domains, config, forwardingExecutor);
                executor.execute(worker);
            }
        } catch (SocketException e) {
            System.out.println("SocketException from TransferListenerThread");
        } catch (IOException e) {
            System.out.println("Cannot handle IO Exception from TransferListenerThread");
        } finally {
            executor.shutdownNow();
            forwardingExecutor.shutdownNow();
        }
    }
}
