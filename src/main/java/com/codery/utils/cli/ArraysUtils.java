package com.codery.utils.cli;

import java.lang.reflect.Array;

/**
 * Created by thomasadriano on 09/07/15.
 */
public class ArraysUtils {

    public static String[] concat(String[] one, String[] another) {
        String[] result = new String[one.length + another.length];

        for (int i = 0; i < one.length; i++) {
            result[i] = one[i];
        }

        for (int i = one.length; i < one.length + another.length; i++) {
            result[i] = another[i-one.length];
        }

        return result;
    }
}
