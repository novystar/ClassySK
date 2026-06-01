package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.skript.util.ClassInfoReference;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.*;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodSignature;
import com.novystxr.classysk.api.classes.AbstractSkriptClass;
import com.novystxr.classysk.api.event.MethodRegistrationEvent;
import com.novystxr.classysk.api.event.MethodRunEvent;
import com.novystxr.classysk.api.methods.ArgumentParser;
import com.novystxr.classysk.api.util.ClassyStringUtils;
import com.novystxr.classysk.main.elements.classes.StructClass;
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

    private AccessModifiable.AccessType accessType;
    private boolean isStatic;
    private boolean returnPlural = false;

    private String methodName;
    private SequencedMap<String, MethodArgument> arguments = null;

    private Expression<ClassInfo<?>> classInfoExpr;
    private SectionNode sectionNode;
    private SkriptMethod skriptMethod;

    private AbstractSkriptClass skriptClass;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        if (!(getParser().getCurrentStructure() instanceof StructClass structClass)) {
            Skript.error("Method declaration can only be used within a class structure.");
            return false;
        }
        methodName = ClassyStringUtils.getLowerCase(parseResult.regexes.get(0));

        // validate and validate method arguments
        if (parseResult.hasTag("args")) {
            String argsString = parseResult.regexes.get(1).group();

            if (!argsString.isEmpty()) {
                arguments = ArgumentParser.parseArgs(argsString);

                if (arguments == null) {
                    return false;
                }
            }
        }

        // get return type, check plurality
        this.sectionNode = sectionNode;
        classInfoExpr = (Expression<ClassInfo<?>>) expressions[0];
        if (classInfoExpr != null) {
            Literal<ClassInfoReference> classInfoReference = (Literal<ClassInfoReference>) ClassInfoReference.wrap(classInfoExpr);
            returnPlural = classInfoReference.getSingle().isPlural().isTrue();
        }

        accessType = parseResult.hasTag("private") ? AccessModifiable.AccessType.PRIVATE : AccessModifiable.AccessType.PUBLIC;
        isStatic = parseResult.hasTag("static");

        return true;
    }

    // here we are just doing registration
    @Override
    public @Nullable TriggerItem walk(Event event) {
        if (event instanceof MethodRegistrationEvent regEvent) {

            AbstractSkriptClass skriptClass = regEvent.skriptClass;
            Class<?> returnType = null;
            ClassInfo<?> classInfo = null;
            if (classInfoExpr != null) classInfo = classInfoExpr.getSingle(event);
            if (classInfo != null) returnType = classInfo.getC();

            MethodSignature signature = new MethodSignature(methodName, arguments, accessType, isStatic, returnType, returnPlural, skriptClass);
            skriptMethod = new SkriptMethod(signature);

            skriptClass.putMethod(methodName, skriptMethod);

        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public void evaluateTrigger() {
        Trigger trigger;
        if (classInfoExpr != null) {
            trigger = loadReturnableSectionCode(sectionNode, "method body", new Class[]{MethodRunEvent.class});
        } else {
            trigger = loadCode(sectionNode, "method body", MethodRunEvent.class);
        }

        skriptMethod.setTrigger(trigger);
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
