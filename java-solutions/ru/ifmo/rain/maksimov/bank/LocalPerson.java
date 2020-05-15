package ru.ifmo.rain.maksimov.bank;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LocalPerson implements Person, Serializable {
    private final int passportId;
    private final String firstName;
    private final String lastName;
    private final Map<String, LocalAccount> accounts;

    public LocalPerson(int passportId, String firstName, String lastName, Map<String, LocalAccount> accounts) {
        this.passportId = passportId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.accounts = accounts;
    }

    @Override
    public int getPassportId() {
        return passportId;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    public Set<String> getAccountsIds() {
        return accounts.keySet();
    }

    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }

    public synchronized Set<Account> getAccounts() {
        return getAccountsIds().stream().map(this::getAccount).collect(Collectors.toSet());
    }

    public void addAccount(String accountName, LocalAccount account) {
        accounts.put(accountName, account);
    }
}
