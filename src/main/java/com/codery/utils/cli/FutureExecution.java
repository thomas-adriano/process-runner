package com.codery.utils.cli;

import com.sun.corba.se.impl.orbutil.closure.Future;

/**
 * Created by thomasadriano on 12/07/15.
 */
public interface FutureExecution {

    int execute();

    FutureExecution pipe(ShellCommand cmd);

    FutureExecution background(ShellCommand cmd);

}
