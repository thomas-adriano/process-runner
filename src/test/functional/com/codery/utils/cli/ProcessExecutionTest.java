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
        int ret = cli.execute(cmd);
        assertThat("The command "+cmd+" should've return 0", ret, is(0));
    }

    @Test
    public void shouldNotReturnZero_WhenProcessIsIncorrect() {
        ShellCli cli = new WindowsCli(new File("src/test/resources/ProcessExecutionTest"));
        WindowsCommand cmd = new WindowsCommand("tasklistr").param("/?");
        int ret = cli.execute(cmd);
        assertThat("The command "+cmd+"should've return 1", ret, is(1));
    }


    @Test
    public void WindowsCliShouldBeImmutable() {
        ShellCli baseCli = new WindowsCli();
        ShellCli cli = new WindowsCli();
        ShellCli cli2 = cli.dir(new File("src/test/resources"));
        assertThat(cli, equalTo((baseCli)));
    }

    @Test
    public void WindowsCommandShouldBeImmutable() {
        WindowsCommand baseCmd = new WindowsCommand("test");
        WindowsCommand cmd = new WindowsCommand("test");
        WindowsCommand cmd2 = cmd.param("bla").param("p","a");

        assertThat("Commands should not mutate", cmd, is(baseCmd));
    }

}
