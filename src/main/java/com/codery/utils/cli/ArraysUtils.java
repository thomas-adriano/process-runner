package com.codery.utils.cli;

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
            result[i] = another[i - one.length];
        }

        return result;
    }

    public static String[] slice(String[] arr, int startPos) {
        String[] result = new String[arr.length - (startPos + 1)];
        for (int i = startPos + 1; i < arr.length; i++) {
            int resultIndex = i - (startPos + 1);
            result[resultIndex] = arr[i];
        }
        return result;
    }
}
