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

    private static final File TES_DIR = new File("src/test/resources/ProcessExecutionTest");


    @Test
    public void shouldReturnZero_WhenProcessIsCorrect() {
        ShellCli cli = new WindowsCli(TES_DIR);
        WindowsCommand cmd = new WindowsCommand("tasklist").param("/?");
        int ret = cli.command(cmd).execute();
        assertThat("The command " + cmd + " should've return 0", ret, is(0));
    }

    @Test
    public void shouldNotReturnZero_WhenProcessIsIncorrect() {
        ShellCli cli = new WindowsCli(TES_DIR);
        WindowsCommand cmd = new WindowsCommand("tasklistr").param("/?");
        int ret = cli.command(cmd).execute();
        assertThat("The command " + cmd + "should've return 1", ret, is(1));
    }

    @Test
    public void shouldDefineEnvironmentVariables_ByExecution() {
        ShellCli cli = new WindowsCli(TES_DIR).setEnvironmentVariable("HOME", System.getProperty("user.home"));
        String expectedVal = System.getProperty("user.home");
        String actualVal = cli.getEnvironentVariables().get("HOME");
        assertThat("Should've setted environment variables.", actualVal, is(expectedVal));
    }

    @Test
    public void shouldRespectTimeout_WhenProvided() {
        long expectedTimeout = 1000L;
        ShellCli cli = new WindowsCli(expectedTimeout, TES_DIR);
        long init = System.currentTimeMillis();
        cli.command(new WindowsCommand("ping").param("8.8.8.8").param("-t")).execute();
        long end = System.currentTimeMillis();
        long elapsed = end - init;
        long errorMargin = 50;

        assertThat("Should've respected configured timeout.", elapsed, lessThanOrEqualTo(expectedTimeout + errorMargin));
    }

    @Test
    public void shouldRespectDefaultTimeout_WhenNoTimeoutIsProvided() {
        //TODO: Tests would take too long to finish. Maybe put timeout tests in another class?
    }

    @Test
    public void shouldRedirectStdOutput_WhenConfiguredSo() {
        OutputStreamSpy spyStd = new OutputStreamSpy(System.out);
        ShellCli cli = new WindowsCli(TES_DIR).addStandardOutput(spyStd);
        String expectedOutput = "OUTPUT TEST";

        WindowsCommand cmd = new WindowsCommand("echo").param(expectedOutput);
        int ret = cli.command(cmd).execute();

        String actualOutput = spyStd.getActualOutput().replace("\"", "").replace("\r", "").replace("\n", "");

        assertThat("Should've redirected the standard output.", actualOutput, is(expectedOutput));
    }


    @Test
    public void shouldRedirectErrOutput_WhenConfiguredSo() {
        OutputStreamSpy spyErr = new OutputStreamSpy(System.err);
        ShellCli cli = new WindowsCli(TES_DIR).addErrorOutput(spyErr);
        String expectedOutput = "The directory name is invalid.";

        WindowsCommand cmd = new WindowsCommand("mkdir").param(":s");
        int ret = cli.command(cmd).execute();

        String actualOutput = spyErr.getActualOutput().replace("\"", "").replace("\r", "").replace("\n", "");

        assertThat("Should've redirected the error output.", actualOutput, is(expectedOutput));
    }

    @Test
    public void pipeProcessTest() {
        ShellCli cli = new WindowsCli(new File("src/test/resources"));
        ShellCommand cmd_1 = new WindowsCommand("tasklist").param("/V").param("/FO", "LIST");
        ShellCommand cmd_2 = new WindowsCommand("findstr").param("PID");
        ShellCommand cmd_3 = new WindowsCommand("findstr").param("1");


        String[] expectedCmd = new String[]{"cmd", "/c", "tasklist", "/V", "/FO", "LIST", "|", "findstr", "PID", "|", "findstr", "1"};
        String[] actualCmd = cli.command(cmd_1).pipe(cmd_2).pipe(cmd_3).getCommand().getCmdLine();

        int ret = cli.command(cmd_1).pipe(cmd_2).pipe(cmd_3).execute();

        assertThat(actualCmd, is(expectedCmd));
        assertThat(ret, is(0));
    }

    @Test
    public void backgroundProcessTest() {
        ShellCli cli = new WindowsCli(new File("src/test/resources"));
        ShellCommand cmd_1 = new WindowsCommand("tasklist").param("/V").param("/FO", "LIST");


        String[] expectedCmd = new String[]{"cmd", "/c", "start", "tasklist", "/V", "/FO", "LIST"};
        String[] actualCmd = cli.command(cmd_1).background().getCommand().getCmdLine();

        int ret = cli.command(cmd_1).background().execute();

        assertThat(actualCmd, is(expectedCmd));
        assertThat(ret, is(0));
    }

    @Test
    public void andProcessTest() {
        ShellCli cli = new WindowsCli(new File("src/test/resources"));
        ShellCommand cmd_1 = new WindowsCommand("tasklist").param("/V").param("/FO", "LIST");
        ShellCommand cmd_2 = new WindowsCommand("findstr").param("PID");
        ShellCommand cmd_3 = new WindowsCommand("findstr").param("1");


        String[] expectedCmd = new String[]{"cmd", "/c", "tasklist", "/V", "/FO", "LIST", "&", "findstr", "PID", "&", "findstr", "1"};
        String[] actualCmd = cli.command(cmd_1).and(cmd_2).and(cmd_3).getCommand().getCmdLine();

        int ret = cli.command(cmd_1).and(cmd_2).and(cmd_3).execute();

        assertThat(actualCmd, is(expectedCmd));
        assertThat(ret, is(0));
    }

    @Test
    public void WindowsCliShouldBeImmutable() {
        ShellCli baseCli = new WindowsCli();
        ShellCli cli = new WindowsCli();
        ShellCli cli2 = cli.dir(new File("src/test/resources"));

        assertThat("Client should not mutate.", cli, equalTo(baseCli));

        cli2 = cli.setEnvironmentVariable("home", System.getProperty("user.home"));

        assertThat("Client should not mutate.", cli, equalTo(baseCli));

        cli.command(new WindowsCommand("tasklist"));

        assertThat("Client should not mutate.", cli, equalTo(baseCli));
    }

    @Test
    public void WindowsCommandShouldBeImmutable() {
        WindowsCommand baseCmd = new WindowsCommand("test");
        WindowsCommand cmd = new WindowsCommand("test");
        WindowsCommand cmd2 = cmd.param("bla").param("p", "a");
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
