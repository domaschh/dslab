package dslab.util;

import dslab.data.Email;
import dslab.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class ForwardTask implements Runnable{

    private String address;
    private Email mail;
    private final ExecutorService forwarderPool;
    private final Config config;

    public ForwardTask(String address, Email mail, ExecutorService forwarderPool, Config config) {
        this.address = address;
        this.mail = mail;
        this.forwarderPool = forwarderPool;
        this.config = config;
    }

    @Override
    public void run() {
        String[] split = this.address.split(":");
        String ip = split[0];
        Integer port = Integer.parseInt(split[1]);

        try {
            Socket socket = new Socket(ip, port);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream());


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
