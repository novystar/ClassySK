package com.novystxr.classysk.api.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import com.novystxr.classysk.api.util.ArgumentIterator;
import com.novystxr.classysk.api.util.DefaultValue;
import com.novystxr.classysk.api.util.DefaultValue.Empty;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.novystxr.classysk.Classysk.CLASSNAME_PATTERN;
import static com.novystxr.classysk.Classysk.NAME_PATTERN;

public class MethodParser {

    // argument components
    private static final String NAME = "(?<name>[_a-zA-Z0-9]+)";
    private static final String TYPE = "(?<type>[a-zA-Z\\d ]+)";
    private static final String VALUE = "(?<value>.+)";
    private static final String OPTIONAL = "(?<optional>\\?)?";

    // compiled argument patterns
    private static final Pattern DEF_ARG_PATTERN =
        Pattern.compile("^\\s*"+NAME+"\\s*"+OPTIONAL+"\\s*:\\s*"+TYPE+"(?:\\s*=\\s*"+VALUE+"+)?$");

    private static final Pattern REF_ARG_PATTERN =
        Pattern.compile("(?:\\s*"+NAME+":\\s)?"+VALUE);

    // syntax patterns
    public static final String METHOD_PATTERN = "(%-classinstance%|:super)\\:\\:<"+NAME_PATTERN+">\\([<.+>]\\)";
    public static final String STATIC_METHOD_PATTERN = "<"+CLASSNAME_PATTERN+">\\:\\:<"+NAME_PATTERN+">\\([<.+>]\\)";

    public record ReferenceArgument(
        @Nullable String name,
        Expression<?> expr
    ) {}

    public record MethodReference(
        String name,
        ReferenceArgument[] args,
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
                builder.append(Classes.getSuperClassInfo(arg.expr.getReturnType()));
                if (i != args.length) {
                    builder.append(", ");
                }
            }
            return builder.append(")").toString();
        }

    }

    public static @Nullable MethodReference parseReference(String name, @Nullable String args, boolean isStatic) {
        List<ReferenceArgument> result = new ArrayList<>();
        for (String arg : new ArgumentIterator(args)) {
            if (arg == null) {
                Skript.error("Invalid text/variables/parentheses in the arguments of this method call.");
                return null;
            }
            Matcher matcher = REF_ARG_PATTERN.matcher(arg);
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
            result.add(new ReferenceArgument(argName, expr));
        }
        return new MethodReference(name, result.toArray(ReferenceArgument[]::new), isStatic);
    }

    public static @Nullable MethodArgument[] parseArguments(@Nullable String args) {
        List<MethodArgument> arguments = new ArrayList<>();
        Set<String> nameSet = new HashSet<>(); // for checking duplicates

        for (String arg : new ArgumentIterator(args)) {
            if (arg == null) {
                Skript.error("Invalid text/variables/parentheses in the arguments of this method");
                return null;
            }
            Matcher matcher = DEF_ARG_PATTERN.matcher(arg);
            if (!matcher.matches()) {
                Skript.error("Invalid method argument(s)");
                return null;
            }
            String name = matcher.group("name").trim();
            String unparsedType = matcher.group("type").trim();
            String unparsedDefault = matcher.group("value");
            boolean optional = matcher.group("optional") != null;

            if (!nameSet.add(name)) {
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
            DefaultValue<?> defaultValue = unparsedDefault != null
                ? new DefaultValue.Dynamic<>(unparsedDefault, type, isPlural)
                : optional ? new Empty<>(type) : null;

            arguments.add(new MethodArgument(name, type, defaultValue, isPlural));
        }
        return arguments.toArray(MethodArgument[]::new);
    }
}
