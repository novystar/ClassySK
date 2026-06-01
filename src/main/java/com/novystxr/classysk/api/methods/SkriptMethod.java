package com.novystxr.classysk.api.methods;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.variables.Variables;
import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.classes.AbstractSkriptClass;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.event.MethodRunEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SequencedMap;

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
        AbstractSkriptClass parentClass

    ) implements AccessModifiable {
        @Override
        public boolean checkAccess(@Nullable AbstractSkriptClass contextClass) {
            if (accessType == AccessType.PRIVATE && contextClass != parentClass) return false;

            return true;
        }

        @Override
        public boolean checkContext(boolean isStatic) {
            return isStatic == this.isStatic;
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

    public Object @Nullable [] run(Event event, SkriptClass skriptClass, @Nullable Map<String, Expression<?>> argExprs) {

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

        MethodRunEvent runEvent = new MethodRunEvent(skriptClass, args);
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

    public static String getEffectiveName(@Nullable SkriptClass parentClass, @Nullable String methodName, @Nullable String args) {
        if (args == null) args = "";
        if (methodName == null) methodName = "";

        String className;
        if (parentClass != null) className = parentClass.getEffectiveName();
        else className = "unknown";

        return className+"::"+methodName+"("+args+")";
    }

}
