package com.codery.utils.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by thomasadriano on 09/07/15.
 */
public class WindowsCommand implements ShellCommand {

    private final String[] params;

    public WindowsCommand(String process) {
        params = new String[]{process};
    }

    public WindowsCommand(String[] cmdLine) {
        this.params = cmdLine;
    }

    @Override
    public String[] getCmdLine() {
        return params;
    }


    @Override
    public WindowsCommand param(String param) {
        return new WindowsCommand(ArraysUtils.concat(params, new String[]{param}));
    }

    @Override
    public WindowsCommand param(String paramName, String paramValue) {
        return new WindowsCommand(ArraysUtils.concat(params, new String[]{paramName, paramValue}));
    }

    @Override
    public String toString() {
        return Arrays.toString(params);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WindowsCommand that = (WindowsCommand) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(params, that.params);

    }

    @Override
    public int hashCode() {
        return params != null ? Arrays.hashCode(params) : 0;
    }
}
