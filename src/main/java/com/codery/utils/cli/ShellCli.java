package com.codery.utils.cli;

import java.io.File;

/**
 * Created by thomasadriano on 09/07/15.
 */
public interface ShellCli {

    ShellCli setEnvironmentVariable(String key, String value);

    FutureExecution command(ShellCommand cmd);

    ShellCli dir(File dir);
}

