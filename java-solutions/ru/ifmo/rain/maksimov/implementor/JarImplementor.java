package ru.ifmo.rain.maksimov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import static ru.ifmo.rain.maksimov.utils.Helper.log;

/**
 * Class that can used to create jar archives automatically.
 * Extended from {@link Implementor}
 */
public class JarImplementor extends Implementor {

    /**
     * Default constructor
     */
    public JarImplementor() {
    }

    /**
     * Usage: -jar type_token path.
     * Type token of abstract class or interface to generate a implementation for.
     * Path where to create jar file contained generated implementations.
     *
     * @param args arguments for a program
     */
    public static void main(String[] args) {
        if (args == null || args.length != 3) {
            log("Use -jar <type_token> <path>");
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                log("Argument can not be null");
                return;
            }
        }
        JarImpler implementor = new Implementor();
        try {
            if ("-jar".equals(args[0])) {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else {
                log("Invalid first argument for a program");
            }
        } catch (ClassNotFoundException e) {
            log("Incorrect class name", e);
        } catch (ImplerException e) {
            log("An error occurred while implementing", e);
        } catch (InvalidPathException e) {
            log("Invalid output path", e);
        }
    }
}
