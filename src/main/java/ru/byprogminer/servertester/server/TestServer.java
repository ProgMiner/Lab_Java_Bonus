package ru.byprogminer.servertester.server;

import java.io.IOException;
import java.util.List;


public interface TestServer {

    void serve() throws IOException;
    void shutdown() throws IOException, InterruptedException;

    ServerTestResult getTestResult();

    int getPort();
    List<Throwable> getExceptions();
}
