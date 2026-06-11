package com.novystxr.classysk.api.methods;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.variables.Variables;
import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.event.MethodRunEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;

public class SkriptMethod {

    public record MethodArgument(
            String name,
            Class<?> type,

            @Nullable Expression<?> defaultValue,
            boolean isPlural

    ) {}

    public record MethodSignature(
        String name,

        @Nullable SequencedMap<String, MethodArgument> arguments,
        AccessType accessType,
        boolean isStatic,

        @Nullable Class<?> returnType,
        boolean returnPlural,
        SkriptClass parentClass

    ) implements AccessModifiable {
        @Override
        public boolean checkAccess(@Nullable SkriptClass contextClass) {
            if (accessType == AccessType.PRIVATE && contextClass != parentClass) return false;

            return true;
        }

        @Override
        public boolean checkContext(boolean isStatic) {
            return isStatic == this.isStatic;
        }

        public boolean hasRequiredArgs() {
            if (arguments == null) return false;

            for (MethodArgument arg : arguments.sequencedValues()) {
                if (arg.defaultValue == null) {
                    return true;
                }
            }
            return false;
        }
    }

    public SkriptMethod(MethodSignature signature) {
        this.signature = signature;
    }

    private Trigger trigger;
    public final MethodSignature signature;

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public Object @Nullable [] run(Event event, ClassInstance instance, @Nullable Map<String, Expression<?>> argExprs) {

        if (trigger == null) return null;

        Map<String, Object[]> args;

        if (argExprs != null) {
            args = new HashMap<>();
            for (Map.Entry<String, Expression<?>> entry : argExprs.entrySet()) {
                args.put(entry.getKey(), entry.getValue().getArray(event));

            }
        } else {
            args = null;
        }

        MethodRunEvent runEvent = new MethodRunEvent(instance, args);
        if (args != null && signature.arguments != null) {
            for (Entry<String, Object[]> arg : args.entrySet()) {

                Object[] values = arg.getValue();
                String key = arg.getKey();

                if (!signature.arguments.get(key).isPlural) {
                    Variables.setVariable(key, values[0], runEvent, true);
                } else {
                    int i = 0;
                    for (Object value : values) {
                        i++;
                        Variables.setVariable(key+"::"+i, value, runEvent, true);
                    }
                }
            }
        }

        if (trigger.execute(runEvent)) {
            return runEvent.returnObject;
        }

        return null;
    }

    public static String getEffectiveName(@Nullable ClassInstance parentClass, @Nullable String methodName, @Nullable String args) {
        if (args == null) args = "";
        if (methodName == null) methodName = "";

        String className;
        if (parentClass != null) className = parentClass.getEffectiveName();
        else className = "unknown";

        return className+"::"+methodName+"("+args+")";
    }

}
