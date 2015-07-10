package com.codery.utils.cli;

import java.io.File;

/**
 * Created by thomasadriano on 09/07/15.
 */
public interface ShellCli {

    ShellCli setEnvorinmentVariable(String key, String value);

    int execute(ShellCommand cmd);

    ShellCli dir(File dir);
}
