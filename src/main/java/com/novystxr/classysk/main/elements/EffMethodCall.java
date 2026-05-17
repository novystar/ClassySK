package com.novystxr.classysk.main.elements;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.MethodParser;
import com.novystxr.classysk.api.SkriptClass;
import com.novystxr.classysk.api.SkriptMethod;
import com.novystxr.classysk.api.SkriptMethod.MethodSignature;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.FunctionReference.Argument;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Locale;
import java.util.SequencedMap;

public class EffMethodCall extends Effect {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.EFFECT,
                SyntaxInfo.builder(EffMethodCall.class)
                        .addPattern("%classs%\\:\\:<(\\w+)>\\([args:<.+>]\\)")
                        .supplier(EffMethodCall::new)
                        .build()
        );
    }

    private Expression<SkriptClass> classExpr;

    private String methodName;
    private String argsString;

    private SequencedMap<String, Expression<?>> arguments = null;
    private MethodSignature signature;
    private SkriptClass skriptClass;

    private boolean hasArgs;

    private Boolean canParse;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {

        hasArgs = parseResult.hasTag("args");

        methodName = parseResult.regexes.get(0).group(0).trim().toLowerCase(Locale.ENGLISH);
        if (hasArgs) {
            argsString = parseResult.regexes.get(1).group(0);
        }

        classExpr = (Expression<SkriptClass>) expressions[0];

        return true;
    }

    private void illegalAccess() {
        canParse = false;
        warning("Illegal Access Warning! Script '" + getParser().getCurrentScript().name() + ".sk' tried to access non-existent method " + classExpr.toString() + "#" + methodName + "(" + argsString + "), or tried to access it from improper context");
    }

    private void failedParse() {
        canParse = false;
        warning("Method call failed to parse! '"+classExpr.toString() + "#" + methodName + "(" + argsString + ")'");
    }

    // runtime validation and method execution
    // TODO: add init parsing and runtime validation in 2 parts rather than all at runtime
    @Override
    protected void execute(Event event) {

        if (canParse == null) {
            skriptClass = classExpr.getSingle(event);

            if (skriptClass == null) {
                illegalAccess();
                return;
            }

            signature = skriptClass.getAccessibleMethod(methodName);

            if (signature == null || !signature.checkAccess(skriptClass, getParser())) {
                illegalAccess();
                return;
            }
            if (hasArgs == (signature.arguments() == null)) {
                failedParse();
                return;
            }

            arguments = MethodParser.parseReferenceArgs(signature, argsString);
            if (hasArgs && arguments == null) {
                failedParse();
                return;
            }

            canParse = true;
        }
            if (!canParse) return;

            signature.run(event, skriptClass, arguments);

    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "method call";
    }
}
