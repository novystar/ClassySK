package com.novystxr.classysk.main.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.skript.util.ClassInfoReference;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.AbstractSkriptClass;
import com.novystxr.classysk.api.AccessType;
import com.novystxr.classysk.api.MethodParser;
import com.novystxr.classysk.api.SkriptMethod.MethodArgument;
import com.novystxr.classysk.api.SkriptMethod.MethodSignature;
import com.novystxr.classysk.api.event.MethodRegistrationEvent;
import com.novystxr.classysk.api.event.MethodRunEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.*;

public class SecMethod extends Section implements ReturnHandler<Object> {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.SECTION,
                SyntaxInfo.builder(SecMethod.class)
                        .addPattern("[public|:private] [:static] method <(\\w+)>\\([args:<.+>]\\) [(\\:\\:|returns) %-classinfo%]")
                        .supplier(SecMethod::new)
                        .build()

        );
    }

    // TODO: functionNamePattern to allow snake case

    private Trigger trigger;

    private AccessType accessType;
    private boolean isStatic;
    private boolean returnPlural = false;

    private String methodName;
    private SequencedMap<String, MethodArgument> arguments = null;

    private Object[] returnObject;

    private Expression<ClassInfo<?>> classInfoExpr;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        if (!(getParser().getCurrentStructure() instanceof StructClass)) {
            Skript.error("Method declaration can only be used within a class structure.");
            return false;
        }

        methodName = parseResult.regexes.get(0).group(0);

        if (methodName == null) {
            Skript.error("Method name cannot be empty");
            return false;
        }

        // validate and parse method arguments
        if (parseResult.hasTag("args")) {
            String argsString = parseResult.regexes.get(1).group();

            if (!argsString.isEmpty()) {
                arguments = MethodParser.getParsedArgs(argsString);

                if (arguments == null) {
                    return false;
                }
            }
        }

        // get return type, check plurality
        classInfoExpr = (Expression<ClassInfo<?>>) expressions[0];
        if (classInfoExpr != null) {
            Literal<ClassInfoReference> classInfoReference = (Literal<ClassInfoReference>) ClassInfoReference.wrap(classInfoExpr);
            returnPlural = classInfoReference.getSingle().isPlural().isTrue();

            trigger = loadReturnableSectionCode(sectionNode, "method body", new Class[]{MethodRunEvent.class});
        } else {
            trigger = loadCode(sectionNode, "method body", MethodRunEvent.class);
        }

        methodName = methodName.trim().toLowerCase(Locale.ENGLISH);
        accessType = parseResult.hasTag("private") ? AccessType.PRIVATE : AccessType.PUBLIC;
        isStatic = parseResult.hasTag("static");

        return true;
    }

    // here we are just doing registration
    @Override
    protected @Nullable TriggerItem walk(Event event) {
        if (event instanceof MethodRegistrationEvent regEvent) {

            AbstractSkriptClass skriptClass = regEvent.skriptClass;
            ClassInfo<?> returnType = null;
            if (classInfoExpr != null) returnType = classInfoExpr.getSingle(event);

            MethodSignature signature = new MethodSignature(methodName, trigger, arguments, accessType, isStatic, returnType, returnPlural);
            skriptClass.putMethodSignature(methodName, signature);

        }

        return null;
    }

    @Override
    public void returnValues(Event event, Expression<?> value) {
        if (event instanceof MethodRunEvent runEvent) {
            runEvent.returnObject = value.getArray(event);
        }
    }

    @Override
    public boolean isSingleReturnValue() {
        return !returnPlural;
    }

    @Override
    public @Nullable Class<?> returnValueType() {
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "method declaration";
    }
}
