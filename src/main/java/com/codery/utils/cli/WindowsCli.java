package com.codery.utils.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindowsCli implements ShellCli {

    private static final String[] CMD_CALL_PARAMS = new String[] { "cmd", "/c" };
    private static final Logger LOGGER = LoggerFactory.getLogger(WindowsCli.class);
    protected static final long DEFAULT_TIMEOUT = 300_000; //5 min
    public static final long MAX_TIMEOUT = Long.MAX_VALUE;
    protected final long timeout;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final List<OutputStream> stdOutputs = new ArrayList<>();
    private final List<OutputStream> errOutputs = new ArrayList<>();
    private final Map<String, String> environment = new HashMap<>();
    protected File dir;
    private boolean isClosed;

    public WindowsCli() {
        this(-1, new File(System.getProperty("user.dir")));
    }

    public WindowsCli(long timeout) {
        this(timeout, new File(System.getProperty("user.dir")));
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
    public ShellCli clearStandardOutputTargets() {
        stdOutputs.removeAll(stdOutputs);
        return this;
    }

    @Override
    public ShellCli clearErrorOutputTargets() {
        errOutputs.removeAll(errOutputs);
        return this;
    }

    @Override
    public WindowsCli setEnvironmentVariable(String key, String value) {
        secureEnvStore(environment, key, value);
        return this;
    }

    @Override
    public WindowsCli setEnvironmentVariables(Map<String, String> vars) {
        for (Entry<String, String> each : vars.entrySet()) {
            secureEnvStore(environment, each.getKey(), each.getValue());
        }
        return this;
    }

    @Override
    public Map<String, String> getEnvironentVariables() {
        return Collections.unmodifiableMap(environment);
    }

    private void secureEnvStore(Map<String, String> target, String key, String value) {
        boolean found = false;
        for (String each : target.keySet()) {
            if (each.equalsIgnoreCase(key)) {
                found = true;
                target.put(each, value);
            }
        }
        if (!found) {
            target.put(key, value);
        }
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
        stdOutputs.add(dest);
        return this;
    }

    @Override
    public WindowsCli addErrorOutput(OutputStream dest) {
        errOutputs.add(dest);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WindowsCli that = (WindowsCli) o;

        if (dir == null && that.dir == null && environment.equals(that.environment)) {
            return true;
        }

        return (dir != null ? dir.equals(that.dir) : dir == that.dir) && environment.equals(that.environment);
    }

    @Override
    public int hashCode() {
        int result = dir != null ? dir.hashCode() : 0;
        result = 31 * result + (environment != null ? environment.hashCode() : 0);
        return result;
    }

    @Override
    public void close() throws Exception {
        if (isClosed) {
            throw new RuntimeException("This instance of " + WindowsCli.class + " is already closed.");
        }
        closOutputStreams(stdOutputs);
        closOutputStreams(errOutputs);
        executor.shutdown();
        isClosed = true;
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
        private File redirectOutputTarget;
        private boolean supressOuput;

        WindowsCliFutureExecution(CliCommand cmd) {
            this.cmd = new CliCommand(cmd.getCmdLine());
        }

        WindowsCliFutureExecution(WindowsCliFutureExecution futureEx) {
            cmd = new CliCommand(futureEx.cmd);
            redirectOutputTarget = futureEx.redirectOutputTarget;
            supressOuput = futureEx.supressOuput;
        }

        @Override
        public int execute() {
            if (isClosed) {
                throw new RuntimeException("It is not possible to execute a closed instance of " + WindowsCli.class);
            }

            ProcessBuilder pb = setupProcessBuilder();
            Process p = null;

            int ret = -1;
            try {
                if (pb.directory() != null && !pb.directory().exists()) {
                    LOGGER.info("Directory \"" + pb.directory() + "\" don't exist and will be created.");
                    Files.createDirectory(pb.directory().toPath());
                }

                LOGGER.debug("Running command \"" + pb.command().toString().replaceAll("\\[|\\]|,", "") + "\" in directory \"" + pb.directory() + "\"");
                p = pb.start();

                //the default output is not used. Closing it prevents some processes from hanging (all powershell executions, for example).
                p.getOutputStream().close();

                if (redirectOutputTarget == null) {
                    startOutputStreamsWriters(p.getInputStream(), p.getErrorStream());
                } else {
                    FileInputStream fis = new FileInputStream(redirectOutputTarget);
                    startOutputStreamsWriters(fis, null);
                }

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
                    if (rem > 0) {
                        try {
                            Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
                        } catch (InterruptedException e) {
                            throw new ShellCliException("An error ocurred verifying timeout completion", e);
                        }
                    }
                }
                rem = unit.toNanos(timeout) - (System.nanoTime() - startTime);
            } while (rem > 0);
            return -1;
        }

        private ProcessBuilder setupProcessBuilder() {
            ProcessBuilder pb = new ProcessBuilder(cmd.getCmdLine());

            for (Entry<String, String> each : environment.entrySet()) {
                secureEnvStore(pb.environment(), each.getKey(), each.getValue());
            }

            pb = pb.directory(dir);
            return pb;
        }

        private void startOutputStreamsWriters(InputStream stdStream, InputStream errStream) {
            List<OutputStream> stdOutStreams = new ArrayList<>();
            List<OutputStream> errOutStreams = new ArrayList<>();
            // it is necessarily to read all process's inputStream from std and err or else the execution will hang.
            if (!stdOutputs.isEmpty() && !supressOuput) {
                stdOutStreams = stdOutputs;
            } else {
                stdOutStreams.add(new ByteArrayOutputStream());
            }

            if (!errOutputs.isEmpty() && !supressOuput) {
                errOutStreams = errOutputs;
            } else {
                errOutStreams.add(new ByteArrayOutputStream());
            }

            if (stdStream != null) {
                executor.submit(new InputStreamReader(stdStream, stdOutStreams));
            }
            if (errStream != null) {
                executor.submit(new InputStreamReader(errStream, errOutStreams));
            }
        }

        @Override
        public FutureExecution redirectOutput(File out) {
            WindowsCliFutureExecution ret = new WindowsCliFutureExecution(createNextCmdLine(">", out.getAbsolutePath()));
            ret.redirectOutputTarget = out;
            return ret;
        }

        @Override
        public FutureExecution redirectOutputAppending(File out) {
            WindowsCliFutureExecution ret = new WindowsCliFutureExecution(createNextCmdLine(">>", out.getAbsolutePath()));
            ret.redirectOutputTarget = out;
            return ret;
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

        private CliCommand createNextCmdLine(String param, String param2) {
            return new CliCommand(ArraysUtils.concat(createPrevCmdLine(param), new String[] { param2 }));
        }

        private String[] createPrevCmdLine(String param) {
            return ArraysUtils.concat(cmd.getCmdLine(), new String[] { param });
        }

        @Override
        public String toString() {
            return Arrays.toString(cmd.getCmdLine());
        }

        @Override
        public FutureExecution supressOutput() {
            WindowsCliFutureExecution ret = new WindowsCliFutureExecution(this);
            ret.supressOuput = true;
            return ret;
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
            try {
                readInputStream();
            } catch (IOException e) {
                throw new ShellCliException("An error occurred trying to write into the output streams.", e);
            } finally {
                flushOutputStreams();
                closeInputStream();
            }
        }

        private synchronized void readInputStream() throws IOException {
            byte[] buffer = new byte[1024];
            while (inStream.read(buffer) != -1) {
                byte[] processedBuffer = trimBytes(buffer);
                for (OutputStream out : outStreams) {
                    out.write(processedBuffer);
                }
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

        private void closeInputStream() {
            try {
                inStream.close();
            } catch (IOException e) {
                throw new ShellCliException("It wasn't possible to close an inputstream.", e);
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
