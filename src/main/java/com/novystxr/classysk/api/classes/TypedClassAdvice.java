package com.novystxr.classysk.api.classes;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.main.elements.Types;
import net.bytebuddy.asm.Advice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TypedClassAdvice {

    public static final Pattern pattern = Pattern.compile("("+ Classysk.CLASSNAME_PATTERN + ") instances?");

    @Advice.OnMethodExit
    static void onExit(@Advice.Argument(0) String input, @Advice.Return(readOnly = false) ClassInfo<?> result) {
        if (result != null) return;

        Matcher matcher = pattern.matcher(input);
        if (matcher.matches()) {
            String name = StringUtils.getLowerCase(matcher.group(1));
            result = getClassInfo(ClassManager.getSubclass(name));

        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends ClassInstance> ClassInfo<T> getClassInfo(Class<T> subclass) {
        return new ClassInfo<>(subclass, "classinstance")
            .name("Class Instance")
            .serializeAs(ClassInstance.class)
            .parser((Parser<T>) Types.classParser);
    }
}
