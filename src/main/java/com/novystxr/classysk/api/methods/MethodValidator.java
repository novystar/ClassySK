package com.novystxr.classysk.api.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodSignature;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.util.ClassyStringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.SequencedMap;

public class MethodValidator {
    public MethodValidator(String methodName, @Nullable String args, boolean expectsReturn, boolean isStatic) {
        this.methodName = methodName;
        this.expectsReturn = expectsReturn;
        this.isStatic = isStatic;

        if (args == null || args.isEmpty()) {
            noReferencedArgs = true; return;
        }
        argsString = args;
        argStrings = ClassyStringUtils.splitArgs(args);
        if (argStrings == null) {
            failedParse();
        }
    }
    public ClassInstance instance = null;
    private boolean noReferencedArgs = false;

    public SequencedMap<String, Expression<?>> parsedArgs;
    public SkriptMethod parsedMethod;

    private final String methodName;
    private String argsString;
    private final boolean isStatic;

    private List<String> argStrings;
    private final boolean expectsReturn;
    private Kleenean isValid = Kleenean.UNKNOWN;

    public void validate(String className) {
        if (!ClassManager.classExists(className)) {
            Skript.error("Cannot resolve class '%s'", className);
            isValid = Kleenean.FALSE;
            return;
        }
        validate(ClassManager.getClass(className));
    }
    public void validate(ClassInstance instance) {
        this.instance = instance;
        isValid = Kleenean.UNKNOWN;
        if (instance == null) {
            illegalAccess(); return;
        }
        SkriptMethod method = instance.getAccessibleMethod(methodName);
        if (method == null) {
            illegalAccess(); return;
        }
        parsedMethod = method;
        MethodSignature signature = method.signature;
        if (expectsReturn && signature.returnType() == null) {
            Skript.error("This method can't return anything");
            isValid = Kleenean.FALSE; return;
        }
        if (noReferencedArgs) {
            if (signature.hasRequiredArgs()) {
                Skript.error("Method has arguments but none provided");
                isValid = Kleenean.FALSE; return;
            }
            if (signature.arguments() == null) return;
        }
        if (isValid.isUnknown()) {
            parsedArgs = ArgumentParser.parseReferenceArgs(signature, argStrings);
            if (parsedArgs == null) isValid = Kleenean.FALSE;
        }
    }

    public void checkAccess(Event event) {
        if (isValid.isFalse()) return;
        if (!parsedMethod.signature.isAccessible(event, isStatic)) {
            isValid = Kleenean.FALSE; return;
        }
        isValid = Kleenean.TRUE;
    }
    public void checkAccess(ParserInstance parser) {
        if (isValid.isFalse()) return;
        if (!parsedMethod.signature.isAccessible(parser, isStatic)) {
            isValid = Kleenean.FALSE; return;
        }
        isValid = Kleenean.TRUE;
    }

    public void updateInstance(Expression<ClassInstance> skriptClassExpr, Event event) {
        // static access
        if (skriptClassExpr == null) {
            return;
        }
        ClassInstance newClass = skriptClassExpr.getSingle(event);
        if (newClass == null) {
            isValid = Kleenean.FALSE;
            return;
        }
        if (this.instance == null || newClass.getParent() != this.instance.getParent()) {
            validate(newClass);
            checkAccess(event);
        }
    }

    private void illegalAccess() {
        isValid = Kleenean.FALSE;
        Skript.error("Illegal Access! Tried to access non-existent method '%s'", SkriptMethod.getEffectiveName(instance, methodName, argsString)+" or tried to access it from improper context");
    }
    private void failedParse() {
        isValid = Kleenean.FALSE;
        Skript.error("Method call failed to validate! '%s'", SkriptMethod.getEffectiveName(instance, methodName, argsString));
    }

    public Kleenean isValid() {
        return isValid;
    }
}