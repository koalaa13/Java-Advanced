package ru.ifmo.rain.maksimov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for remote bank account.
 */
public interface Account extends Remote {
    /**
     * Get id of an account.
     *
     * @return Id of an account.
     * @throws RemoteException see {@link java.rmi.Remote}.
     */
    String getId() throws RemoteException;

    /**
     * Get amount of money on account.
     *
     * @return Amount of money on account.
     * @throws RemoteException see {@link java.rmi.Remote}.
     */
    int getAmount() throws RemoteException;

    /**
     * Set amount of money on account.
     *
     * @throws RemoteException see {@link java.rmi.Remote}.
     */
    void setAmount(int amount) throws RemoteException;

    void addAmount(int amount) throws RemoteException;
}
