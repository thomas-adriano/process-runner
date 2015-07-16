package com.codery.utils.cli;

import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;


import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by thomasadriano on 09/07/15.
 */
public class ProcessExecutionTest {

    private static final File TEST_DIR = new File("src/test/resources/ProcessExecutionTest");

    @Test
    public void oneWindowsCli_ShouldSupportMultipleExecutions() throws Exception {
        try (ShellCli cli = new WindowsCli(TEST_DIR).addStandardOutput(System.out).addErrorOutput(System.err)) {
            int ret_1 = cli.command(new CliCommand("tasklist").param("/?")).execute();
            int ret_2 = cli.command(new CliCommand("tasklist").param("/?")).execute();
            int ret_3 = cli.command(new CliCommand("tasklist").param("/?")).execute();

            assertThat(ret_1, is(0));
            assertThat(ret_2, is(0));
            assertThat(ret_3, is(0));
        }
    }

    @Test
    public void shouldReturnZero_WhenProcessIsCorrect() throws Exception {
        try (ShellCli cli = new WindowsCli(TEST_DIR)) {
            CliCommand cmd = new CliCommand("tasklist").param("/?");
            int ret = cli.command(cmd).execute();
            assertThat("The command " + cmd + " should've return 0", ret, is(0));
        }
    }

    @Test
    public void shouldNotReturnZero_WhenProcessIsIncorrect() throws Exception {
        try (ShellCli cli = new WindowsCli(TEST_DIR)) {
            CliCommand cmd = new CliCommand("tasklistr").param("/?");
            int ret = cli.command(cmd).execute();
            assertThat("The command " + cmd + "should've return 1", ret, is(1));

        }
    }

    @Test
    public void shouldDefineEnvironmentVariables_ByExecution() throws Exception {
        try (ShellCli cli = new WindowsCli(TEST_DIR).setEnvironmentVariable("HOME", System.getProperty("user.home"))) {
            String expectedVal = System.getProperty("user.home");
            String actualVal = cli.getEnvironentVariables().get("HOME");
            assertThat("Should've setted environment variables.", actualVal, is(expectedVal));
        }
    }

    @Test
    public void shouldRespectTimeout_WhenProvided() throws Exception {
        long expectedTimeout = 1000L;
        try (ShellCli cli = new WindowsCli(expectedTimeout, TEST_DIR)) {
            long init = System.currentTimeMillis();
            cli.command(new CliCommand("ping").param("8.8.8.8").param("-t")).execute();
            long end = System.currentTimeMillis();
            long elapsed = end - init;
            long errorMargin = 50;

            assertThat("Should've respected configured timeout.", elapsed, lessThanOrEqualTo(expectedTimeout + errorMargin));
        }
    }

    @Test
    public void shouldRespectDefaultTimeout_WhenNoTimeoutIsProvided() {
        //TODO: Tests would take too long to finish. Maybe put timeout tests in another class?
    }

    @Test
    public void shouldRedirectStdOutput_WhenConfiguredSo() throws Exception {
        OutputStreamSpy spyStd = new OutputStreamSpy(System.out);
        try (ShellCli cli = new WindowsCli(TEST_DIR).addStandardOutput(spyStd)) {
            String expectedOutput = "OUTPUT TEST";

            CliCommand cmd = new CliCommand("echo").param(expectedOutput);
            int ret = cli.command(cmd).execute();

            String actualOutput = spyStd.getActualOutput().replace("\"", "").replace("\r", "").replace("\n", "");

            assertThat("Should've redirected the standard output.", actualOutput, is(expectedOutput));
        }
    }


    @Test
    public void shouldRedirectErrOutput_WhenConfiguredSo() throws Exception {
        OutputStreamSpy spyErr = new OutputStreamSpy(System.err);
        try (ShellCli cli = new WindowsCli(TEST_DIR).addErrorOutput(spyErr)) {
            String expectedOutput = "The directory name is invalid.";

            CliCommand cmd = new CliCommand("mkdir").param(":s");
            int ret = cli.command(cmd).execute();

            String actualOutput = spyErr.getActualOutput().replace("\"", "").replace("\r", "").replace("\n", "");

            assertThat("Should've redirected the error output.", actualOutput, is(expectedOutput));
        }
    }

    @Test
    public void pipeProcessTest() throws Exception {
        try (ShellCli cli = new WindowsCli(new File("src/test/resources"))) {
            CliCommand cmd_1 = new CliCommand("tasklist").param("/V").param("/FO", "LIST");
            CliCommand cmd_2 = new CliCommand("findstr").param("PID");
            CliCommand cmd_3 = new CliCommand("findstr").param("1");


            String[] expectedCmd = new String[]{"cmd", "/c", "tasklist", "/V", "/FO", "LIST", "|", "findstr", "PID", "|", "findstr", "1"};
            String[] actualCmd = cli.command(cmd_1).pipe(cmd_2).pipe(cmd_3).getCommand().getCmdLine();

            int ret = cli.command(cmd_1).pipe(cmd_2).pipe(cmd_3).execute();

            assertThat(actualCmd, is(expectedCmd));
            assertThat(ret, is(0));
        }
    }

    @Test
    public void backgroundProcessTest() throws Exception {
        try (ShellCli cli = new WindowsCli(new File("src/test/resources"))) {
            CliCommand cmd_1 = new CliCommand("tasklist").param("/V").param("/FO", "LIST");


            String[] expectedCmd = new String[]{"cmd", "/c", "start", "tasklist", "/V", "/FO", "LIST"};
            String[] actualCmd = cli.command(cmd_1).background().getCommand().getCmdLine();

            int ret = cli.command(cmd_1).background().execute();

            assertThat(actualCmd, is(expectedCmd));
            assertThat(ret, is(0));
        }
    }

    @Test
    public void andProcessTest() throws Exception {
        try (ShellCli cli = new WindowsCli(TEST_DIR)) {
            CliCommand cmd_1 = new CliCommand("tasklist").param("/V").param("/FO", "LIST");
            CliCommand cmd_2 = new CliCommand("tasklist").param("/?");
            CliCommand cmd_3 = new CliCommand("tasklist").param("/V");


            String[] expectedCmd = new String[]{"cmd", "/c", "tasklist", "/V", "/FO", "LIST", "&", "tasklist", "/?", "&", "tasklist", "/V"};
            String[] actualCmd = cli.command(cmd_1).and(cmd_2).and(cmd_3).getCommand().getCmdLine();

            int ret = cli.command(cmd_1).and(cmd_2).and(cmd_3).execute();
            assertThat(actualCmd, is(expectedCmd));
            assertThat(ret, is(0));
        }
    }

    @Test
    public void WindowsCliShouldBeImmutable() throws Exception {
        try (ShellCli baseCli = new WindowsCli()) {
            ShellCli cli = new WindowsCli();
            ShellCli cli2 = cli.dir(new File("src/test/resources"));

            assertThat("Client should not mutate.", cli, equalTo(baseCli));

            cli2 = cli.setEnvironmentVariable("home", System.getProperty("user.home"));

            assertThat("Client should not mutate.", cli, equalTo(baseCli));

            cli.command(new CliCommand("tasklist"));

            assertThat("Client should not mutate.", cli, equalTo(baseCli));
        }
    }

    @Test
    public void WindowsCommandShouldBeImmutable() {
        CliCommand baseCmd = new CliCommand("test");
        CliCommand cmd = new CliCommand("test");
        CliCommand cmd2 = cmd.param("bla").param("p", "a");
        assertThat("Commands should not mutate.", cmd, is(baseCmd));
    }

    private class OutputStreamSpy extends BufferedOutputStream {

        private final StringBuilder actualOutput = new StringBuilder();

        public OutputStreamSpy(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] buff) throws IOException {
            super.write(buff);
            actualOutput.append(new String(buff));
        }

        public String getActualOutput() {
            return actualOutput.toString();
        }
    }


}
