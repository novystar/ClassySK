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
import com.novystxr.classysk.api.util.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.novystxr.classysk.api.util.StringUtils.splitArgs;

public class MethodParser {

    private static final Pattern FULL_PATTERN =
        Pattern.compile("^\\s*(?<name>[^:(){}\",]+?)\\s*:\\s*(?<type>[a-zA-Z ]+?)\\s*(?:\\s*=\\s*(?<def>.+))?\\s*$");
    private static final Pattern ARGUMENT_PATTERN = Pattern.compile("(?:\\s*(?<name>[_a-zA-Z0-9]+):)?(?<value>.+)");

    public static final String METHOD_PATTERN = "%classinstance%\\:\\:<"+ Classysk.NAME_PATTERN +">\\([args:<.+>]\\)";
    public static final String STATIC_METHOD_PATTERN = "<"+ Classysk.CLASSNAME_PATTERN +">\\:\\:<"+ Classysk.NAME_PATTERN +">\\([args:<.+>]\\)";

    public record ReferenceArgument(
        @Nullable String name,
        Expression<?> expr
    ) {}

    public record MethodReference(
        String name,
        List<ReferenceArgument> args
    ) {}

    public static @Nullable MethodReference parseReference(String name, @Nullable String args) {
        List<ReferenceArgument> referenceArguments = new ArrayList<>();

        if (args == null) {
            return new MethodReference(name, new ArrayList<>(){});
        }

        List<String> rawArgs = splitArgs(args);
        if (rawArgs == null) {
            Skript.error("Could not separate arguments; Invalid parenthesis");
            return null;
        }

        for (String arg : rawArgs) {
            Matcher matcher = ARGUMENT_PATTERN.matcher(arg);
            if (!matcher.matches()) {
                Skript.error("Invalid argument pattern: "+ arg);
                return null;
            }

            String unparsedExpr = matcher.group("value");
            String argName = matcher.group("name");

            SkriptParser parser = new SkriptParser(unparsedExpr, SkriptParser.ALL_FLAGS, ParseContext.DEFAULT);
            Expression<?> expr = LiteralUtils.defendExpression(parser.parseExpression(Object.class));

            if (!LiteralUtils.canInitSafely(expr)) {
                Skript.error("Can't understand this expression: "+unparsedExpr);
                return null;
            }
            referenceArguments.add(new ReferenceArgument(argName, expr));
        }

        return new MethodReference(name, referenceArguments);
    }

    public static @Nullable SequencedMap<String, MethodArgument> parseArguments(String argsString) {
        SequencedMap<String, MethodArgument> arguments = new LinkedHashMap<>();
        List<String> args = StringUtils.splitArgs(argsString);
        if (args == null) {
            Skript.error("Invalid text/variables/parentheses in the arguments of this method");
            return null;
        }
        for (String arg : args) {
            Matcher matcher = FULL_PATTERN.matcher(arg);
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
                if (defaultValue == null || !LiteralUtils.canInitSafely(defaultValue)) {
                    Skript.error("Can't understand this expression: " + unparsedDefault);
                    return null;
                }
                if (!defaultValue.isSingle() && !isPlural) {
                    Skript.error("Cannot pass plural default value into single method argument");
                    return null;
                }
                if (!Converters.converterExists(type, defaultValue.getReturnType())) {
                    Skript.error("Default value does not match the specified classInfo");
                    return null;
                }
            }
            MethodArgument argument = new MethodArgument(type, defaultValue, isPlural);
            arguments.put(name, argument);
        }
        return arguments;
    }
}
