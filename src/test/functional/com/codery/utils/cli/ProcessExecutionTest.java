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
    public void shouldExecuteProcesses() {
        ShellCli cli = new WindowsCli(new File("src/test/resources/ProcessExecutionTest"));
        int ret = cli.execute(new WindowsCommand("tasklist").param("/?"));
        assertThat(ret, is(0));
    }


}
