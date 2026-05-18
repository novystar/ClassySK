package com.novystxr.classysk.api;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.variables.Variables;
import com.novystxr.classysk.api.event.MethodRunEvent;
import com.novystxr.classysk.main.elements.classes.StructClass;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SequencedMap;

public class SkriptMethod {

    public record MethodArgument(
            String name,
            ClassInfo<?> type,

            @Nullable Expression<?> defaultValue,
            boolean isPlural

    ) {}

    public record MethodSignature(
        String name,
        Trigger trigger,

        @Nullable SequencedMap<String, MethodArgument> arguments,
        AccessType accessType,
        boolean isStatic,

        @Nullable ClassInfo<?> returnType,
        boolean returnPlural
    ) {
        public boolean checkAccess(SkriptClass skriptClass) {
            ParserInstance parser = ParserInstance.get();

            if (accessType == AccessType.PRIVATE) {
                if (parser.getCurrentStructure() instanceof StructClass structClass) {
                    return structClass.getName().equals(skriptClass.name);
                }
            }

            return true;
        }

        public @Nullable Object[] run(Event event, SkriptClass skriptClass, @Nullable Map<String, Expression<?>> argExprs) {

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
            if (args != null && arguments != null) {
                for (Entry<String, Object[]> arg : args.entrySet()) {

                    Object[] values = arg.getValue();
                    String key = arg.getKey();

                    if (!arguments.get(key).isPlural) {
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
    }
}
