package com.novystxr.classysk.api.util;

import java.util.Locale;
import java.util.regex.MatchResult;

public class ClassyUtils {
    public static String formatList(Object... objects) {
        StringBuilder builder = new StringBuilder();

        for (Object object : objects) {
            builder.append(object);
            builder.append(", ");
        }

        return builder.toString();

    }

    public static String getLowerCase(MatchResult matchResult) {
        return matchResult.group(0).trim().toLowerCase(Locale.ENGLISH);
    }
}
