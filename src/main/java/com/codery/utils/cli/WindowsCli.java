package com.codery.utils.cli;

import java.io.File;
import java.util.Map;

/**
 * Created by thomasadriano on 09/07/15.
 */
public class WindowsCli implements ShellCli {

    private final ProcessBuilder pb;

    public WindowsCli() {
        this.pb = new ProcessBuilder();
    }

    public WindowsCli(File dir) {
        this.pb = new ProcessBuilder();
        pb.directory(dir);
    }

    private WindowsCli(WindowsCli cli) {
        this.pb = new ProcessBuilder();
        pb.command(cli.pb.command());
        pb.directory(cli.pb.directory());
        pb.redirectError(cli.pb.redirectError());
        pb.redirectErrorStream(cli.pb.redirectErrorStream());
        pb.redirectInput(cli.pb.redirectInput());
        syncEnvVariables(cli);
    }

    private void syncEnvVariables(WindowsCli cli) {
        for (String key : cli.pb.environment().keySet()) {
            String thisVal = pb.environment().get(key);
            String otherVal = cli.pb.environment().get(key);
            if (thisVal == null || !thisVal.equalsIgnoreCase(otherVal)) {
                pb.environment().put(key, otherVal);
            }
        }
    }

    @Override
    public ShellCli setEnvorinmentVariable(String key, String value) {
        WindowsCli result = new WindowsCli(this);
        result.pb.environment().put(key, value);
        return result;
    }

    @Override
    public int execute(ShellCommand cmd) {
        return cmd.execute(pb);
    }

    @Override
    public WindowsCli dir(File dir) {
        WindowsCli result = new WindowsCli(this);
        result.pb.directory(dir);
        return result;
    }

}
