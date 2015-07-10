package com.codery.utils.cli;

import org.junit.Test;

import java.io.File;

import static org.hamcrest.core.Is.is;
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


}
