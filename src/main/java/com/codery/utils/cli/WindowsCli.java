package com.codery.utils.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

/**
 * Created by thomasadriano on 09/07/15.
 */
public class WindowsCli implements ShellCli {

    private final ProcessBuilder pb;
    private final static String[] CMD_CALL_PARAMS = new String[]{"cmd", "/c"};

    public WindowsCli() {
        this.pb = new ProcessBuilder();
    }

    public WindowsCli(File dir) {
        this.pb = new ProcessBuilder();
        pb.directory(dir);
        pb.command("cmd", "/c");
    }

    private WindowsCli(WindowsCli cli) {
        this.pb = copyProcessBuilder(cli.pb);
    }

    private ProcessBuilder copyProcessBuilder(ProcessBuilder pb) {
        ProcessBuilder result = new ProcessBuilder();
        result.command(pb.command());
        result.directory(pb.directory());
        result.redirectError(pb.redirectError());
        result.redirectErrorStream(pb.redirectErrorStream());
        result.redirectInput(pb.redirectInput());
        syncEnvVariables(pb, result);
        return result;
    }

    private void syncEnvVariables(ProcessBuilder subject, ProcessBuilder target) {
        for (String key : subject.environment().keySet()) {
            String thisVal = target.environment().get(key);
            String otherVal = subject.environment().get(key);
            if (thisVal == null || !thisVal.equalsIgnoreCase(otherVal)) {
                target.environment().put(key, otherVal);
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
        ProcessBuilder pb = copyProcessBuilder(this.pb);
        pb.command(ArraysUtils.concat(CMD_CALL_PARAMS, cmd.getParams()));
        Process p = null;

        int ret = -1;
        try {
            if (!pb.directory().exists()) {
                Files.createDirectory(pb.directory().toPath());
            }

            p = pb.start();
            ret = p.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new ShellCliException("An error ocurred while trying to execute command " + pb.command());
        }
        return ret;
    }

    @Override
    public WindowsCli dir(File dir) {
        WindowsCli result = new WindowsCli(this);
        result.pb.directory(dir);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WindowsCli that = (WindowsCli) o;

        return pb != null ? isEquals(pb, that.pb) : false;
    }

    private boolean isEquals(ProcessBuilder one, ProcessBuilder two) {
        return one.command().equals(two.command()) &&
                one.directory().equals(two.directory()) &&
                one.redirectError().equals(two.redirectError()) &&
                one.redirectInput().equals(two.redirectInput()) &&
                one.environment().equals(two.environment());
    }

    @Override
    public int hashCode() {
        return pb != null ? pb.hashCode() : 0;
    }
}
