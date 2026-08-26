package com.novystxr.classysk.api.methods;

import com.novystxr.classysk.api.Modifier;

import java.util.SequencedMap;

public class AnonymousMethod extends SkriptMethod {
    public AnonymousMethod(String name, SequencedMap<String, MethodArgument> arguments, Modifier[] modifiers, Class<?> type, boolean isPlural, String origin) {
        super(name, arguments, modifiers, type, isPlural, origin);
    }

    public AnonymousMethod(SkriptMethod method) {
        super(method.name, method.arguments, method.modifiers, method.type, method.isPlural, method.origin);
    }
}
