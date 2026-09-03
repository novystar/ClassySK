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
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import com.novystxr.classysk.api.util.DefaultValue;
import com.novystxr.classysk.api.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.novystxr.classysk.api.util.StringUtils.splitArgs;
import static com.novystxr.classysk.Classysk.CLASSNAME_PATTERN;
import static com.novystxr.classysk.Classysk.NAME_PATTERN;

public class MethodParser {

    // argument components
    private static final String NAME = "(?<name>[_a-zA-Z0-9]+)";
    private static final String TYPE = "(?<type>[a-zA-Z ]+)";
    private static final String VALUE = "(?<value>.+)";

    // compiled argument patterns
    private static final Pattern DEF_ARG_PATTERN =
        Pattern.compile("^\\s*"+NAME+"\\s*:\\s*"+TYPE+"(?:\\s*=\\s*"+VALUE+"+)?$");

    private static final Pattern REF_ARG_PATTERN =
        Pattern.compile("(?:\\s*"+NAME+":\\s)?"+VALUE);

    // syntax patterns
    public static final String HINT_PATTERN = "(?:<("+CLASSNAME_PATTERN+"|)\\u003E)?";
    public static final String METHOD_PATTERN = "(%-classinstance%|:super)<"+HINT_PATTERN+"::("+NAME_PATTERN+")>\\([<.+>]\\)";
    public static final String STATIC_METHOD_PATTERN = "<("+CLASSNAME_PATTERN+")::("+NAME_PATTERN+")>\\([<.+>]\\)";

    public record ReferenceArgument(
        @Nullable String name,
        Expression<?> expr
    ) {}

    public record MethodReference(
        String name,
        List<ReferenceArgument> args,
        boolean isStatic
    ) {

        @Override
        public @NonNull String toString() {
            StringBuilder builder = new StringBuilder(name+"(");

            int i = 0;
            for (ReferenceArgument arg : args) {
                i++;
                if (arg.name != null) {
                    builder.append(arg.name).append(": ");
                }
                builder.append(Classes.getExactClassInfo(arg.expr.getReturnType()));
                if (i != args.size()) {
                    builder.append(", ");
                }
            }
            return builder.append(")").toString();
        }
    }

    public static @Nullable MethodReference parseReference(String name, @NotNull String args, boolean isStatic) {
        List<ReferenceArgument> referenceArguments = new ArrayList<>();

        if (args.isEmpty()) {
            return new MethodReference(name, new ArrayList<>(), isStatic);
        }

        List<String> rawArgs = splitArgs(args);
        if (rawArgs == null) {
            Skript.error("Could not separate arguments; Invalid parenthesis");
            return null;
        }

        for (String arg : rawArgs) {
            Matcher matcher = REF_ARG_PATTERN.matcher(arg);
            if (!matcher.matches()) {
                Skript.error("Invalid argument pattern: "+ arg);
                return null;
            }

            String unparsedExpr = matcher.group("value").trim();
            String argName = matcher.group("name");

            SkriptParser parser = new SkriptParser(unparsedExpr, SkriptParser.ALL_FLAGS, ParseContext.DEFAULT);
            Expression<?> expr = LiteralUtils.defendExpression(parser.parseExpression(Object.class));

            if (!LiteralUtils.canInitSafely(expr)) {
                Skript.error("Can't understand this expression: "+unparsedExpr);
                return null;
            }
            referenceArguments.add(new ReferenceArgument(argName, expr));
        }

        return new MethodReference(name, referenceArguments, isStatic);
    }

    public static @Nullable SequencedMap<String, MethodArgument> parseArguments(String argsString) {
        SequencedMap<String, MethodArgument> arguments = new LinkedHashMap<>();
        List<String> args = StringUtils.splitArgs(argsString);
        if (args == null) {
            Skript.error("Invalid text/variables/parentheses in the arguments of this method");
            return null;
        }
        for (String arg : args) {
            Matcher matcher = DEF_ARG_PATTERN.matcher(arg);
            if (!matcher.matches()) {
                Skript.error("Invalid method argument(s)");
                return null;
            }
            String name = matcher.group("name").trim();
            String unparsedType = matcher.group("type").trim();
            String unparsedDefault = matcher.group("value");

            if (arguments.containsKey(name)) {
                Skript.error("Duplicate method arguments");
                return null;
            }
            ClassInfo<?> classInfo = Classes.getClassInfoFromUserInput(unparsedType);
            boolean isPlural = Utils.isPlural(unparsedType).plural();
            if (classInfo == null) {
                Skript.error("Unable to resolve type: %s", unparsedType);
                return null;
            }
            Class<?> type = Utils.getComponentType(classInfo.getC());
            DefaultValue<?> defaultValue = null;

            if (unparsedDefault != null) {
                defaultValue = new DefaultValue.Dynamic<>(unparsedDefault, type, isPlural);
                String variableName = unparsedDefault.endsWith("*") ? unparsedDefault.substring(0, unparsedDefault.length() - 3) + (!isPlural ? "::1" : "") : unparsedDefault;

                if (!Variable.isValidVariableName(variableName, true, false)) {
                    Skript.error("Invalid argument name: %s", variableName);
                    return null;
                }
            }
            MethodArgument argument = new MethodArgument(type, defaultValue, isPlural);
            arguments.put(name, argument);
        }
        return arguments;
    }
}
