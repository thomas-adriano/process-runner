package com.codery.utils.cli;

import java.io.IOException;
import java.util.ArrayList;
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

    public int execute(ProcessBuilder pb) {
        pb.command(params);
        Process p = null;
        int ret = -1;
        try {
            p = pb.start();
            ret = p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
    }


    @Override
    public ShellCommand param(String param) {
        return new WindowsCommand(ArraysUtils.concat(params, new String[]{param}));
    }

    @Override
    public ShellCommand param(String paramName, String paramValue) {
        return new WindowsCommand(ArraysUtils.concat(params, new String[]{paramName, paramValue}));
    }
}
