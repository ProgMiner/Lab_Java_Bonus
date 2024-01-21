package ru.byprogminer.servertester;

import ru.byprogminer.servertester.config.TestConfig;
import ru.byprogminer.servertester.config.loader.PropertiesTestConfigLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {

    private static final Pattern ARG_PATTERN = Pattern.compile("^--(.*)=(.*)$");

    private static final Path configPath = Paths.get("./config.properties");

    public static void main(String[] args) throws IOException {
        new TestRunner(loadConfig(args)).runTest();
    }

    private static TestConfig loadConfig(String[] args) {
        final Properties properties = new Properties();
        RuntimeException ex = null;

        try {
            loadConfigFile(properties);
        } catch (RuntimeException e) {
            ex = Utils.nextException(ex, e);
        }

        try {
            argsToProperties(args, properties);
        } catch (RuntimeException e) {
            ex = Utils.nextException(ex, e);
        }

        final TestConfig config = new TestConfig();

        try {
            new PropertiesTestConfigLoader(properties).load(config);
        } catch (RuntimeException e) {
            ex = Utils.nextException(ex, e);
        }

        try {
            config.verify();
        } catch (RuntimeException e) {
            ex = Utils.nextException(ex, e);
        }

        if (ex != null) {
            throw ex;
        }

        return config;
    }

    private static void loadConfigFile(Properties properties) {
        if (!Files.exists(configPath)) {
            return;
        }

        try (final InputStream is = Files.newInputStream(configPath)) {
            properties.load(is);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static void argsToProperties(String[] args, Properties result) {
        for (String arg : args) {
            final Matcher m = ARG_PATTERN.matcher(arg);

            if (!m.find()) {
                throw new IllegalArgumentException("arguments must have form --key=value");
            }

            result.put(m.group(1), m.group(2));
        }
    }
}
