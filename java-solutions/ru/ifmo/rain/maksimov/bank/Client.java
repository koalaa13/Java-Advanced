package ru.ifmo.rain.maksimov.bank;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Objects;

import static ru.ifmo.rain.maksimov.utils.BankUtils.logAction;
import static ru.ifmo.rain.maksimov.utils.Helper.log;

public class Client {

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            log("Expected 5 arguments: <first name> <last name> <passport id> <account id> <change>");
            return;
        }

        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            log("Expected non-null arguments");
            return;
        }

        final Bank bank;
        String firstName = args[0], lastName = args[1], accountId = args[3];
        int passportId, change;

        try {
            passportId = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            log("Passport id should be a number", e);
            return;
        }

        try {
            change = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            log("Change should be a number", e);
            return;
        }

        try {
            Registry registry = LocateRegistry.getRegistry(1488);
            bank = (Bank) registry.lookup("bank");
        } catch (RemoteException | NotBoundException e) {
            log("Cannot find remote bank", e);
            return;
        }

        Person person;
        try {
            person = bank.getRemotePerson(passportId);
            if (person == null) {
                logAction("Creating new person with first name " + firstName + " last name " + lastName + " passport id " + passportId);
                person = bank.createAndGetRemotePerson(passportId, firstName, lastName);
            }

            if (!bank.containsPerson(passportId, firstName, lastName)) {
                logAction("Incorrect person data");
                return;
            }
        } catch (RemoteException e) {
            log("Error occurred while getting person with passport id " + passportId + " from remote bank", e);
            return;
        }

        Account account;
        try {
            account = bank.getAccount(person, accountId);
            int accountMoneyBeforeChange = account.getAmount();
            logAction("Account: " + account.getId());
            logAction("Money: " + accountMoneyBeforeChange);
            logAction("Changing amount...");
            account.addAmount(change);
            logAction("Changed amount successfully");
            logAction("Money after change: " + account.getAmount());
        } catch (RemoteException e) {
            log("Error occurred while getting account with id " + accountId + "from remote bank", e);
        }
    }
}
