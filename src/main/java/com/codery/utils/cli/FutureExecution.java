package com.codery.utils.cli;

/**
 * Created by thomasadriano on 12/07/15.
 */
public interface FutureExecution {

    int execute();

    FutureExecution pipe(CliCommand cmd);

    FutureExecution background();

    FutureExecution and(CliCommand cmd);

    CliCommand getCommand();

}
