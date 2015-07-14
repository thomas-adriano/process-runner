package com.codery.utils.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by thomasadriano on 09/07/15.
 */
//TODO: build a "Download executable" option.
public class WindowsCli implements ShellCli {

    private static final Logger LOGGER = LoggerFactory.getLogger(WindowsCli.class);
    private final static String[] CMD_CALL_PARAMS = new String[]{"cmd", "/c"};
    private File dir;
    private final Map<String, String> environment = new HashMap<>();
    private final List<OutputStream> stdOutputs = new ArrayList<>();
    private final List<OutputStream> errOutputs = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final int DEFAULT_TIMEOUT = 300_000;
    private final long timeout;

    public WindowsCli() {
        this(-1, (File) null);
    }

    public WindowsCli(long timeout) {
        this(timeout, (File) null);
    }

    public WindowsCli(File dir) {
        this(-1, dir);
    }

    public WindowsCli(long timeout, File dir) {
        if (timeout < 0) {
            this.timeout = DEFAULT_TIMEOUT;
        } else {
            this.timeout = timeout;
        }

        this.dir = dir;
    }

    private WindowsCli(WindowsCli cli) {
        this.dir = cli.dir;
        this.errOutputs.addAll(cli.errOutputs);
        this.stdOutputs.addAll(cli.stdOutputs);
        this.environment.putAll(cli.environment);
        this.timeout = cli.timeout;
    }

    @Override
    public ShellCli setEnvironmentVariable(String key, String value) {
        WindowsCli result = new WindowsCli(this);
        result.environment.put(key, value);
        return result;
    }

    @Override
    public FutureExecution command(ShellCommand cmd) {
        return new WindowsCliFutureExecution(new WindowsCommand(ArraysUtils.concat(CMD_CALL_PARAMS, cmd.getCmdLine())));
    }

    @Override
    public WindowsCli dir(File dir) {
        WindowsCli result = new WindowsCli(this);
        result.dir = dir;
        return result;
    }

    public WindowsCli addStandardOutputConsumer(OutputStream dest) {
        WindowsCli result = new WindowsCli(this);
        result.stdOutputs.addAll(this.stdOutputs);
        result.stdOutputs.add(dest);
        return result;
    }

    public WindowsCli addErrorOutputConsumer(OutputStream dest) {
        WindowsCli result = new WindowsCli(this);
        result.errOutputs.addAll(this.errOutputs);
        result.errOutputs.add(dest);
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
            ProcessBuilder pb = setupProcessBuilder();
            Process p = null;

            int ret = -1;
            try {
                if (!pb.directory().exists()) {
                    LOGGER.info("Directory \"" + pb.directory() + "\" don't exist and will be created.");
                    Files.createDirectory(pb.directory().toPath());
                }

                LOGGER.info("Running command \"" + pb.command() + "\" in directory \"" + pb.directory() + "\"");
                p = pb.start();
                startOutputStreamsWriters(p.getInputStream(), p.getErrorStream());
                ret = waitFor(p, timeout, TimeUnit.MILLISECONDS);
            } catch (IOException ex) {
                throw new ShellCliException("An error ocurred while trying to execute command \"" + pb.command() + "\" in directory \"" + pb.directory() + "\"", ex);
            } finally {
                executor.shutdown();
            }
            return ret;
        }

        private int waitFor(Process p, long timeout, TimeUnit unit) {
            long startTime = System.nanoTime();
            long rem = unit.toNanos(timeout);

            do {
                try {
                    return p.exitValue();
                } catch (IllegalThreadStateException ex) {
                    if (rem > 0)
                        try {
                            Thread.sleep(
                                    Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                }
                rem = unit.toNanos(timeout) - (System.nanoTime() - startTime);
            } while (rem > 0);
            return -1;
        }

        private ProcessBuilder setupProcessBuilder() {
            ProcessBuilder pb = new ProcessBuilder(cmd.getCmdLine());
            pb.environment().putAll(environment);
            pb.directory(dir);
            return pb;
        }

        private void startOutputStreamsWriters(InputStream stdStream, InputStream errStream) {
            executor.submit(new InputStreamReader(stdStream, stdOutputs));
            executor.submit(new InputStreamReader(errStream, errOutputs));
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

    private static final class InputStreamReader implements Runnable {

        private final List<OutputStream> outStreams;
        private final InputStream inStream;

        public InputStreamReader(InputStream inStream, List<OutputStream> outStreams) {
            this.inStream = inStream;
            this.outStreams = outStreams;
        }

        @Override
        public void run() {
            for (OutputStream out : outStreams) {
                byte[] buffer = new byte[1024];
                try {
                    inStream.read(buffer);
                    out.write(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
