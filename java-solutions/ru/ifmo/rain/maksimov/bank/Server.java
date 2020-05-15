package ru.ifmo.rain.maksimov.bank;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import static ru.ifmo.rain.maksimov.utils.BankUtils.logAction;
import static ru.ifmo.rain.maksimov.utils.Helper.log;

public class Server {
    private final static int PORT = 1337;

    public static void main(String[] args) {
        Bank bank;
        try {
            bank = new RemoteBank(PORT);
            Bank stub = (Bank) UnicastRemoteObject.exportObject(bank, 0);

            Registry registry = LocateRegistry.createRegistry(1488);
            registry.rebind("bank", stub);
        } catch (RemoteException e) {
            log("Cannot export object", e);
            return;
        }
        logAction("Server started successfully");
    }
}
