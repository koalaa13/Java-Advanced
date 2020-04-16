package ru.ifmo.rain.maksimov.test;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;
import ru.ifmo.rain.maksimov.implementor.Implementor;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws ImplerException {
        JarImpler impler = new Implementor();
        Path path = Path.of("");
        impler.implement(SimpleInterface.class, path);
    }
}
