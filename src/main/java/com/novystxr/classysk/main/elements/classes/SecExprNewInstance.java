package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.doc.*;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.classes.ClassOption;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.api.util.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Name("New Instance")
@Keywords({"constructor", "create class", "creation"})
@Description("Create a new instance of a class, you can optionally supply fields as a section")
@Example("set {_myClass} to new instance of MyClass")
@Example("""
    set {_myClass} to new instance of PlayerStats:
    \tkills: 30
    \tbalance: 5000
    """)
@Since("1.0")
public class SecExprNewInstance extends SectionExpression<ClassInstance> {

    private static final Pattern VALID_NODE_PATTERN = Pattern.compile("("+ Classysk.NAME_PATTERN +"): (.+)");

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EXPRESSION,
            DefaultSyntaxInfos.Expression.builder(SecExprNewInstance.class, ClassInstance.class)
                .addPattern("[a] new instance of <"+ Classysk.CLASSNAME_PATTERN +">")
                .supplier(SecExprNewInstance::new)
                .build()
        );
    }

    private SkriptClass skriptClass;
    private final Map<String, Expression<?>> fields = new HashMap<>();

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, ParseResult result, @Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
        String name = StringUtils.getLowerCase(result.regexes.getFirst());
        if (!ClassManager.isAccessible(name)) {
            Skript.error("Class named " + name + " does not exist");
            return false;
        }
        skriptClass = ClassManager.getClass(name);
        boolean inParent = SkriptMethod.getContextClass(getParser()) == skriptClass;

        if (!inParent && !skriptClass.option(ClassOption.EXTERNAL_CREATION)) {
            Skript.error("External creation is not permitted for this class");
            return false;
        }
        if (sectionNode == null) return true;

        boolean allowPrivate = inParent || skriptClass.option(ClassOption.PRIVATE_ACCESS_ON_CREATE);

        for (Node node : sectionNode) {
            String key = node.getKey();
            if (key == null) throw new IllegalStateException("Got node with null key");

            if (node instanceof SimpleNode) {
                Matcher matcher = VALID_NODE_PATTERN.matcher(key);
                if (!matcher.matches()) {
                    Skript.error("Invalid field name: " + key);
                    return false;
                }
                String fieldName = StringUtils.getLowerCase(matcher.group(1));
                String unparsedValue = matcher.group(2);

                FieldSignature signature = skriptClass.getFieldSignature(fieldName);
                if (signature == null) {
                    Skript.error("Could not find field from class: " + skriptClass.getEffectiveName());
                    return false;
                }
                if (signature.isStatic()) {
                    Skript.error("Static field cannot be set on an instance");
                    return false;
                }
                if (signature.accessType().isPrivate() && !allowPrivate) {
                    Skript.error("This class does not permit private access in constructors");
                    return false;
                }
                Expression<?> valueExpr = LiteralUtils.defendExpression(new SkriptParser(unparsedValue, SkriptParser.ALL_FLAGS, ParseContext.DEFAULT).parseExpression(signature.type()));
                if (valueExpr == null || !LiteralUtils.canInitSafely(valueExpr)) return false;

                fields.put(fieldName, valueExpr);
            } else {
                Skript.error("Sections are not allowed here");
                return false;
            }
        }
        return true;
    }

    @Override
    protected ClassInstance @Nullable [] get(Event event) {
        SkriptClass parent = skriptClass;
        ClassInstance newInstance = parent.createInstance();

        for (Entry<String, Expression<?>> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            Expression<?> valueExpr = entry.getValue();

            Object[] value = valueExpr.getArray(event);
            newInstance.setFieldValue(fieldName, value);
        }
        return new ClassInstance[]{newInstance};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends ClassInstance> getReturnType() {
        return ClassInstance.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "new instance of class "+skriptClass.getEffectiveName();
    }
}
