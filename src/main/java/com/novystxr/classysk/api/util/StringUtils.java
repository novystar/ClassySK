package com.novystxr.classysk.api.util;

import ch.njol.skript.SkriptConfig;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.MatchResult;

public class StringUtils {
    public static @Nullable List<String> splitArgs(String args) {
        List<String> result = new ArrayList<>();
        int j = 0;
        for (int i = 0; i <= args.length(); i = SkriptParser.next(args, i, ParseContext.DEFAULT)) {
            if (i == -1) return null;
            if (i != args.length() && args.charAt(i) != ',') continue;

            String arg = args.substring(j, i);
            result.add(arg);

            j = i + 1;
            if (i == args.length()) break;
        }
        return result;
    }

    public static String titleCase(String input) {
        return input.substring(0, 1).toUpperCase()+input.substring(1);
    }

    public static String getLowerCase(String value) {
        if (value == null) return null;
        if (SkriptConfig.caseInsensitiveVariables.value()) {
            return value.trim().toLowerCase(Locale.ENGLISH);
        }
        return value.trim();
    }

    public static String getLowerCase(MatchResult matchResult) {
        return getLowerCase(matchResult.group(0));
    }
}
