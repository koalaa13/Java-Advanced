package ru.ifmo.rain.maksimov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.ifmo.rain.maksimov.utils.ConcurrentUtils.closeExecutorService;
import static ru.ifmo.rain.maksimov.utils.HelloUtils.*;
import static ru.ifmo.rain.maksimov.utils.Helper.log;

/**
 * Implementation of {@link HelloServer}
 */
public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private boolean closed;
    private ExecutorService listener;
    private ExecutorService workers;
    private int buffSize;

    /**
     * Default constructor. In the start server is closed.
     */
    public HelloUDPServer() {
        socket = null;
        closed = true;
        listener = null;
        workers = null;
    }

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
            buffSize = socket.getReceiveBufferSize();
        } catch (SocketException e) {
            log("Can not create socket to port: " + port, e);
            return;
        }
        listener = Executors.newSingleThreadExecutor();
        workers = Executors.newFixedThreadPool(threads);
        closed = false;
        listener.submit(this::receiveAndResponse);
    }

    private void receiveAndResponse() {
        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            try {
                final DatagramPacket data = makeMessageToReceive(buffSize);
                socket.receive(data);
                workers.submit(() -> sendResponse(data));
            } catch (IOException e) {
                if (!closed) {
                    log("Error occurred while receiving data", e);
                }
            }
        }
    }

    private void sendResponse(final DatagramPacket packet) {
        final String message = getMessageText(packet);
        try {
            final DatagramPacket respond = makeAndSetMessageToSend("Hello, " + message, packet.getSocketAddress());
            socket.send(respond);
        } catch (IOException e) {
            if (!closed) {
                log("Error occurred while sending response.", e);
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        socket.close();
        closeExecutorService(listener);
        closeExecutorService(workers);
    }

    /**
     * Start a server with given arguments.
     * If some arguments are wrong write a error message with {@link ru.ifmo.rain.maksimov.utils.Helper#log(String, Exception)}.
     *
     * @param args arguments to start server with.
     */
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            log("Usage: port threadCount");
            return;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            log("Arguments can not be null");
            return;
        }
        int port, threads;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            log("First argument (port) should be a number");
            return;
        }
        try {
            threads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            log("Second argument (threadCount) should be a number");
            return;
        }
        new HelloUDPServer().start(port, threads);
    }
}
