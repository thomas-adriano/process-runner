package com.codery.utils.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by thomasadriano on 09/07/15.
 */
//TODO: build a "Download executable" option.
public class WindowsCli implements ShellCli {

    private static final Logger LOGGER = LoggerFactory.getLogger(WindowsCli.class);
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
    public FutureExecution command(ShellCommand cmd) {
        return new WindowsCliFutureExecution(this.pb, cmd);
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

        if (this.pb == null && that.pb == null) {
            return true;
        }

        return pb != null ? isProcessBuilderEquals(pb, that.pb) : false;
    }

    private boolean isProcessBuilderEquals(ProcessBuilder one, ProcessBuilder two) {
        return (one.command() != null ? one.command().equals(two.command()) : one.command() == two.command()) &&
                (one.directory() != null ? one.directory().equals(two.directory()) : one.directory() == two.directory()) &&
                (one.redirectError() != null ? one.redirectError().equals(two.redirectError()) : one.redirectError() == two.redirectError()) &&
                (one.redirectInput() != null ? one.redirectInput().equals(two.redirectInput()) : one.redirectInput() == two.redirectInput()) &&
                (one.environment() != null ? one.environment().equals(two.environment()) : one.environment() == two.environment());
    }

    @Override
    public int hashCode() {
        return pb != null ? pb.hashCode() : 0;
    }

    private class WindowsCliFutureExecution implements FutureExecution {

        private final ProcessBuilder innerPb;
        private final ShellCommand cmd;

        WindowsCliFutureExecution(ProcessBuilder innerPb, ShellCommand cmd) {
            this.innerPb = innerPb;
            this.cmd = cmd;
        }


        @Override
        public int execute() {
            ProcessBuilder pb = copyProcessBuilder(this.innerPb);
            pb.command(ArraysUtils.concat(CMD_CALL_PARAMS, cmd.getCmdLine()));
            Process p = null;

            int ret = -1;
            try {
                if (!pb.directory().exists()) {
                    LOGGER.info("Directory \"" + pb.directory() + "\" don't exist and will be created.");
                    Files.createDirectory(pb.directory().toPath());
                }

                LOGGER.info("Running command \"" + pb.command() + "\" in directory \"" + pb.directory() + "\"");
                p = pb.start();
                ret = p.waitFor();
            } catch (IOException | InterruptedException e) {
                throw new ShellCliException("An error ocurred while trying to execute command \"" + pb.command() + "\" in directory \"" + pb.directory() + "\"");
            }
            return ret;
        }


        @Override
        public FutureExecution pipe(ShellCommand cmd) {
            return new WindowsCliFutureExecution(innerPb, createNextCmdLine("|", cmd));
        }

        @Override
        public FutureExecution background(ShellCommand cmd) {
            return new WindowsCliFutureExecution(innerPb, createNextCmdLine("&&", cmd));
        }

        private ShellCommand createNextCmdLine(String param, ShellCommand cmd) {
            return new WindowsCommand(ArraysUtils.concat(createPrevCmdLine(param), cmd.getCmdLine()));
        }

        private String[] createPrevCmdLine(String param) {
            return ArraysUtils.concat(innerPb.command().toArray(new String[0]), new String[]{param});
        }


    }

}
