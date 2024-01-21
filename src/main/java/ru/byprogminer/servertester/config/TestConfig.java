package ru.byprogminer.servertester.config;

import ru.byprogminer.servertester.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;


public class TestConfig implements Iterable<TestRunConfig> {

    private Architecture arch;
    private Integer clientRequests;

    private Variable<Integer> arraySize;
    private Variable<Integer> clients;
    private Variable<PrettyDuration> requestDelta;

    private Path outputDir;

    private Field rangeField;

    public void verify() {
        RuntimeException ex = null;

        for (final Field f : Field.values()) {
            if (f.getValue(this) == null) {
                ex = Utils.nextException(ex, new IllegalArgumentException(f.name + " isn't set"));
                continue;
            }

            try {
                f.verify(this);
            } catch (RuntimeException e) {
                ex = Utils.nextException(ex, e);
            }
        }

        if (ex != null) {
            throw ex;
        }

        {
            int variables = 3;

            if (arraySize.isConstant()) {
                --variables;
            } else {
                rangeField = Field.ARRAY_SIZE;
            }

            if (clients.isConstant()) {
                --variables;
            } else {
                rangeField = Field.CLIENTS;
            }

            if (requestDelta.isConstant()) {
                --variables;
            } else {
                rangeField = Field.REQUEST_DELTA;
            }

            if (variables != 1) {
                ex = new IllegalArgumentException(
                        "one of variables must not be constant (currently: " + variables + ")"
                );
            }
        }

        if (ex != null) {
            throw ex;
        }
    }

    public String getVariableParameter() {
        return rangeField.name;
    }

    @Override
    public Iterator<TestRunConfig> iterator() {
        return rangeField.iterator(this);
    }

    private TestRunConfig createInitialRunConfig() {
        return new TestRunConfig(
                clientRequests,
                arraySize.iterator().next(),
                clients.iterator().next(),
                requestDelta.iterator().next(),
                rangeField::getValue
        );
    }

    public Architecture getArch() {
        return arch;
    }

    public void setArch(Architecture arch) {
        this.arch = Objects.requireNonNull(arch, "arch");
    }

    public int getClientRequests() {
        return clientRequests;
    }

    public void setClientRequests(int clientRequests) {
        this.clientRequests = clientRequests;
    }

    public Variable<Integer> getArraySize() {
        return arraySize;
    }

    public void setArraySize(Variable<Integer> arraySize) {
        this.arraySize = Objects.requireNonNull(arraySize, "arraySize");
    }

    public Variable<Integer> getClients() {
        return clients;
    }

    public void setClients(Variable<Integer> clients) {
        this.clients = Objects.requireNonNull(clients, "clients");
    }

    public Variable<PrettyDuration> getRequestDelta() {
        return requestDelta;
    }

    public void setRequestDelta(Variable<PrettyDuration> requestDelta) {
        this.requestDelta = Objects.requireNonNull(requestDelta, "requestDelta");
    }

    public Path getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(Path outputDir) {
        this.outputDir = Objects.requireNonNull(outputDir, "outputDir");
    }

    public enum Architecture {

        BLOCK, NON_BLOCK, ASYNC
    }

    private enum Field {

        ARCH("server architecture") {

            @Override
            Object getValue(TestConfig self) {
                return self.arch;
            }
        },

        CLIENT_REQUESTS("number of requests per client") {

            @Override
            Object getValue(TestConfig self) {
                return self.clientRequests;
            }

            @Override
            Integer getValue(TestRunConfig runConfig) {
                return runConfig.clientRequests;
            }

            @Override
            void verify(TestConfig self) {
                if (self.clientRequests <= 0) {
                    throw new IllegalArgumentException("number of requests per client must be positive");
                }
            }
        },

        ARRAY_SIZE("array size") {

            @Override
            Object getValue(TestConfig self) {
                return self.arraySize;
            }

            @Override
            Integer getValue(TestRunConfig runConfig) {
                return runConfig.arraySize;
            }

            @Override
            void verify(TestConfig self) {
                if (self.arraySize.iterator().next() <= 0) {
                    throw new IllegalArgumentException("array size must be positive");
                }
            }

            @Override
            Iterator<TestRunConfig> iterator(TestConfig self) {
                final Variable<Integer> value = self.arraySize;

                if (value.isConstant()) {
                    return null;
                }

                final TestRunConfig initial = self.createInitialRunConfig();

                return new Iterator<TestRunConfig>() {

                    private final Iterator<Integer> it = value.iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public TestRunConfig next() {
                        return initial.withArraySize(it.next());
                    }
                };
            }
        },

        CLIENTS("number of clients") {

            @Override
            Object getValue(TestConfig self) {
                return self.clients;
            }

            @Override
            Integer getValue(TestRunConfig runConfig) {
                return runConfig.clients;
            }

            @Override
            void verify(TestConfig self) {
                if (self.clients.iterator().next() <= 0) {
                    throw new IllegalArgumentException("number of clients must be positive");
                }
            }

            @Override
            Iterator<TestRunConfig> iterator(TestConfig self) {
                final Variable<Integer> value = self.clients;

                if (value.isConstant()) {
                    return null;
                }

                final TestRunConfig initial = self.createInitialRunConfig();

                return new Iterator<TestRunConfig>() {

                    private final Iterator<Integer> it = value.iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public TestRunConfig next() {
                        return initial.withClients(it.next());
                    }
                };
            }
        },

        REQUEST_DELTA("duration between client requests") {

            @Override
            Object getValue(TestConfig self) {
                return self.clientRequests;
            }

            @Override
            PrettyDuration getValue(TestRunConfig runConfig) {
                return runConfig.requestDelta;
            }

            @Override
            void verify(TestConfig self) {
                final PrettyDuration value = self.requestDelta.iterator().next();

                if (value.value.isNegative()) {
                    throw new IllegalArgumentException("duration between client requests must not be negative");
                }
            }

            @Override
            Iterator<TestRunConfig> iterator(TestConfig self) {
                final Variable<PrettyDuration> value = self.requestDelta;

                if (value.isConstant()) {
                    return null;
                }

                final TestRunConfig initial = self.createInitialRunConfig();

                return new Iterator<TestRunConfig>() {

                    private final Iterator<PrettyDuration> it = value.iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public TestRunConfig next() {
                        return initial.withRequestDelta(it.next());
                    }
                };
            }
        },

        OUTPUT_DIR("output directory") {

            @Override
            Object getValue(TestConfig self) {
                return self.outputDir;
            }

            @Override
            void verify(TestConfig self) {
                try {
                    Files.createDirectories(self.outputDir);
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        };

        public final String name;

        Field(String name) {
            this.name = name;
        }

        abstract Object getValue(TestConfig self);

        Object getValue(TestRunConfig runConfig) {
            throw new UnsupportedOperationException("not in run config");
        }

        void verify(TestConfig self) {}

        Iterator<TestRunConfig> iterator(TestConfig self) {
            return null;
        }
    }
}
