package ru.ifmo.rain.bobrov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Walk {
    private static final int PRIME_NUMBER = 0x01000193;

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Wrong arguments.");
            return;
        }
        Path pathIn;
        Path pathOut;
        try {
            pathIn = Paths.get(args[0]);
            pathOut = Paths.get(args[1]);
        } catch (InvalidPathException e) {
            System.err.println("Input or output path is incorrect.");
            return;
        }
        if (pathOut.getParent() != null) {
            try {
                Files.createDirectories(pathOut.getParent());
            } catch (IOException e) {
                System.err.println("Impossible to create parent directory.");
                return;
            }
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(pathIn.toFile()), StandardCharsets.UTF_8))) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathOut.toFile()), StandardCharsets.UTF_8))) {
                String fileName;
                while ((fileName = reader.readLine()) != null) {
                    writer.write(String.format("%08x %s%n", FNV(fileName), fileName));
                }
            } catch (FileNotFoundException e) {
                System.err.println("Output file not found.");
            } catch (IOException e) {
                System.err.println("Error while writing files.");
            }
        } catch (FileNotFoundException e) {
            System.err.println("Input file not found.");
        } catch (IOException e) {
            System.err.println("Error while reading  files.");
        }
    }

    private static int FNV(String fileName) {
        int hash = 0x811c9dc5;
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(Paths.get(fileName)))) {
            byte[] buff = new byte[1024];
            int length;
            while ((length = inputStream.read(buff)) != -1) {
                for (int i = 0; i < length; ++i) {
                    hash *= PRIME_NUMBER;
                    hash ^= (buff[i] & 0xFF);
                }
            }
        } catch (Exception e) {
            return 0;
        }
        return hash;
    }
}
