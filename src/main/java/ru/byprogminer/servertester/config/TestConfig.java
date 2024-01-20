package ru.byprogminer.servertester.config;

import ru.byprogminer.servertester.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;


public class TestConfig {

    private Architecture arch;
    private Integer clientRequests;

    private Variable<Integer> arraySize;
    private Variable<Integer> clients;
    private Variable<Duration> requestDelta;

    private Path outputDir;

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
            }

            if (clients.isConstant()) {
                --variables;
            }

            if (requestDelta.isConstant()) {
                --variables;
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

    public Variable<Duration> getRequestDelta() {
        return requestDelta;
    }

    public void setRequestDelta(Variable<Duration> requestDelta) {
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

            @Override
            void verify(TestConfig self) {}
        },

        CLIENT_REQUESTS("number of requests per client") {

            @Override
            Object getValue(TestConfig self) {
                return self.clientRequests;
            }

            @Override
            void verify(TestConfig self) {
                if (self.clientRequests <= 0) {
                    throw new IllegalArgumentException("number of requests per client must be positive");
                }
            }
        },

        ARRAY_SIZE("size of array to sort") {

            @Override
            Object getValue(TestConfig self) {
                return self.arraySize;
            }

            @Override
            void verify(TestConfig self) {
                if (self.arraySize.iterator().next() <= 0) {
                    throw new IllegalArgumentException("array size must be positive");
                }
            }
        },

        CLIENTS("number of clients") {

            @Override
            Object getValue(TestConfig self) {
                return self.clients;
            }

            @Override
            void verify(TestConfig self) {
                if (self.clients.iterator().next() <= 0) {
                    throw new IllegalArgumentException("number of clients must be positive");
                }
            }
        },

        REQUEST_DELTA("duration between client requests") {

            @Override
            Object getValue(TestConfig self) {
                return self.clientRequests;
            }

            @Override
            void verify(TestConfig self) {
                final Duration value = self.requestDelta.iterator().next();

                if (value.isNegative() || value.isZero()) {
                    throw new IllegalArgumentException("duration between client requests must be positive");
                }
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
        abstract void verify(TestConfig self);
    }
}
