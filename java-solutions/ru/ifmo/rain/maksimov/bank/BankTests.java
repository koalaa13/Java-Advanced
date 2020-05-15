package ru.ifmo.rain.maksimov.bank;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class BankTests {
    private final static int PORT = 3333;
    private static Registry registry;
    private static Bank bank;
    private final String firstName = "Kek";
    private final String lastName = "Kekov";
    private final int passportId = 1337;
    private final String correctAccountId = "kek";

    @BeforeClass
    public static void createRegistry() throws RemoteException {
        registry = LocateRegistry.createRegistry(PORT);
    }

    @Before
    public void createRemoteBank() throws RemoteException {
        bank = new RemoteBank(PORT);
        Bank stub = (Bank) UnicastRemoteObject.exportObject(bank, 0);
        registry.rebind("bank", stub);
    }

    @Test
    public void createOneRemotePerson() throws RemoteException {
        assertTrue(bank.createPerson(passportId, firstName, lastName));
        Person person = bank.getRemotePerson(passportId);
        assertEquals(passportId, person.getPassportId());
        assertEquals(firstName, person.getFirstName());
        assertEquals(lastName, person.getLastName());
    }

    @Test
    public void createExistingPerson() throws RemoteException {
        assertTrue(bank.createPerson(passportId, firstName, lastName));
        assertFalse(bank.createPerson(passportId, firstName, lastName));
        Person person = bank.getRemotePerson(passportId);
        assertEquals(passportId, person.getPassportId());
        assertEquals(firstName, person.getFirstName());
        assertEquals(lastName, person.getLastName());
    }

    @Test
    public void createNonValidPerson() throws RemoteException {
        assertFalse(bank.createPerson(-1, firstName, lastName));
        assertFalse(bank.createPerson(passportId, null, lastName));
        assertFalse(bank.createPerson(passportId, firstName, null));
        assertFalse(bank.createPerson(passportId, "", ""));
        Person person = bank.getRemotePerson(-1);
        assertNull(person);
    }

    @Test
    public void localPersonTest() throws RemoteException {
        LocalPerson person = (LocalPerson) bank.createAndGetLocalPerson(passportId, firstName, lastName);
        assertEquals(passportId, person.getPassportId());
        assertEquals(firstName, person.getFirstName());
        assertEquals(lastName, person.getLastName());
        assertEquals(Collections.EMPTY_SET, person.getAccountsIds());

        String localPersonAccountId = "localPersonAccountId";
        LocalAccount localAccount = new LocalAccount(localPersonAccountId);
        person.addAccount(localPersonAccountId, localAccount);
        person.addAccount(localPersonAccountId, localAccount);
        final String anotherLocalPersonAccountId = localPersonAccountId + "Kek";
        LocalAccount anotherLocalAccount = new LocalAccount(anotherLocalPersonAccountId);
        person.addAccount(anotherLocalPersonAccountId, anotherLocalAccount);
        assertEquals(2, person.getAccountsIds().size());
        assertTrue(person.getAccountsIds().contains(localAccount.getId()));
        assertTrue(person.getAccountsIds().contains(anotherLocalAccount.getId()));

        final Account account = person.getAccount(localPersonAccountId);
        assertEquals(localAccount.getId(), account.getId());
        assertEquals(localAccount.getAmount(), account.getAmount());

        final Account anotherAccount = person.getAccount(anotherLocalPersonAccountId);
        assertEquals(anotherAccount.getId(), anotherLocalAccount.getId());
        assertEquals(anotherAccount.getAmount(), anotherLocalAccount.getAmount());
    }

    @Test
    public void getNonexistentLocalPerson() throws RemoteException {
        Person person = bank.getLocalPerson(-1);
        assertNull(person);
    }

    @Test
    public void createAndGetRemotePerson() throws RemoteException {
        Person person = bank.createAndGetRemotePerson(passportId, firstName, lastName);
        assertEquals(passportId, person.getPassportId());
        assertEquals(firstName, person.getFirstName());
        assertEquals(lastName, person.getLastName());
    }

    @Test
    public void getLocalPersonWithExistingAccounts() throws RemoteException {
        Person person = bank.createAndGetRemotePerson(passportId, firstName, lastName);

        final String emptyAccId = "emptyAcc";
        final String nonEmptyAccId = "nonEmptyAcc";

        bank.createAccount(person, emptyAccId);
        Account nonEmptyAccount = bank.getAccount(person, nonEmptyAccId);
        nonEmptyAccount.setAmount(222);

        LocalPerson localPerson = (LocalPerson) bank.getLocalPerson(passportId);
        assertEquals(2, localPerson.getAccountsIds().size());
        Set<String> accounts = localPerson.getAccountsIds();
        assertTrue("Should have to accounts on person with ids: " + emptyAccId + " and " + nonEmptyAccId, accounts.containsAll(List.of(emptyAccId, nonEmptyAccId)));
    }

    @Test
    public void createAccountWithNonValidData() throws RemoteException {
        assertFalse(bank.createAccount(new RemotePerson(-1, firstName, lastName), correctAccountId));
        assertFalse(bank.createAccount(new RemotePerson(passportId, firstName, lastName), ""));
        assertFalse(bank.createAccount(new RemotePerson(passportId, firstName, lastName), null));
        assertFalse(bank.createAccount(new RemotePerson(passportId, "", ""), correctAccountId));
    }

    @Test
    public void createExistingAccount() throws RemoteException {
        Person person = bank.createAndGetRemotePerson(passportId, firstName, lastName);
        assertTrue(bank.createAccount(person, correctAccountId));
        assertFalse(bank.createAccount(person, correctAccountId));
    }

    @Test
    public void createAccountForLocalPerson() throws RemoteException {
        LocalPerson person = (LocalPerson) bank.createAndGetLocalPerson(passportId, firstName, lastName);
        bank.createAccount(person, correctAccountId);
        assertEquals(1, person.getAccounts().size());
        assertEquals(correctAccountId, person.getAccountsIds().iterator().next());
    }

    @Test
    public void getAccountWithNonValidData() throws RemoteException {
        assertNull(bank.getAccount(new RemotePerson(-1, firstName, lastName), correctAccountId));
        assertNull(bank.getAccount(new RemotePerson(passportId, firstName, lastName), ""));
        assertNull(bank.getAccount(new RemotePerson(passportId, firstName, lastName), null));
        assertNull(bank.getAccount(new RemotePerson(passportId, "", ""), correctAccountId));
    }

    @Test
    public void getAccountFromLocalPerson() throws RemoteException {
        LocalPerson person = (LocalPerson) bank.createAndGetLocalPerson(passportId, firstName, lastName);
        bank.createAccount(person, correctAccountId);
        assertSame(person.getAccount(correctAccountId), bank.getAccount(person, correctAccountId));
    }

    @Test
    public void containsPerson() throws RemoteException {
        bank.createPerson(passportId, firstName, lastName);

        assertFalse(bank.containsPerson(-1, null, null));
        assertFalse(bank.containsPerson(passportId + 1, firstName, lastName));
        assertFalse(bank.containsPerson(passportId, "", ""));
        assertFalse(bank.containsPerson(passportId, firstName, lastName + '!'));
        assertTrue(bank.containsPerson(passportId, firstName, lastName));
    }

    @Test
    public void getAccounts() throws RemoteException {
        assertNull(bank.getAccountsByPerson(null));

        LocalPerson localPerson = (LocalPerson) bank.createAndGetLocalPerson(passportId + 1, firstName + '!', lastName + '!');

        bank.createAccount(localPerson, correctAccountId);
        assertSame(localPerson.getAccountsIds(), bank.getAccountsByPerson(localPerson));
    }

    @Test
    public void createManyAccounts() throws RemoteException {
        final int accountsCount = 10;
        Person person = bank.createAndGetRemotePerson(passportId, firstName, lastName);
        for (int i = 0; i < accountsCount; ++i) {
            bank.createAccount(person, correctAccountId + i);
        }
        Set<String> accounts = bank.getAccountsByPerson(person);
        assertEquals(10, accounts.size());
        for (int i = 0; i < accountsCount; ++i) {
            assertTrue(accounts.contains(correctAccountId + i));
        }
    }

    @Test
    public void createManyAccountsSimultaneously() throws RemoteException, InterruptedException {
        Person person = bank.createAndGetRemotePerson(passportId, firstName, lastName);

        final int countThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(countThreads);
        CountDownLatch done = new CountDownLatch(countThreads);
        for (int i = 0; i < countThreads; ++i) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    bank.createAccount(person, correctAccountId + finalI);
                } catch (RemoteException ignored) {
                }
                done.countDown();
            });
        }

        done.await();
        Set<String> accounts = bank.getAccountsByPerson(person);
        assertEquals(countThreads, accounts.size());

        for (int i = 0; i < countThreads; ++i) {
            assertTrue(accounts.contains(correctAccountId + i));
        }
    }

    @Test
    public void changeMoneySimultaneously_LocalAccount() throws InterruptedException, RemoteException {
        LocalAccount account = new LocalAccount("LocalAccount");
        checkChangeMoneyOnAccountSimultaneously(account);
    }

    private void checkChangeMoneyOnAccountSimultaneously(Account account) throws InterruptedException, RemoteException {
        final int countThreads = 10;
        final int startMoney = 10;

        account.setAmount(startMoney);
        assertEquals(startMoney, account.getAmount());

        ExecutorService executorService = Executors.newFixedThreadPool(countThreads);
        CountDownLatch done = new CountDownLatch(countThreads);
        for (int i = 0; i < countThreads; ++i) {
            executorService.submit(() -> {
                try {
                    account.addAmount(10);
                } catch (RemoteException ignored) {
                }
                done.countDown();
            });
        }

        done.await();
        assertEquals(startMoney + 10 * countThreads, account.getAmount());
    }

    @Test
    public void changeMoneySimultaneously_RemoteAccount() throws RemoteException, InterruptedException {
        RemoteAccount account = new RemoteAccount("RemoteAccount");
        checkChangeMoneyOnAccountSimultaneously(account);
    }

    @Test
    public void localAccountNoChanges() throws RemoteException {
        LocalPerson localPerson = (LocalPerson) bank.createAndGetLocalPerson(passportId, firstName, lastName);
        bank.createAccount(localPerson, correctAccountId);

        LocalAccount localAccount = (LocalAccount) bank.getAccount(localPerson, correctAccountId);
        localAccount.setAmount(10);

        RemotePerson remotePerson = (RemotePerson) bank.getRemotePerson(passportId);
        assertEquals(localPerson.getPassportId(), remotePerson.getPassportId());
        assertEquals(localPerson.getFirstName(), remotePerson.getFirstName());
        assertEquals(localPerson.getLastName(), remotePerson.getLastName());

        RemoteAccount remoteAccount = (RemoteAccount) bank.getAccount(remotePerson, correctAccountId);
        remoteAccount.setAmount(20);

        assertEquals(10, localAccount.getAmount());
        assertEquals(20, remoteAccount.getAmount());

        LocalPerson localPersonCopy = (LocalPerson) bank.getLocalPerson(passportId);
        assertEquals(20, bank.getAccount(localPersonCopy, correctAccountId).getAmount());
    }

    private void runRaw(Method method, final String... args) {
        try {
            method.invoke(null, (Object) args);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Can not call method main(String[])", e);
        } catch (InvocationTargetException e) {
            throw new AssertionError("Error thrown,", e.getCause());
        }
    }

    @Test
    public void clientMain() {
        Class<?> cls;
        try {
            cls = Class.forName("ru.ifmo.rain.maksimov.bank.Client");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Can not find Client class");
        }
        Method main;
        try {
            main = cls.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Can not find main(String[]) method in Client class");
        }

        runRaw(main);
        runRaw(main, (String) null);

        final String[] args = new String[5];
        Arrays.stream(args).forEach(arg -> arg = null);
        runRaw(main, args);

        args[0] = args[1] = args[3] = args[4] = "kek";
        args[2] = "abacaba";
        runRaw(main, args);

        args[2] = "2222";
        runRaw(main, args);

        args[2] = "9999999999999999999999999999999999999999999999999";
        runRaw(main, args);

        args[2] = args[4] = "2222";
        runRaw(main, args);
    }
}
