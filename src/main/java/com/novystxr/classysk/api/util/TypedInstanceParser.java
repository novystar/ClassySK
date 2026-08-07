package com.novystxr.classysk.api.util;

import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;
import com.novystxr.classysk.api.classes.ClassInstance.TypedInstanceWrapper;

public class TypedInstanceParser<T extends TypedInstanceWrapper> extends Parser<T> {

    @Override
    public boolean canParse(ParseContext context) {
        return false;
    }

    @Override
    public String toString(T o, int flags) {
        return "Class Instance " + StringUtils.titleCase(o.instance.name);
    }

    @Override
    public String toVariableNameString(T o) {
        return "Class Instance " + o.instance.name + " (" + o.instance.getHashCode() + ")";
    }
}
