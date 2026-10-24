package com.novystxr.classysk.api.util;

import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;

public class ArgumentIterator implements Iterator<String>, Iterable<String> {

    public ArgumentIterator(String args) {
        this.args = args;
    }

    private final String args;
    private int start = 0;
    private int end = 0;
    private boolean hasNext = true;

    @Override
    public boolean hasNext() {
        return args != null && hasNext;
    }

    @Override
    public String next() {
        while (end != args.length() && args.charAt(end) != ',') {
            end = SkriptParser.next(args, end, ParseContext.DEFAULT);
            if (end == -1) {
                hasNext = false;
                return null;
            }
        }
        String result = args.substring(start, end);
        if (end == args.length()) {
            hasNext = false;
        } else {
            end++;
            start = end;
        }
        return result;

    }

    @Override
    public @NonNull Iterator<String> iterator() {
        return this;
    }
}
