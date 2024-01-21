package ru.byprogminer.servertester.config.loader;

import ru.byprogminer.servertester.Utils;
import ru.byprogminer.servertester.config.PrettyDuration;
import ru.byprogminer.servertester.config.TestConfig;

import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;


public class PropertiesTestConfigLoader implements TestConfigLoader {

    private final Properties properties;

    private final VariableParser<Integer> intVariableParser = new IntVariableParser();
    private final VariableParser<PrettyDuration> durationVariableParser = new DurationVariableParser();

    public PropertiesTestConfigLoader(Properties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    public void load(TestConfig config) {
        RuntimeException ex = null;

        for (final Key key : Key.values()) {
            final String value = properties.getProperty(key.key);

            if (value != null) {
                try {
                    key.handle(value, this, config);
                } catch (RuntimeException e) {
                    final RuntimeException e1 =
                            new IllegalArgumentException("cannot parse argument \"" + key.key + "\"", e);

                    ex = Utils.nextException(ex, e1);
                }
            }
        }

        if (ex != null) {
            throw ex;
        }
    }

    private enum Key {

        ARCH("arch") {

            @Override
            public void handle(String value, PropertiesTestConfigLoader self, TestConfig config) {
                config.setArch(TestConfig.Architecture.valueOf(value.toUpperCase()));
            }
        },

        CLIENT_REQUESTS("client_requests") {

            @Override
            public void handle(String value, PropertiesTestConfigLoader self, TestConfig config) {
                config.setClientRequests(Integer.parseInt(value));
            }
        },

        ARRAY_SIZE("array_size") {

            @Override
            public void handle(String value, PropertiesTestConfigLoader self, TestConfig config) {
                config.setArraySize(self.intVariableParser.parse(value));
            }
        },

        CLIENTS("clients") {

            @Override
            public void handle(String value, PropertiesTestConfigLoader self, TestConfig config) {
                config.setClients(self.intVariableParser.parse(value));
            }
        },

        REQUEST_DELTA("request_delta") {

            @Override
            public void handle(String value, PropertiesTestConfigLoader self, TestConfig config) {
                config.setRequestDelta(self.durationVariableParser.parse(value));
            }
        },

        OUTPUT_DIR("output_dir") {

            @Override
            public void handle(String value, PropertiesTestConfigLoader self, TestConfig config) {
                config.setOutputDir(Paths.get(value));
            }
        };

        public final String key;

        Key(String key) {
            this.key = Objects.requireNonNull(key);
        }

        public abstract void handle(String value, PropertiesTestConfigLoader self, TestConfig config);
    }
}
