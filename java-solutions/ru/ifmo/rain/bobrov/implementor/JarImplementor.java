package ru.ifmo.rain.bobrov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * Class provides public methods to generate implementations for given interfaces and packs it into {@code .jar} if it is needed.
 * Class implements {@link Impler}  and {@link JarImpler} interfaces.
 *
 * @author Bobrov Oleg
 * @version 0.0.1
 * @see Impler
 * @see JarImpler
 */
public class JarImplementor implements Impler, JarImpler {
    /**
     * Variable for producing unique argument name in method
     */
    private static int num = 0;

    /**
     * Default constructor.
     */
    public JarImplementor() {

    }

    /**
     * Main method receives array of string arguments.
     * A command line utility for {@link JarImplementor}.
     * <p>
     * First launchMode: 2 arguments:{@code <className> <outputPath>}. Creates a {@code .java} file, {@link #implement(Class, Path)} is used.
     * Second launchMode: 3 arguments: {@code -jar <className> <outputPath>}. Creates a {@code .jar} file, {@link #implementJar(Class, Path)} is used.
     * <p>
     * Inform about any errors occurred.
     *
     * @param args of command line
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 2 && args.length != 3)
                || args[0] == null || args[1] == null) {
            System.out.println("Wrong arguments");
            return;
        }
        if (args.length == 3 && args[2] == null) {
            System.out.println("Wrong arguments");
            return;
        }
        boolean isJar = false;
        if (args.length == 3 && "-jar".equals(args[0])) {
            isJar = true;
            args[0] = args[1];
            args[1] = args[2];
        }
        Class<?> token;
        try {
            token = Class.forName(args[0]);
        } catch (ClassNotFoundException e) {
            System.out.println("Interface not found");
            return;
        }
        Path root;
        try {
            root = Paths.get(args[1]);
        } catch (InvalidPathException e) {
            System.out.println("Path not found");
            return;
        }
        try {
            if (isJar) {
                new JarImplementor().implementJar(token, root);
            } else {
                new JarImplementor().implement(token, root);
            }
        } catch (ImplerException e) {
            System.out.println("Error during implementation");
        }
    }

    /**
     * Creates {@code .jar} file containing class with name {@code token}Impl.java implementing interface specified by provided {@code token}.
     * Created {@code .jar} file location is specified by {@code jarFile}.
     *
     * @param token   {@link Class} type token to create implementation for.
     * @param jarFile {@link Path} target {@code .jar} file.
     * @throws ImplerException when occurs error during implementation.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (token.isPrimitive() || Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Token can't be primitive or has private modifier.");
        }
        Path jarParentPath = jarFile.toAbsolutePath().normalize().getParent();
        if (jarParentPath != null) {
            try {
                Files.createDirectories(jarParentPath);
            } catch (IOException e) {
                throw new ImplerException("Error during creating jar parent dir" + e.getMessage());
            }
        } else {
            throw new ImplerException("Jar parent path is null");
        }
        Path implementationPath;
        try {
            implementationPath = Files.createTempDirectory(jarParentPath, "tmp");
        } catch (IOException e) {
            throw new ImplerException("Error during creating temporary dir" + e.getMessage());
        }
        try {
            implement(token, implementationPath);
            compile(token, implementationPath);
            createJarFile(token, jarFile, implementationPath);
        } finally {
            deleteDir(implementationPath.toFile());
        }

    }

    /**
     * Compiles code of token implementation stored in temporary directory.
     *
     * @param token              {@link Class} to compile implementation of.
     * @param implementationPath {@link Path} of directory where implementation code source will be stored
     * @throws ImplerException in case impossibility to find valid class path
     * @throws ImplerException in case cannot find java compiler
     * @throws ImplerException in case error during compilation occurred
     */
    private void compile(Class<?> token, Path implementationPath) throws ImplerException {
        Path originalPath;
        try {
            CodeSource codeSource = token.getProtectionDomain().getCodeSource();
            String uri = codeSource == null ? "" : codeSource.getLocation().getPath();
            if (uri.toCharArray()[0] == '/') {
                uri = uri.substring(1);
            }
            originalPath = Path.of(uri);
        } catch (InvalidPathException e) {
            throw new ImplerException("Impossible to find valid class path: " + e.getMessage());
        }
        String[] compilerArgs = {
                "-encoding", "UTF-8",
                "-cp",
                implementationPath.toString() + File.pathSeparator + originalPath.toString(),
                Path.of(implementationPath.toString(),
                        token.getPackageName().replace('.', File.separatorChar),
                        token.getSimpleName() + "Impl.java").toString()
        };
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Cannot find java compiler");
        }
        if (compiler.run(null, null, null, compilerArgs) != 0) {
            throw new ImplerException("Error during compilation");
        }
    }

    /**
     * Creates {@code .jar} file containing compiled implementation of {@code token} at {@link Path}.
     *
     * @param token              {@link Class} to zip implementation of.
     * @param implementationPath {@link Path} of directory where implementation code is stored
     * @param jarPath            {@link Path} where resulting {@code .jar} will be created
     * @throws ImplerException in case I/O error occurred
     */
    private void createJarFile(Class<?> token, Path jarPath, Path implementationPath) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream stream = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
            String name = String.join("/", token.getPackageName().split("\\.")) +
                    "/" + token.getSimpleName() + "Impl.class";
            stream.putNextEntry(new JarEntry(name));
            Files.copy(Paths.get(implementationPath.toString(), name), stream);
        } catch (IOException e) {
            throw new ImplerException("Impossible to create jar " + e.getMessage());
        }
    }

    /**
     * Creates class with name {@code token}Impl.java implementing interface specified by provided {@code token}.
     * Generated {@code .java} file location is specified by {@code root}.
     *
     * @param token {@link Class} type token to create implementation for.
     * @param root  {@link Path} root directory.
     * @throws ImplerException when occurs error during implementation.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token.isPrimitive() || Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Token can't be primitive");
        }
        String rootName = String.join(File.separator, token.getPackageName().split("\\.")) +
                File.separator + token.getSimpleName() + "Impl.java";
        Path rootPath;
        try {
            rootPath = Paths.get(root.toString(), rootName);
        } catch (InvalidPathException e) {
            throw new ImplerException("Invalid path", e);
        }
        if (rootPath.getParent() != null) {
            if (!Files.exists(rootPath.getParent())) {
                try {
                    Files.createDirectories(rootPath.getParent());
                } catch (IOException e) {
                    throw new ImplerException("Invalid path", e);
                }
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(rootPath)) {
            writer.write(generate(token));
        } catch (IOException e) {
            throw new ImplerException("Error during writing");
        }
    }

    /**
     * Creates source code for given {@link Class}.
     *
     * @param token {@link Class} which implementation is required
     * @return {@link String} generated source code
     */
    private String generate(Class<?> token) {
        String packageName = generatePackageName(token);
        String className = generateClassName(token);
        String methods = generateMethods(token);
        return writeEncoding(String.join(System.lineSeparator(), packageName, className, methods, "}"));
    }

    /**
     * Creates a {@link String} that describes package class package.
     *
     * @param token {@link Class} to get package info
     * @return {@link String} containing package info declaration
     */
    private String generatePackageName(Class<?> token) {
        Package pack = token.getPackage();
        if (pack == null || pack.getName().equals("")) {
            return "";
        }
        return String.join(" ", "package", pack.getName(), ";");
    }

    /**
     * Creates class name declaration block.
     *
     * @param token {@link Class} which implementation is required
     * @return {@link String} class name with provided modifiers and class opening bracket
     * @see #getModifiers(int, int)
     */
    private String generateClassName(Class<?> token) {
        return String.join(" ",
                getModifiers(token.getModifiers(), Modifier.INTERFACE),
                "class", token.getSimpleName() + "Impl", "implements", token.getCanonicalName(), "{");
    }

    /**
     * Creates all methods class by provided {@code token} and join them with system line separator.
     *
     * @param token {@link Class} which implementation is required
     * @return {@link String} containing all methods implemented by provided {@code token} separated by system line separator.
     * @see #getMethodSignature(Method)
     */
    private String generateMethods(Class<?> token) {
        return Arrays.stream(token.getMethods())
                .map(this::getMethodSignature)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Creates signature of specific method with needed modifiers, name, arguments, return type and exceptions.
     *
     * @param method {@link Method} which signature is required
     * @return {@link String} containing fully generated signature of provided {@code method}
     * @see #getType(Class)
     * @see #getModifiers(int, int)
     */
    private String getMethodSignature(Method method) {
        String modifiers = getModifiers(method.getModifiers(), Modifier.TRANSIENT);
        String returnType = method.getReturnType().getCanonicalName();
        String name = method.getName();
        String args = Arrays.stream(method.getParameterTypes())
                .map(type -> String.join(" ", type.getCanonicalName(), "arg" + num++))
                .collect(Collectors.joining(","));
        String exceptions = method.getExceptionTypes().length == 0 ? "" :
                String.join(" ", "throws", Arrays.stream(method.getExceptionTypes())
                        .map(Class::getCanonicalName)
                        .collect(Collectors.joining(", ")));
        String nameLine = String.join(" ", modifiers, returnType, name, "(", args, ")", exceptions, "{");
        String body = String.join(" ", "return", getType(method.getReturnType()), ";", System.lineSeparator(), "}");
        return String.join(System.lineSeparator(), nameLine, body);
    }

    /**
     * Provides valid return type for {@link Class } provided
     *
     * @param methodToken {@link Class} which return type is required
     * @return {@link String} containing valid return type for {@code methodToken } provided
     */
    private String getType(Class<?> methodToken) {
        if (!methodToken.isPrimitive()) {
            return "null";
        } else if (methodToken.equals(void.class)) {
            return "";
        } else if (methodToken.equals(boolean.class)) {
            return "false";
        } else {
            return "0";
        }
    }

    /**
     * Provides valid modifiers extracting {@code modifier}
     * for {@link Class} or {@link Method} generated.
     *
     * @param curModifiers {@link int} value of current {@link Class} or {@link Method} modifiers
     * @param modifier     {@link int} value of modifier to extract from {@link Class} or {@link Method}
     * @return {@link String} containing valid modifiers for {@link Class} or {@link Method}
     */
    private String getModifiers(int curModifiers, int modifier) {
        return Modifier.toString(curModifiers & ~Modifier.ABSTRACT & ~modifier & ~Modifier.STATIC & ~Modifier.PROTECTED & ~Modifier.PRIVATE);
    }

    /**
     * Encodes the provided {@code String}, escaping all unicode characters with {@code \\u} prefix.
     *
     * @param str {@link String} to be encoded
     * @return the encoded {@link String}
     */
    private static String writeEncoding(String str) {
        StringBuilder builder = new StringBuilder();
        for (char elem : str.toCharArray()) {
            if (elem < 128) {
                builder.append(elem);
            } else {
                builder.append("\\u").append(String.format("%04x", (int) elem));
            }
        }
        return builder.toString();
    }

    /**
     * Deletes all files of directory {@link File} recursively.
     *
     * @param file target directory {@link Path}
     * @return {@code true} if dir was successfully deleted and false either
     */
    private boolean deleteDir(File file) {
        boolean result = true;
        File[] files = file.listFiles();
        if (files != null) {
            for (File subFile : files) {
                result &= deleteDir(subFile);
            }
        }
        return result & file.delete();
    }
}
