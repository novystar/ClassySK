package com.novystxr.classysk.api.assignability;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.util.ReflectUtils;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.main.elements.Types;
import net.bytebuddy.asm.Advice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassInfoAdvice {

    public static final Pattern pattern = Pattern.compile("("+ Classysk.CLASSNAME_PATTERN +") (?i)instances?");

    @Advice.OnMethodExit
    static void onExit(@Advice.Argument(0) String input, @Advice.Return(readOnly = false) ClassInfo<?> result) {
        if (result != null) return;

        Matcher matcher = pattern.matcher(input);
        if (matcher.matches()) {
            String name = StringUtils.getLowerCase(matcher.group(1));
            result = getClassInfo(name, AssignabilityBridge.getSubInterface(name));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends AssignabilityBridge> ClassInfo<T> getClassInfo(String name, Class<T> subclass) {
        ReflectUtils.createLanguageNode(name);
        return new ClassInfo<>(subclass, name+"classinstance")
            .serializeAs(ClassInstance.class)
            .parser((Parser<T>) Types.classParser);
    }
}
