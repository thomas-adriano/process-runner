package com.codery.utils.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by thomasadriano on 09/07/15.
 */
public class WindowsCli implements ShellCli {

    private static final Logger LOGGER = LoggerFactory.getLogger(WindowsCli.class);
    private final static String[] CMD_CALL_PARAMS = new String[]{"cmd", "/c"};
    private File dir;
    private final Map<String, String> environment = new HashMap<>();

    public WindowsCli() {
        this((File) null);
    }

    public WindowsCli(File dir) {
        this.dir = dir;
    }

    private WindowsCli(WindowsCli cli) {
        this.dir = cli.dir;
        syncEnvVariables(cli.environment);
    }

    private void syncEnvVariables(Map<String, String> that) {
        for (String key : that.keySet()) {
            String thisVal = this.environment.get(key);
            String otherVal = that.get(key);
            this.environment.put(key, otherVal);
        }
    }

    @Override
    public ShellCli setEnvironmentVariable(String key, String value) {
        WindowsCli result = new WindowsCli(this);
        environment.put(key, value);
        return result;
    }

    @Override
    public FutureExecution command(ShellCommand cmd) {
        return new WindowsCliFutureExecution(new WindowsCommand(ArraysUtils.concat(CMD_CALL_PARAMS, cmd.getCmdLine())));
    }

    @Override
    public WindowsCli dir(File dir) {
        WindowsCli result = new WindowsCli(this);
        this.dir = dir;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WindowsCli that = (WindowsCli) o;

        if (this.dir == null && that.dir == null && this.environment.equals(that.environment)) {
            return true;
        }

        return (this.dir != null ? this.dir.equals(that.dir) : this.dir == that.dir) &&
                this.environment.equals(that.environment);
    }

    @Override
    public int hashCode() {
        int result = dir != null ? dir.hashCode() : 0;
        result = 31 * result + (environment != null ? environment.hashCode() : 0);
        return result;
    }

    private class WindowsCliFutureExecution implements FutureExecution {

        private final ShellCommand cmd;

        WindowsCliFutureExecution(WindowsCommand cmd) {
            this.cmd = new WindowsCommand(cmd.getCmdLine());
        }


        @Override
        public int execute() {
            ProcessBuilder pb = new ProcessBuilder(cmd.getCmdLine());
            pb.directory(dir);
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
            return new WindowsCliFutureExecution(createNextCmdLine("|", cmd));
        }

        @Override
        public FutureExecution and(ShellCommand cmd) {
            return new WindowsCliFutureExecution(createNextCmdLine("&", cmd));
        }

        @Override
        public FutureExecution background() {
            return new WindowsCliFutureExecution(createBackgroundCmdLine());
        }

        @Override
        public ShellCommand getCommand() {
            return cmd;
        }

        private WindowsCommand createBackgroundCmdLine() {
            String[] cmdArr = cmd.getCmdLine();
            String[] prefixCmdArr = new String[]{"start"};
            if (cmd.getCmdLine()[0].equalsIgnoreCase("cmd")) {
                cmdArr = ArraysUtils.slice(cmdArr, 1);
                prefixCmdArr = ArraysUtils.concat(CMD_CALL_PARAMS, prefixCmdArr);
            }
            return new WindowsCommand(ArraysUtils.concat(prefixCmdArr, cmdArr));
        }

        private WindowsCommand createNextCmdLine(String param, ShellCommand cmd) {
            return new WindowsCommand(ArraysUtils.concat(createPrevCmdLine(param), cmd.getCmdLine()));
        }

        private String[] createPrevCmdLine(String param) {
            return ArraysUtils.concat(cmd.getCmdLine(), new String[]{param});
        }

        @Override
        public String toString() {
            return Arrays.toString(cmd.getCmdLine());
        }

    }

}
