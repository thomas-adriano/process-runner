package com.codery.utils.cli;

import java.io.File;

/**
 * Created by thomasadriano on 12/07/15.
 */
public interface FutureExecution {

    int execute();

    FutureExecution pipe(CliCommand cmd);

    FutureExecution background();

    FutureExecution and(CliCommand cmd);

    FutureExecution redirectOutput(File out);

    FutureExecution redirectOutputAppending(File out);
    
    FutureExecution supressOutput();

    CliCommand getCommand();

}
