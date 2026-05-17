package com.novystxr.classysk.api.util;

public class ClassyUtils {
    public static String formatList(Object... objects) {
        StringBuilder builder = new StringBuilder();

        for (Object object : objects) {
            builder.append(object);
            builder.append(", ");
        }

        return builder.toString();

    }
}
