package com.codery.utils.cli;

import java.util.Arrays;

/**
 * Created by thomasadriano on 09/07/15.
 */
public class CliCommand {

    private final String[] params;

    public CliCommand(String process) {
        params = new String[]{process};
    }

    public CliCommand(String[] cmdLine) {
        this.params = cmdLine;
    }

    public CliCommand(CliCommand cmd) {
        this.params = cmd.params;}

    public String[] getCmdLine() {
        return params;
    }


    public CliCommand param(String param) {
        return new CliCommand(ArraysUtils.concat(params, new String[]{param}));
    }

    public CliCommand param(String paramName, String paramValue) {
        return new CliCommand(ArraysUtils.concat(params, new String[]{paramName, paramValue}));
    }

    public String toString() {
        return Arrays.toString(params).replaceAll("\\]|\\]|,", "");
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CliCommand that = (CliCommand) o;

        return Arrays.equals(params, that.params);

    }

    public int hashCode() {
        return params != null ? Arrays.hashCode(params) : 0;
    }
}
