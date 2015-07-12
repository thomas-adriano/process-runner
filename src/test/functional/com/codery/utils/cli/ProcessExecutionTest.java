package com.codery.utils.cli;

import org.junit.Test;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by thomasadriano on 09/07/15.
 */
public class ProcessExecutionTest {

    @Test
    public void shouldReturnZero_WhenProcessIsCorrect() {
        ShellCli cli = new WindowsCli(new File("src/test/resources/ProcessExecutionTest"));
        WindowsCommand cmd = new WindowsCommand("tasklist").param("/?");
        int ret = cli.command(cmd).execute();
        assertThat("The command " + cmd + " should've return 0", ret, is(0));
    }

    @Test
    public void shouldNotReturnZero_WhenProcessIsIncorrect() {
        ShellCli cli = new WindowsCli(new File("src/test/resources/ProcessExecutionTest"));
        WindowsCommand cmd = new WindowsCommand("tasklistr").param("/?");
        int ret = cli.command(cmd).execute();
        assertThat("The command " + cmd + "should've return 1", ret, is(1));
    }

    @Test
    public void shouldDefineEnvironmentVariables_ByExecution() {

    }

    @Test
    public void fluentApiInterface_Test() {
        ShellCli cli = new WindowsCli(new File("src/test/resources"));
        ShellCommand cmd_1 = new WindowsCommand("tasklist").param("/V").param("/FO", "LIST");
        ShellCommand cmd_2 = new WindowsCommand("findstr").param("PID");

        String[] expectedCmd = new String[]{"cmd","/c","tasklist", "/V", "/FO", "LIST", "|", "findstr", "PID"};
        String[] actualCmd = cli.command(cmd_1).pipe(cmd_2).getCommand().getCmdLine();

        int ret = cli.command(cmd_1).pipe(cmd_2).execute();

        assertThat(actualCmd, is(expectedCmd));
        assertThat(ret, is(0));
    }

    @Test
    public void WindowsCliShouldBeImmutable() {
        ShellCli baseCli = new WindowsCli();
        ShellCli cli = new WindowsCli();
        ShellCli cli2 = cli.dir(new File("src/test/resources"));
        assertThat(cli, equalTo(baseCli));
    }

    @Test
    public void WindowsCommandShouldBeImmutable() {
        WindowsCommand baseCmd = new WindowsCommand("test");
        WindowsCommand cmd = new WindowsCommand("test");
        WindowsCommand cmd2 = cmd.param("bla").param("p", "a");
        assertThat("Commands should not mutate", cmd, is(baseCmd));
    }


}
