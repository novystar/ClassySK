package com.novystxr.classysk.api.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodSignature;
import com.novystxr.classysk.api.util.ConverterUtils;
import com.novystxr.classysk.api.util.ClassyStringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see org.skriptlang.skript.common.function
 **/
public class ArgumentParser {
    private static final Pattern argsPattern =
            Pattern.compile("^\\s*(?<name>[^:(){}\",]+?)\\s*:\\s*(?<type>[a-zA-Z ]+?)\\s*(?:\\s*=\\s*(?<def>.+))?\\s*$");

    private static final Pattern namedArgPattern =
            Pattern.compile("^[^:(){}\\s\",]+:\\s?.+$");

    public static final String methodPattern = "%classs%\\:\\:<"+ Classysk.namePattern +">\\([args:<.+>]\\)";
    public static final String staticMethodPattern = "<"+ Classysk.classNamePattern +">\\:\\:<"+ Classysk.namePattern +">\\([args:<.+>]\\)";

    public static @Nullable SequencedMap<String, Expression<?>> parseReferenceArgs(MethodSignature signature, List<String> args) {
        if (args == null) args = new ArrayList<>();
        SequencedMap<String, Expression<?>> result = new LinkedHashMap<>();
        if (signature.arguments() == null) {
            Skript.error("This method does not have any arguments");
            return null;
        }
        boolean hasNamedArgs = false;
        boolean hasUnnamedArgs = false;

        List<String> argNames = new ArrayList<>(signature.arguments().keySet());
        int i = -1;
        for (String arg : args) {
            i++;
            String argName;
            String argUnparsedExpr;

            if (namedArgPattern.matcher(arg).matches()) {
                String[] argSplit = arg.split(":\\s?");
                argName = argSplit[0];
                argUnparsedExpr = argSplit[1];

                if (!signature.arguments().containsKey(argName)) {
                    Skript.error("Unknown method argument '%s'", argName);
                    return null;
                }
                hasNamedArgs = true;
            } else {
                if (i == argNames.size()) {
                    Skript.error("Argument %s out of bounds for this method signature", i + 1);
                    return null;
                }
                argName = argNames.get(i);
                argUnparsedExpr = arg;
                hasUnnamedArgs = true;
            }
            Class<?> type = signature.arguments().get(argName).type();
            SkriptParser parser = new SkriptParser(argUnparsedExpr, SkriptParser.ALL_FLAGS, ParseContext.DEFAULT);

            Expression<?> argExpr = LiteralUtils.defendExpression(parser.parseExpression(type));
            if (argExpr == null) return null;

            result.put(argName, argExpr);
        }
        List<String> referenceArgNames = new ArrayList<>(result.keySet());
        if (hasNamedArgs && hasUnnamedArgs) {
            i = -1; // check arguments order
            for (String name : argNames) {
                i++;
                if (!referenceArgNames.get(i).equals(name)) {
                    Skript.error("Mixed method arguments must be in order");
                    return null;
                }
            }
        }
        // resolve defaults, if one cannot resolve then fail parse
        for (Entry<String, MethodArgument> entry : signature.arguments().entrySet()) {
            if (referenceArgNames.contains(entry.getKey())) continue;

            Expression<?> defaultValue = entry.getValue().defaultValue();
            if (defaultValue == null) {
                Skript.error("Could not resolve some argument(s) for this method call");
                return null;
            }
            result.put(entry.getKey(), defaultValue);
        }
        return result;
    }

    public static @Nullable SequencedMap<String, MethodArgument> parseArgs(String argsString) {
        SequencedMap<String, MethodArgument> arguments = new LinkedHashMap<>();
        List<String> args = ClassyStringUtils.splitArgs(argsString);
        if (args == null) {
            Skript.error("Invalid text/variables/parentheses in the arguments of this method");
            return null;
        }
        for (String arg : args) {
            Matcher matcher = argsPattern.matcher(arg);
            if (!matcher.matches()) {
                Skript.error("Invalid method argument(s)");
                return null;
            }
            String name = matcher.group("name");
            String unparsedType = matcher.group("type");
            String unparsedDefault = matcher.group("def");

            if (arguments.containsKey(name)) {
                Skript.error("Duplicate method arguments");
                return null;
            }
            ClassInfo<?> classInfo = Classes.getClassInfoFromUserInput(unparsedType);
            boolean isPlural = Utils.isPlural(unparsedType).plural();
            if (classInfo == null) {
                Skript.error("Unable to resolve classInfo '%s'", unparsedType);
                return null;
            }
            Class<?> type = Utils.getComponentType(classInfo.getC());
            Expression<?> defaultValue = null;

            if (unparsedDefault != null) {

                SkriptParser parser = new SkriptParser(unparsedDefault, SkriptParser.ALL_FLAGS, ParseContext.DEFAULT);

                defaultValue = LiteralUtils.defendExpression(parser.parseExpression(type));
                String variableName = unparsedDefault.endsWith("*") ? unparsedDefault.substring(0, unparsedDefault.length() - 3) + (!isPlural ? "::1" : "") : unparsedDefault;

                if (!Variable.isValidVariableName(variableName, true, false)) {
                    Skript.error("Invalid argument name: %s", variableName);
                    return null;
                }
                if (defaultValue == null || LiteralUtils.hasUnparsedLiteral(defaultValue)) {
                    Skript.error("Can't understand this expression: " + unparsedDefault);
                    return null;
                }
                if (!defaultValue.isSingle() && !isPlural) {
                    Skript.error("Cannot pass plural default value into single method argument");
                    return null;
                }
                if (!ConverterUtils.canConvert(type, defaultValue.getReturnType())) {
                    Skript.error("Default value does not match the specified classInfo");
                    return null;
                }
            }
            MethodArgument argument = new MethodArgument(name, type, defaultValue, isPlural);
            arguments.put(name, argument);
        }
        return arguments;
    }
}