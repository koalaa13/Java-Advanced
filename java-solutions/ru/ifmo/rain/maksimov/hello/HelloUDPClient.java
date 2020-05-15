package ru.ifmo.rain.maksimov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ru.ifmo.rain.maksimov.utils.ConcurrentUtils.closeExecutorService;
import static ru.ifmo.rain.maksimov.utils.HelloUtils.*;
import static ru.ifmo.rain.maksimov.utils.Helper.log;

public class HelloUDPClient implements HelloClient {
    private final static int TIMEOUT = 500;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            log("Unable to find host: " + host, e);
            return;
        }
        final SocketAddress dst = new InetSocketAddress(address, port);
        final ExecutorService workers = Executors.newFixedThreadPool(threads);
        for (int threadId = 0; threadId < threads; ++threadId) {
            final int finalThreadId = threadId;
            workers.submit(() -> sendAndReceive(dst, prefix, requests, finalThreadId));
        }
        closeExecutorService(workers, threads * requests, TimeUnit.MINUTES);
    }

    private static void sendAndReceive(final SocketAddress address, final String prefix, int cnt, final int threadId) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);
            final DatagramPacket respond = makeMessageToReceive(socket.getReceiveBufferSize());
            final DatagramPacket request = makeMessageToSend(address, 0);
            for (int requestId = 0; requestId < cnt; ++requestId) {
                final String requestText = getRequestText(prefix, threadId, requestId);
                while (!socket.isClosed() || Thread.currentThread().isInterrupted()) {
                    try {
                        setMessageText(request, requestText);
                        socket.send(request);
                        System.out.println("\nRequest sent:\n" + requestText);
                        socket.receive(respond);
                        final String respondText = getMessageText(respond);
                        if (checkRespond(threadId, requestId, respondText)) {
                            System.out.println("\nRespond received:\n" + respondText);
                            break;
                        }
                    } catch (IOException e) {
                        log("Error occurred while processing request", e);
                    }
                }
            }
        } catch (SocketException e) {
            log("Can not create socket to server: " + address, e);
        }
    }

    private static String getRequestText(final String prefix, final int threadId, final int requestId) {
        return prefix + threadId + '_' + requestId;
    }

    private static boolean checkRespond(final int threadId, final int requestId, final String response) {
        return response.matches("[\\D]*" + threadId + "[\\D]*" + requestId + "[\\D]*");
    }

    public static void main(String[] args) {

    }
}
