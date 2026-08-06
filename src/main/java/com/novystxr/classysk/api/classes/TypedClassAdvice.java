package com.novystxr.classysk.api.classes;

import ch.njol.skript.classes.ClassInfo;
import com.novystxr.classysk.api.util.StringUtils;
import net.bytebuddy.asm.Advice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TypedClassAdvice {

    @Advice.OnMethodExit
    static void onExit(@Advice.Argument(0) String input, @Advice.Return(readOnly = false) ClassInfo<?> result) {
        if (result == null) return;
        if (result instanceof TypedClassInfo<?> typedInfo) {

            Pattern[] patterns = typedInfo.getUserInputPatterns();
            if (patterns == null) return;

            for (final Pattern pattern : typedInfo.getUserInputPatterns()) {
                Matcher matcher = pattern.matcher(input);
                if (matcher.matches()) {
                    String name = StringUtils.getLowerCase(matcher.group(1));
                    result = new TypedClassInfo<>(typedInfo.getC(), typedInfo.getCodeName(), name);
                }
            }
        }
    }
}
