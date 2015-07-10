package com.codery.utils.cli;

/**
 * Created by thomasadriano on 09/07/15.
 */
public interface ShellCommand {

    ShellCommand param(String param);

    ShellCommand param(String paramName, String paramValue);

    String[] getParams();
}
