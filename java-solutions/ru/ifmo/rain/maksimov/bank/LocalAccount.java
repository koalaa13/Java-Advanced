package ru.ifmo.rain.maksimov.bank;

import java.io.Serializable;

import static ru.ifmo.rain.maksimov.utils.BankUtils.logAction;

/**
 * Serializable implementation of {@link Account}
 */
public class LocalAccount implements Account, Serializable {
    private final String id;
    private int amount;

    /**
     * Constructor.
     *
     * @param id     id to set.
     * @param amount amount of money to set.
     */
    public LocalAccount(String id, int amount) {
        this.id = id;
        this.amount = amount;
    }

    /**
     * Constructor. Amount of money will be set to 0.
     *
     * @param id id to set.
     */
    public LocalAccount(String id) {
        this(id, 0);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getAmount() {
        logAction("Getting money for account: " + id);
        return amount;
    }

    @Override
    public synchronized void setAmount(int amount) {
        logAction("Setting money for account: " + id);
        this.amount = amount;
    }

    @Override
    public synchronized void addAmount(int amount) {
        logAction("Adding money for account: " + id);
        this.amount += amount;
    }
}
