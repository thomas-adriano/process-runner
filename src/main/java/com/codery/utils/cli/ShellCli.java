package com.codery.utils.cli;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created by thomasadriano on 09/07/15.
 */
public interface ShellCli {

    ShellCli setEnvironmentVariable(String key, String value);

    ShellCli setEnvironmentVariables(Map<String, String> vars);

    Map<String,String> getEnvironentVariables();

    FutureExecution command(ShellCommand cmd);

    ShellCli dir(File dir);

    WindowsCli addStandardOutput(OutputStream dest);

    WindowsCli addErrorOutput(OutputStream dest);
}

