package com.novystxr.classysk.api;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import com.novystxr.classysk.api.SkriptMethod.MethodArgument;
import com.novystxr.classysk.api.SkriptMethod.MethodSignature;
import com.novystxr.classysk.api.util.ConverterUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodParser {

    /**
     * @see org.skriptlang.skript.common.function.FunctionParser
     * @see org.skriptlang.skript.common.function.FunctionReferenceParser
     **/
    private static final Pattern argsPattern =
            Pattern.compile("^\\s*(?<name>[^:(){}\",]+?)\\s*:\\s*(?<type>[a-zA-Z ]+?)\\s*(?:\\s*=\\s*(?<def>.+))?\\s*$");

    private static final Pattern namedArgPattern =
            Pattern.compile("^[^:(){}\\s\",]+:\\s?.+$");

    private static @Nullable List<String> splitArgs(String args) {
        List<String> result = new ArrayList<>();

        int j = 0;
        for (int i = 0; i <= args.length(); i = SkriptParser.next(args, i, ParseContext.DEFAULT)) {
            if (i == -1) return null;
            if (i != args.length() && args.charAt(i) != ',') continue;

            String arg = args.substring(j, i);
            result.add(arg);

            j = i + 1;
            if (i == args.length()) break;

        }

        return result;
    }

    /**
     * @see org.skriptlang.skript.common.function.FunctionArgumentParser
     */

    public static @Nullable SequencedMap<String, Expression<?>> parseReferenceArgs(MethodSignature signature, String argsString) {

        SequencedMap<String, Expression<?>> result = new LinkedHashMap<>();
        List<String> args = splitArgs(argsString);

        if (signature.arguments() == null) {
            Skript.error("This method does not have any arguments");
            return null;
        }

        if (args == null) {
            Skript.error("Invalid text/variables/parentheses in method call");
            return null;
        }

        if (signature.arguments().size() != args.size()) {
            Skript.error("Expected "+signature.arguments().size()+" arguments but got "+args.size());
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

            // is a named argument
            if (namedArgPattern.matcher(arg).matches()) {
                String[] argSplit = arg.split(":\\s?");

                assert argSplit.length == 2;

                argName = argSplit[0];
                argUnparsedExpr = argSplit[1];

                if (!signature.arguments().containsKey(argName)) {
                    Skript.error("Unknown method argument '%s'", argName);
                    return null;
                }

                hasNamedArgs = true;
            } else {
                argName = argNames.get(i);
                argUnparsedExpr = arg;

                hasUnnamedArgs = true;
            }

            Class<?> type = signature.arguments().get(argName).type().getC();
            Expression<?> argExpr = new SkriptParser(argUnparsedExpr, SkriptParser.ALL_FLAGS, ParseContext.DEFAULT).parseExpression(type);

            if (argExpr == null) return null;
            result.put(argName, argExpr);

        }

        List<String> referenceArgNames = new ArrayList<>(result.keySet());

        // check arguments order if mixed arguments
        if (hasNamedArgs && hasUnnamedArgs) {
            i = -1;
            for (String name : argNames) {
                i++;

                if (!referenceArgNames.get(i).equals(name)) {
                    Skript.error("Mixed method arguments must be in order");
                    return null;
                }
            }
        }

        return result;

    }

    public static @Nullable SequencedMap<String, MethodArgument> getParsedArgs(String argsString) {

        SequencedMap<String, MethodArgument> arguments = new LinkedHashMap<>();

        List<String> args = splitArgs(argsString);
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

            ClassInfo<?> type = Classes.getClassInfoFromUserInput(unparsedType);
            boolean isPlural = Utils.isPlural(unparsedType).plural();

            if (type == null) {
                Skript.error("Unable to resolve type '%s'", unparsedType);
                return null;
            }

            Expression<?> defaultValue = null;

            if (unparsedDefault != null) {
                Class<?> target = Utils.getComponentType(type.getC());
                defaultValue = new SkriptParser(unparsedDefault, SkriptParser.ALL_FLAGS, ParseContext.DEFAULT).parseExpression(target);

                String variableName = unparsedDefault.endsWith("*") ? unparsedDefault.substring(0, unparsedDefault.length() - 3) + (!isPlural ? "::1" : "") : unparsedDefault;
                if (!Variable.isValidVariableName(variableName, true, false)) {
                    Skript.error("Invalid argument name: %s", variableName);
                    return null;
                };

                if (defaultValue == null || LiteralUtils.hasUnparsedLiteral(defaultValue)) {
                    Skript.error("Can't understand this expression: " + unparsedDefault);
                    return null;
                }

                if (!defaultValue.isSingle() && !isPlural) {
                    Skript.error("Cannot pass plural default value into single method argument");
                    return null;
                }

                if (!ConverterUtils.canConvert(target, defaultValue.getReturnType())) {
                    Skript.error("Default value does not match the specified type");
                    return null;
                }
            }

            MethodArgument argument = new MethodArgument(name, type, defaultValue, isPlural);
            arguments.put(name, argument);
        }

        return arguments;
    }

}