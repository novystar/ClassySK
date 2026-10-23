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
    private int next = 0;
    private int start = 0;

    @Override
    public boolean hasNext() {
        return next != -1 && next < args.length();
    }

    @Override
    public String next() {
        while (next != args.length() && args.charAt(next) != ',') {
            next = SkriptParser.next(args, next, ParseContext.DEFAULT);
            if (next == -1) return null; // invalid brackets
        }
        String result = args.substring(start, next);
        start = next + 1;
        return result;

    }

    @Override
    public @NonNull Iterator<String> iterator() {
        return this;
    }
}
