package com.codery.utils.cli;

import java.io.IOException;

/**
 * Created by Thomas on 7/9/2015.
 */
public class ProcessBuilderWrapper {
    private final ProcessBuilder pb;

    public ProcessBuilderWrapper(ProcessBuilder pb) {
        this.pb = pb;
    }

    public Process start() throws IOException {
       return pb.start();
    }
}
