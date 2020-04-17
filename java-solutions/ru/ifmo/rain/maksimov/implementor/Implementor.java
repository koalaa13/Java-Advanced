package ru.ifmo.rain.maksimov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import static ru.ifmo.rain.maksimov.utils.Helper.*;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Implementation class for {@link JarImpler} interface
 *
 * @author koalaa13 (github.com/koalaa13)
 */
public class Implementor implements JarImpler {
    /**
     * Default constructor
     */
    public Implementor() {
    }

    /**
     * Default value for object
     */
    private static final String DEFAULT_OBJECT = " null";
    /**
     * Default value for all primitives
     */
    private final static String DEFAULT_PRIMITIVE = " 0";
    /**
     * Default value for void methods
     */
    private final static String DEFAULT_VOID = "";
    /**
     * Default value for boolean
     */
    private final static String DEFAULT_BOOLEAN = " false";
    /**
     * End of line symbol
     */
    private final static String END_OF_LINE = System.lineSeparator();
    /**
     * Tab as 4 spaces
     */
    private final static String TAB = "    ";
    /**
     * Instance of {@link Cleaner} used in {@link #cleanDir(Path)}
     */
    private final static Cleaner DELETER = new Cleaner();


    /**
     * Class used for recursively deleting folders
     */
    private static class Cleaner extends SimpleFileVisitor<Path> {

        /**
         * Default constructor
         */
        public Cleaner() {
        }

        /**
         * Deletes file
         *
         * @param file  current file in fileTree
         * @param attrs attributes of file
         * @return {@link FileVisitResult#CONTINUE}
         * @throws IOException if error occurred while deleting file
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        /**
         * {@link Override} of {@link SimpleFileVisitor#postVisitDirectory(Object, IOException)}
         *
         * @param dir current directory in fileTree
         * @param exc null if the iteration threw files in directory completes without error
         *            else the {@link IOException} that caused the iteration threw files in directory finished prematurely
         * @return {@link FileVisitResult#CONTINUE}
         * @throws IOException if error occurred while deleting directory
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Wrapper class for {@link Method}
     */
    private static class MethodWrapper {

        /**
         * Method to store
         */
        private final Method method;

        /**
         * Constructs a {@link MethodWrapper} for instance of {@link Method}
         *
         * @param method instance of {@link Method} to be wrapped
         */
        private MethodWrapper(Method method) {
            this.method = method;
        }

        /**
         * Compares object with this wrapper for equality. Wrappers are equal
         * if their wrapped methods have equal name, return type and parameters' types.
         *
         * @param o instance of {@link Object} to compare with
         * @return true if object is equal to this wrapper, false otherwise
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Method that = ((MethodWrapper) o).getMethod();
            return Arrays.equals(method.getParameterTypes(), that.getParameterTypes()) &&
                    method.getReturnType().equals(that.getReturnType()) &&
                    method.getName().equals(that.getName());
        }

        /**
         * Calculate hashcode for this wrapper, using {@link Objects#hash(Object...)}
         *
         * @return hashcode for this wrapper
         */
        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(method.getParameterTypes()),
                    method.getReturnType().hashCode(),
                    method.getName().hashCode());
        }


        /**
         * Getter for wrapped method
         *
         * @return wrapped method
         */
        public Method getMethod() {
            return method;
        }

    }

    /**
     * Recursively deletes directory represented by {@link Path}
     *
     * @param dir directory to be removed
     * @throws IOException if error occurred while deleting
     */
    private static void cleanDir(Path dir) throws IOException {
        Files.walkFileTree(dir, DELETER);
    }

    /**
     * Add "Impl" suffix to {@link Class#getSimpleName()} of given class token
     *
     * @param token token class to get name
     * @return {@link String} with name ended with "Impl"
     */
    private String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Function to get {@link String} with a few {@link Implementor#TAB}
     *
     * @param count count of {@link Implementor#TAB} to get
     * @return {@link String} that contains that count of {@link Implementor#TAB}
     */
    private String getTabs(int count) {
        return TAB.repeat(Math.max(0, count));
    }

    /**
     * Get package of given class.
     *
     * @param token class token to get package
     * @return name of package
     */
    private String getPackage(Class<?> token) {
        StringBuilder res = new StringBuilder();
        if (!"".equals(token.getPackageName())) {
            res.append("package ").append(token.getPackageName()).append(';').append(END_OF_LINE);
        }
        res.append(END_OF_LINE);
        return res.toString();
    }

    /**
     * Get beginning declaration of the class, containing package, modifier,
     * name and extended class or implemented interface
     *
     * @param token class token to get header
     * @return {@link String} beginning of declaration
     */
    private String getClassHead(Class<?> token) {
        return getPackage(token) + "public class " + getClassName(token) + ' ' +
                (token.isInterface() ? "implements " : "extends ") +
                token.getCanonicalName() + " {" + END_OF_LINE;
    }

    /**
     * If {@link Executable} is {@link Method} returns return type and name of it,
     * else returns name of generated class
     *
     * @param token      class token to get {@link Constructor}
     * @param executable instance of {@link Method} or {@link Constructor}
     * @return {@link String} representation of return type and name if {@link Executable}
     * is {@link Method} or name of generated class if {@link Constructor}
     */
    private String getReturnTypeAndName(Class<?> token, Executable executable) {
        if (executable instanceof Method) {
            Method tmp = (Method) executable;
            return tmp.getReturnType().getCanonicalName() + ' ' + tmp.getName();
        } else {
            return getClassName(token);
        }
    }

    /**
     * Return representation of {@link Parameter} with type or no
     *
     * @param parameter  parameter to get a representation
     * @param typeNeeded boolean flag responsible for adding parameter type
     * @return {@link String} representation of parameter
     */
    private String getParam(Parameter parameter, boolean typeNeeded) {
        return (typeNeeded ? parameter.getType().getCanonicalName() + ' ' : "") + parameter.getName();
    }

    /**
     * Returns representation of all parameters of {@link Executable}
     * divided by ','
     *
     * @param executable {@link Executable} to get representation of its parameters
     * @param typeNeeded boolean flag responsible for adding parameters types
     * @return {@link String} representation of parameters
     */
    private String getParams(Executable executable, boolean typeNeeded) {
        return Arrays.stream(executable.getParameters())
                .map(parameter -> getParam(parameter, typeNeeded))
                .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Get representation of exceptions those could be thrown by {@link Executable}
     *
     * @param executable {@link Executable} to get exceptions
     * @return {@link String} representation of exceptions of {@link Executable}
     */
    private String getExceptions(Executable executable) {
        StringBuilder res = new StringBuilder();
        Class<?>[] exceptions = executable.getExceptionTypes();
        if (exceptions.length > 0) {
            res.append(" throws ");
        }
        res.append(Arrays.stream(exceptions)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", ")));
        return res.toString();
    }

    /**
     * Get default return value for {@link Class}
     *
     * @param token class token to get default value
     * @return {@link String} representation of default value for {@link Class}
     */
    private String getDefaultValue(Class<?> token) {
        if (boolean.class.equals(token)) {
            return DEFAULT_BOOLEAN;
        } else if (void.class.equals(token)) {
            return DEFAULT_VOID;
        } else if (token.isPrimitive()) {
            return DEFAULT_PRIMITIVE;
        }
        return DEFAULT_OBJECT;
    }

    /**
     * Get representation of body of {@link Method} or {@link Constructor}
     *
     * @param executable {@link Method} or {@link Constructor} to get body
     * @return {@link String} representation of body of {@link Method} or {@link Constructor}
     */
    private String getBody(Executable executable) {
        if (executable instanceof Method) {
            return "return" + getDefaultValue(((Method) executable).getReturnType()) + ';';
        } else {
            return "super" + getParams(executable, false) + ';';
        }
    }

    /**
     * Get whole representation of {@link Method} or {@link Constructor}
     *
     * @param token      null if {@link Executable} is {@link Method}
     *                   or class token to get representation of constructor
     *                   if {@link Executable} is {@link Constructor}
     * @param executable {@link Method} or {@link Constructor}
     * @return {@link String} representation of {@link Method} or {@link Constructor}
     */
    private String getExecutable(Class<?> token, Executable executable) {
        StringBuilder res = new StringBuilder(getTabs(1));
        final int mods = executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.NATIVE & ~Modifier.TRANSIENT;
        res.append(Modifier.toString(mods))
                .append(mods > 0 ? " " : "")
                .append(getReturnTypeAndName(token, executable))
                .append(getParams(executable, true))
                .append(getExceptions(executable))
                .append(" {")
                .append(END_OF_LINE)
                .append(getTabs(2))
                .append(getBody(executable))
                .append(END_OF_LINE)
                .append(getTabs(1))
                .append("}")
                .append(END_OF_LINE);
        return res.toString();
    }

    /**
     * Wrap all methods and add them to storage
     *
     * @param methods Array of {@link Method} to add
     * @param storage {@link Set} where to add
     */
    private void addMethodsToSet(Method[] methods, Set<MethodWrapper> storage) {
        Arrays.stream(methods)
                .map(MethodWrapper::new)
                .forEach(storage::add);
    }

    /**
     * Get only abstract methods from {@link Set} of {@link MethodWrapper}
     *
     * @param storage {@link Set} to get abstract methods from
     * @return {@link Set} with only abstract methods
     */
    private Set<MethodWrapper> getOnlyAbstractMethods(Set<MethodWrapper> storage) {
        return storage.stream()
                .filter(methodWrapper -> Modifier.isAbstract(methodWrapper.getMethod().getModifiers()))
                .collect(Collectors.toSet());
    }

    /**
     * Write representations of all abstract methods of given {@link Class}
     * to given {@link Writer}
     *
     * @param token  class token to generate abstract methods
     * @param writer instance of {@link Writer} where to write representation of methods
     * @throws IOException when error occurred during writing
     */
    private void implementAbstractMethods(Class<?> token, Writer writer) throws IOException {
        Set<MethodWrapper> methods = new HashSet<>();
        addMethodsToSet(token.getMethods(), methods);
        while (token != null) {
            addMethodsToSet(token.getDeclaredMethods(), methods);
            token = token.getSuperclass();
        }
        for (MethodWrapper methodWrapper : getOnlyAbstractMethods(methods)) {
            writer.write(toUnicode(getExecutable(null, methodWrapper.getMethod())));
        }
    }

    /**
     * Write representations of all non-private constructors of given {@link Class}
     * to given {@link Writer}
     *
     * @param token  class token to generate constructors
     * @param writer instance of {@link Writer} where to write representation of constructors
     * @throws ImplerException when there is no non-private constructors of class
     * @throws IOException     when error occurred during writing
     */
    private void implementConstructors(Class<?> token, Writer writer) throws ImplerException, IOException {
        Constructor<?>[] constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .toArray(Constructor[]::new);
        if (constructors.length == 0) {
            throw new ImplerException("There is no non-private constructors in class");
        }
        for (Constructor<?> constructor : constructors) {
            writer.write(toUnicode(getExecutable(token, constructor)));
        }
    }

    /**
     * Write whole implementation of given instance of {@link Class}
     * to given {@link Path}
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException when can not implement class
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkNotNull(token, root);
        if (token.isPrimitive() || token.isArray() || token == Enum.class || Modifier.isFinal(token.getModifiers()) || Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Incorrect class token");
        }
        root = getPath(token, root, ".java");
        createDirectories(root);

        try (BufferedWriter writer = Files.newBufferedWriter(root)) {
            try {
                writer.write(toUnicode(getClassHead(token)));
                if (!token.isInterface()) {
                    implementConstructors(token, writer);
                }
                implementAbstractMethods(token, writer);
                writer.write('}' + END_OF_LINE);
            } catch (IOException e) {
                throw new ImplerException("Error while writing to output file", e);
            }
        } catch (IOException e) {
            throw new ImplerException("Unable to write to output file", e);
        }
    }

    /**
     * Create directory represented by {@link Path}
     *
     * @param root {@link Path} where to create directory
     * @throws ImplerException when error occurred while creating directories
     */
    private void createDirectories(Path root) throws ImplerException {
        if (root.getParent() != null) {
            try {
                Files.createDirectories(root.getParent());
            } catch (IOException e) {
                throw new ImplerException("Error while creating directories for output file", e);
            }
        }
    }

    /**
     * Returns path to file, containing implementation of given class, with specific file extension
     * located in directory represented by {@link Path}
     *
     * @param token class to get name from
     * @param root  path to parent directory of class
     * @param end   file extension
     * @return {@link Path} representing path to file
     */
    private Path getPath(Class<?> token, Path root, String end) {
        return root.resolve(token.getPackageName().replace('.', File.separatorChar))
                .resolve(getClassName(token) + end);
    }

    /**
     * Return UNICODE representation of given {@link String}
     *
     * @param str {@link String} to get UNICODE format
     * @return {@link String} with UNICODE format of given string
     */
    private String toUnicode(String str) {
        StringBuilder b = new StringBuilder();

        for (char c : str.toCharArray()) {
            if (c >= 128)
                b.append("\\u").append(String.format("%04X", (int) c));
            else
                b.append(c);
        }

        return b.toString();
    }

    /**
     * Get path for class token
     *
     * @param token class token to get path for
     * @return {@link String string} that contains representation of path
     * @throws ImplerException when can not get path
     */
    private String getClassPath(final Class<?> token) throws ImplerException {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            throw new ImplerException("Error while getting path for class token", e);
        }
    }

    /**
     * As same as {@link #implement(Class, Path)} but
     * creates a .jar file
     *
     * @param token type token to create implementation for.
     * @param root  {@link Path} where to create .jar file
     * @throws ImplerException when error occurred
     *                         during implementation or creating .jar file
     */
    @Override
    public void implementJar(Class<?> token, Path root) throws ImplerException {
        checkNotNull(token, root);
        createDirectories(root);
        final Path tempDir;
        try {
            tempDir = Files.createTempDirectory(root.toAbsolutePath().getParent(), "temp");
        } catch (IOException e) {
            throw new ImplerException("Can not create temp directory", e);
        }
        try {
            implement(token, tempDir);
            final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
            if (javaCompiler == null || javaCompiler.run(null, null, null, "-cp",
                    tempDir.toString() + File.pathSeparator + System.getProperty("java.class.path") +
                            File.pathSeparator + getClassPath(token),
                    getPath(token, tempDir, ".java").toString()) != 0) {
                throw new ImplerException("Error while compiling generated files");
            }
            Manifest manifest = new Manifest();
            Attributes attributes = manifest.getMainAttributes();
            attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, "Nikita Maksimov");
            try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(root), manifest)) {
                jarOutputStream.putNextEntry(new ZipEntry(getZipEntryName(token)));
                Files.copy(getPath(token, tempDir, ".class"), jarOutputStream);
            } catch (IOException e) {
                throw new ImplerException("Error while writing to JAR file", e);
            }
        } finally {
            try {
                cleanDir(tempDir);
            } catch (IOException e) {
                log("Can not delete temp directory", e);
            }
        }
    }

    /**
     * Get name of {@link ZipEntry} for given {@link Class}.
     *
     * @param token class token to get name of {@link ZipEntry}
     * @return {@link String} representation of name of {@link ZipEntry}
     */
    private String getZipEntryName(Class<?> token) {
        return token.getPackageName().replace('.', '/')
                + '/' + getClassName(token)
                + ".class";
    }

    /**
     * Check is class token and path are not null
     *
     * @param token {@link Class} to check
     * @param root  {@link Path} to check
     * @throws ImplerException when {@link Class} or {@link Path} are null
     */
    private void checkNotNull(Class<?> token, Path root) throws ImplerException {
        if (token == null) {
            throw new ImplerException("Class token can not be null");
        }
        if (root == null) {
            throw new ImplerException("Path root can not be null");
        }
    }

    /**
     * Usage: className rootPath - runs {@link #implement(Class, Path)} with given arguments
     * If arguments are incorrect or an error occurred during implementation log a message with {@link ru.ifmo.rain.maksimov.utils.Helper#log(String, Exception)}
     *
     * @param args arguments to run an application with
     */
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            log("Use <type_token> <path>");
            return;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            log("Arguments can not be null");
            return;
        }
        JarImpler implementor = new Implementor();
        try {
            implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
        } catch (ClassNotFoundException e) {
            log("Incorrect class name", e);
        } catch (ImplerException e) {
            log("An error occurred while implementing", e);
        } catch (InvalidPathException e) {
            log("Invalid output path", e);
        }
    }
}