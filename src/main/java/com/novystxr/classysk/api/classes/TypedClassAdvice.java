package com.novystxr.classysk.api.classes;

import ch.njol.skript.classes.ClassInfo;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.api.util.TypedInstanceParser;
import net.bytebuddy.asm.Advice;

import java.util.regex.Matcher;

public class TypedClassAdvice {

    @Advice.OnMethodExit
    static void onExit(@Advice.Argument(0) String input, @Advice.Return(readOnly = false) ClassInfo<?> result) {
        if (result != null) return;

        Matcher matcher = TypedInstanceWrapper.pattern.matcher(input);

        if (matcher.matches()) {
            String name = StringUtils.getLowerCase(matcher.group(1));
            Class<? extends TypedInstanceWrapper> subclass = ClassManager.getSubclass(name);

            result = new ClassInfo<>(subclass, "typedinstance")
                .serializeAs(ClassInstance.class)
                .parser(new TypedInstanceParser<>());
        }
    }
}
