package ru.ifmo.rain.maksimov.utils;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class HelloUtils {
    public static String getMessageText(final DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public static void setMessageText(final DatagramPacket packet, final String message) {
        packet.setData(message.getBytes(StandardCharsets.UTF_8));
    }

    public static DatagramPacket makeMessageToSend(final SocketAddress dst, final int buffSize) {
        final byte[] buff = new byte[buffSize];
        return new DatagramPacket(buff, buffSize, dst);
    }

    public static DatagramPacket makeMessageToReceive(final int buffSize) {
        final byte[] buff = new byte[buffSize];
        return new DatagramPacket(buff, buffSize);
    }

    public static DatagramPacket makeAndSetMessageToSend(final String message, final SocketAddress dst) {
        DatagramPacket result = makeMessageToSend(dst, 0);
        setMessageText(result, message);
        return result;
    }
}
