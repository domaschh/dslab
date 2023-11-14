package dslab.util;

import at.ac.tuwien.dsg.orvell.Shell;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;

public class MonitoringConsumer extends Thread implements Runnable {
    //<host>:<port> <email-address>
    public static final int MESSAGE_RCVD_ARG_LEN = 2;
    private final DatagramSocket socket;
    private final Shell shell;
    private final Map<String, Integer> serverHostAndPort;
    private final Map<String, Integer> senderEmail;
    private boolean running = true;

    public MonitoringConsumer(Shell shell, DatagramSocket socket, Map<String, Integer> serverHostAndPort, Map<String, Integer> senderEmail) {
        this.shell = shell;
        this.socket = socket;
        this.serverHostAndPort = serverHostAndPort;
        this.senderEmail = senderEmail;
    }

    @Override
    public void run() {
        byte[] buffer= new byte[1024];
        DatagramPacket packet= new DatagramPacket(buffer, buffer.length);

        try {
             while (running) {
                socket.receive(packet);
                String request = new String(packet.getData());

                String[] parts = request.split(" ");
                if (parts.length != MESSAGE_RCVD_ARG_LEN) {
                    shell.err().println("Incorrect input");
                    continue;
                }

                this.serverHostAndPort.merge(parts[0], 1, Integer::sum);
                this.senderEmail.merge(parts[1].trim(), 1, Integer::sum);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    public void stopConsumer() {
        if (this.socket != null) {
            this.socket.close();
            System.out.println("Socket closed");
        }
        this.running = false;
    }

}
