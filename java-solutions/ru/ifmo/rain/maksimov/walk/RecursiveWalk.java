package ru.ifmo.rain.maksimov.walk;

import static ru.ifmo.rain.maksimov.utils.Helper.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {
    private final static int BUFFER_SIZE = 8192;

    public static String hash(String filename) {
        final int PRIME_KEY = 0x01000193;
        int res = 0x811c9dc5;
        try (InputStream fileInputStream = Files.newInputStream(Paths.get(filename))) {
            byte[] buf = new byte[BUFFER_SIZE];
            for (int readed; (readed = fileInputStream.read(buf, 0, BUFFER_SIZE)) >= 0; ) {
                for (int i = 0; i < readed; ++i) {
                    res *= PRIME_KEY;
                    res ^= buf[i] & 0xff;
                }
            }
        } catch (IOException e) {
            log("IOException while hashing file " + filename, e);
            return getOutputFormat(filename, 0);
        }
        return getOutputFormat(filename, res);
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            log("Invalid arguments. Use RecursiveWalk <input file> <output file>");
            return;
        }
        Path in, out;
        try {
            in = Paths.get(args[0]);
        } catch (InvalidPathException e) {
            log("Invalid input file: " + args[0], e);
            return;
        }

        try {
            out = Paths.get(args[1]);
        } catch (InvalidPathException e) {
            log("Invalid output file: " + args[1], e);
            return;
        }

        if (out.getParent() != null) {
            try {
                Files.createDirectories(out.getParent());
            } catch (IOException e) {
                log("Can not create output file", e);
                return;
            }
        }

        try (BufferedReader reader = Files.newBufferedReader(in)) {
            try (BufferedWriter writer = Files.newBufferedWriter(out)) {
                String filename = reader.readLine();
                while (filename != null) {
                    try {
                        MyVisitor visitor = new MyVisitor(writer);
                        Files.walkFileTree(Paths.get(filename), visitor);
                    } catch (FileNotFoundException e) {
                        log("No such file: " + filename, e);
                        writer.write(getOutputFormat(filename, 0));
                    } catch (InvalidPathException e) {
                        log("Invalid path:" + filename, e);
                        writer.write(getOutputFormat(filename, 0));
                    } catch (IOException e) {
                        log("Read or write error occurred", e);
                        writer.write(getOutputFormat(filename, 0));
                    }
                    filename = reader.readLine();
                }
            } catch (FileNotFoundException e) {
                log("No such output file", e);
            } catch (IOException e) {
                log("Read or write error occurred with output file", e);
            }
        } catch (FileNotFoundException e) {
            log("No such input file", e);
        } catch (IOException e) {
            log("Read or write error occurred with input file", e);
        }
    }

    private static class MyVisitor extends SimpleFileVisitor<Path> {
        private final Writer writer;

        private MyVisitor(Writer writer) {
            this.writer = writer;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            writer.write(hash(file.toString()));
            return FileVisitResult.CONTINUE;
        }
    }
}
