package com.codery.utils.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by thomasadriano on 09/07/15.
 */
//TODO: build a "Download executable" option.
public class WindowsCli implements ShellCli {

    private static final Logger LOGGER = LoggerFactory.getLogger(WindowsCli.class);
    private static final String[] CMD_CALL_PARAMS = new String[] { "cmd", "/c" };
    private static final long DEFAULT_TIMEOUT = 300_000; //5 min
    public static final long MAX_TIMEOUT = Long.MAX_VALUE;
    private final long timeout;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final List<OutputStream> stdOutputs = new ArrayList<>();
    private final List<OutputStream> errOutputs = new ArrayList<>();
    private final Map<String, String> environment = new HashMap<>();
    private File dir;

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

    @Override
    public ShellCli setEnvironmentVariable(String key, String value) {
        this.environment.put(key, value);
        return this;
    }

    @Override
    public WindowsCli setEnvironmentVariables(Map<String, String> vars) {
        this.environment.putAll(vars);
        return this;
    }

    @Override
    public Map<String, String> getEnvironentVariables() {
        return Collections.unmodifiableMap(this.environment);
    }

    @Override
    public FutureExecution command(CliCommand cmd) {
        return new WindowsCliFutureExecution(new CliCommand(ArraysUtils.concat(CMD_CALL_PARAMS, cmd.getCmdLine())));
    }

    @Override
    public WindowsCli dir(File dir) {
        this.dir = dir;
        return this;
    }

    @Override
    public WindowsCli addStandardOutput(OutputStream dest) {
        this.stdOutputs.add(dest);
        return this;
    }

    @Override
    public WindowsCli addErrorOutput(OutputStream dest) {
        this.errOutputs.add(dest);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WindowsCli that = (WindowsCli) o;

        if (this.dir == null && that.dir == null && this.environment.equals(that.environment)) {
            return true;
        }

        return (this.dir != null ? this.dir.equals(that.dir) : this.dir == that.dir) && this.environment.equals(that.environment);
    }

    @Override
    public int hashCode() {
        int result = dir != null ? dir.hashCode() : 0;
        result = 31 * result + (environment != null ? environment.hashCode() : 0);
        return result;
    }

    @Override
    public void close() throws Exception {
        closOutputStreams(stdOutputs);
        closOutputStreams(errOutputs);
        executor.shutdown();
    }

    private void closOutputStreams(List<OutputStream> outStreams) {
        for (OutputStream out : outStreams) {
            if (out == System.out || out == System.err) {
                continue;
            }
            try {
                out.close();
            } catch (IOException e) {
                throw new ShellCliException("It wasn't possible to close an outputstream.", e);
            }
        }
    }

    private class WindowsCliFutureExecution implements FutureExecution {

        private final CliCommand cmd;

        WindowsCliFutureExecution(CliCommand cmd) {
            this.cmd = new CliCommand(cmd.getCmdLine());
        }

        @Override
        public int execute() {
            ProcessBuilder pb = setupProcessBuilder();
            Process p = null;

            int ret = -1;
            try {
                if (pb.directory() != null && !pb.directory().exists()) {
                    LOGGER.info("Directory \"" + pb.directory() + "\" don't exist and will be created.");
                    Files.createDirectory(pb.directory().toPath());
                }

                LOGGER.info("Running command \"" + pb.command().toString().replaceAll("\\[|\\]|,", "") + "\" in directory \"" + pb.directory() + "\"");
                p = pb.start();
                startOutputStreamsWriters(p.getInputStream(), p.getErrorStream());
                if (timeout == MAX_TIMEOUT) {
                    ret = p.waitFor();
                } else {
                    ret = waitFor(p, timeout, TimeUnit.MILLISECONDS);
                }
            } catch (IOException | InterruptedException | ShellCliException ex) {
                throw new ShellCliException("An error occurred while trying to execute command \"" + pb.command() + "\" in directory \"" + pb.directory() + "\"", ex);
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
                    if (rem > 0) try {
                        Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
                    } catch (InterruptedException e) {
                        throw new ShellCliException("An error ocurred verifying timeout completion", e);
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
            List<OutputStream> stdOutStreams = new ArrayList<>();
            List<OutputStream> errOutStreams = new ArrayList<>();
            // it is necessarily to read all process's inputStream from std and err or else the execution will hang.
            if (!stdOutputs.isEmpty()) {
                stdOutStreams = stdOutputs;
            } else {
                stdOutStreams.add(new ByteArrayOutputStream());
            }

            if (!errOutputs.isEmpty()) {
                errOutStreams = errOutputs;
            } else {
                errOutStreams.add(new ByteArrayOutputStream());
            }

            executor.submit(new InputStreamReader(stdStream, stdOutStreams));
            executor.submit(new InputStreamReader(errStream, errOutStreams));
        }

        @Override
        public FutureExecution pipe(CliCommand cmd) {
            return new WindowsCliFutureExecution(createNextCmdLine("|", cmd));
        }

        @Override
        public FutureExecution and(CliCommand cmd) {
            return new WindowsCliFutureExecution(createNextCmdLine("&", cmd));
        }

        @Override
        public FutureExecution background() {
            return new WindowsCliFutureExecution(createBackgroundCmdLine());
        }

        @Override
        public CliCommand getCommand() {
            return cmd;
        }

        private CliCommand createBackgroundCmdLine() {
            String[] cmdArr = cmd.getCmdLine();
            String[] prefixCmdArr = new String[] { "start" };
            if (cmd.getCmdLine()[0].equalsIgnoreCase("cmd")) {
                cmdArr = ArraysUtils.slice(cmdArr, 1);
                prefixCmdArr = ArraysUtils.concat(CMD_CALL_PARAMS, prefixCmdArr);
            }
            return new CliCommand(ArraysUtils.concat(prefixCmdArr, cmdArr));
        }

        private CliCommand createNextCmdLine(String param, CliCommand cmd) {
            return new CliCommand(ArraysUtils.concat(createPrevCmdLine(param), cmd.getCmdLine()));
        }

        private String[] createPrevCmdLine(String param) {
            return ArraysUtils.concat(cmd.getCmdLine(), new String[] { param });
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
            byte[] buffer = new byte[1024];

            try {
                while (inStream.read(buffer) != -1) {
                    byte[] processedBuffer = trimBytes(buffer);
                    for (OutputStream out : outStreams) {
                        out.write(processedBuffer);
                    }
                }

            } catch (IOException e) {
                throw new ShellCliException("An error occurred trying to write into the output streams.", e);
            } finally {
                flushOutputStreams();
            }
        }

        private void flushOutputStreams() {
            for (OutputStream out : outStreams) {
                try {
                    out.flush();
                } catch (IOException e) {
                    throw new ShellCliException("It wasn't possible to flush an outputstream.", e);
                }
            }
        }

        private byte[] trimBytes(byte[] bytes) {
            if (bytes[bytes.length - 1] != 0) {
                return bytes;
            }

            int i = bytes.length - 1;
            while (i >= 0 && bytes[i] == 0) {
                --i;
            }

            return Arrays.copyOf(bytes, i + 1);
        }

    }

}
