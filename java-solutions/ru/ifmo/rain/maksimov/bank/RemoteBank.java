package ru.ifmo.rain.maksimov.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static ru.ifmo.rain.maksimov.utils.BankUtils.*;
import static ru.ifmo.rain.maksimov.utils.Helper.log;

public class RemoteBank implements Bank {
    private final int port;
    /**
     * Map from accountId to an account.
     */
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    /**
     * Map from passportId to a person.
     */
    private final Map<Integer, Person> people = new ConcurrentHashMap<>();
    /**
     * Map from passportId to Set of accountId's
     */
    private final Map<Integer, Set<String>> accountsByPassportId = new ConcurrentHashMap<>();


    public RemoteBank(int port) {
        this.port = port;
    }

    @Override
    public boolean createAccount(Person person, String id) throws RemoteException {
        if (validateAccount(person, id)) {
            return false;
        }
        String accountId = getAccountId(person.getPassportId(), id);
        synchronized (accounts) {
            if (accounts.get(accountId) != null) {
                return false;
            }
            logAction("Creating account: " + accountId);
            Account account = new RemoteAccount(id);
            accounts.put(accountId, account);
            UnicastRemoteObject.exportObject(account, port);
        }
        accountsByPassportId.get(person.getPassportId()).add(id);

        if (person instanceof LocalPerson) {
            ((LocalPerson) person).addAccount(id, new LocalAccount(id));
        }

        return true;
    }

    @Override
    public synchronized Person createAndGetRemotePerson(int passportId, String firstName, String lastName) throws RemoteException {
        createPerson(passportId, firstName, lastName);
        return getRemotePerson(passportId);
    }

    @Override
    public synchronized Person createAndGetLocalPerson(int passportId, String firstName, String lastName) throws RemoteException {
        createPerson(passportId, firstName, lastName);
        return getLocalPerson(passportId);
    }

    @Override
    public Account getAccount(Person person, String id) throws RemoteException {
        if (validateAccount(person, id)) {
            return null;
        }

        String accountId = getAccountId(person.getPassportId(), id);
        Account account = accounts.get(accountId);

        if (account == null) {
            createAccount(person, id);
            account = accounts.get(accountId);
        }

        logAction("Getting account: " + accountId);

        if (person instanceof LocalPerson) {
            return ((LocalPerson) person).getAccount(id);
        }

        return account;
    }

    @Override
    public boolean createPerson(int passportId, String firstName, String lastName) throws RemoteException {
        if (validatePerson(passportId, firstName, lastName)) {
            return false;
        }

        Person person = people.get(passportId);
        if (person != null) {
            return false;
        }

        logAction("Creating person " + firstName + ' ' + lastName + " with passportId " + passportId);
        person = new RemotePerson(passportId, firstName, lastName);
        people.put(passportId, person);
        accountsByPassportId.put(passportId, new ConcurrentSkipListSet<>());
        UnicastRemoteObject.exportObject(person, port);
        return true;
    }

    @Override
    public boolean containsPerson(int passportId, String firstName, String lastName) throws RemoteException {
        if (validatePerson(passportId, firstName, lastName)) {
            return false;
        }
        logAction("Checking for availability " + firstName + ' ' + lastName + " with passportId " + passportId);
        Person person = people.get(passportId);
        return person != null && firstName.equals(person.getFirstName()) && lastName.equals(person.getLastName());
    }

    @Override
    public Person getLocalPerson(int passportId) throws RemoteException {
        Person person = people.get(passportId);
        if (person == null) {
            return null;
        }

        logAction("Getting local person with passportId " + passportId);
        Map<String, LocalAccount> localAccount = new ConcurrentHashMap<>();

        getAccountsByPerson(person).forEach(accId -> {
            try {
                Account account = getAccount(person, accId);
                localAccount.put(accId, new LocalAccount(accId, account.getAmount()));
            } catch (RemoteException e) {
                log("Error occurred while creating a local accounts");
            }
        });

        return new LocalPerson(passportId, person.getFirstName(), person.getLastName(), localAccount);
    }

    @Override
    public Person getRemotePerson(int passportId) {
        if (passportId < 0) {
            return null;
        }

        logAction("Getting remote person with passportId " + passportId);
        return people.get(passportId);
    }

    @Override
    public Set<String> getAccountsByPerson(Person person) throws RemoteException {
        if (person == null) {
            return null;
        }
        int passportId = person.getPassportId();
        logAction("Getting accounts for person with passportId " + passportId);
        if (person instanceof LocalPerson) {
            return ((LocalPerson) person).getAccountsIds();
        }
        return accountsByPassportId.get(passportId);
    }
}
