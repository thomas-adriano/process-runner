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

    private WindowsCommand(String[] params) {
        this.params = params;
    }

    @Override
    public String[] getParams() {
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
}
