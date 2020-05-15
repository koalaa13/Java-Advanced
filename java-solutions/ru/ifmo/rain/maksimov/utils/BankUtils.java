package ru.ifmo.rain.maksimov.utils;

import ru.ifmo.rain.maksimov.bank.Person;

import java.rmi.RemoteException;

public class BankUtils {
    public static void logAction(String message) {
        System.out.println(message);
    }

    public static String getAccountId(int passportId, String accountSubId) {
        return String.valueOf(passportId) + ':' + accountSubId;
    }

    public static boolean validatePerson(final int passportId, final String firstName, final String lastName) {
        return passportId < 0 || firstName == null || lastName == null || firstName.isEmpty() || lastName.isEmpty();
    }

    public static boolean validateAccount(final Person person, final String accountId) throws RemoteException {
        return person == null || validatePerson(person.getPassportId(), person.getFirstName(), person.getLastName()) || accountId == null || accountId.isEmpty();
    }
}
