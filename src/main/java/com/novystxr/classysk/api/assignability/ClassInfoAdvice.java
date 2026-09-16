package com.novystxr.classysk.api.assignability;

import ch.njol.skript.classes.ClassInfo;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.util.StringUtils;
import net.bytebuddy.asm.Advice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassInfoAdvice {

    public static final Pattern pattern = Pattern.compile("(\\w*) instances?");

    @Advice.OnMethodExit
    static void onExit(@Advice.Argument(0) String input, @Advice.Return(readOnly = false) ClassInfo<?> result) {
        if (result != null) return;

        Matcher matcher = pattern.matcher(input);
        if (matcher.matches()) {
            String name = StringUtils.getLowerCase(matcher.group(1));
            result = new ClassInfo<>(AssignabilityBridge.getSubInterface(name), "typedinstance")
                .serializeAs(ClassInstance.class);
        }
    }
}
