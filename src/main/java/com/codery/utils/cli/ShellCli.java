package com.codery.utils.cli;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created by thomasadriano on 09/07/15.
 */
public interface ShellCli extends AutoCloseable {

    ShellCli setEnvironmentVariable(String key, String value);

    ShellCli setEnvironmentVariables(Map<String, String> vars);

    Map<String,String> getEnvironentVariables();

    FutureExecution command(CliCommand cmd);

    ShellCli dir(File dir);

    ShellCli addStandardOutput(OutputStream dest);

    ShellCli addErrorOutput(OutputStream dest);
    
    ShellCli clearStandardOutputTargets();
    
    ShellCli clearErrorOutputTargets();
}

