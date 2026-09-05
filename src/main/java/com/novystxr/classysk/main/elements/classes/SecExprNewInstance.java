package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.*;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.classes.*;
import com.novystxr.classysk.api.classes.SkriptClass.AnonymousClass;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.methods.MethodRegistry.MethodIdentifier;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.api.methods.SkriptMethod.AnonymousMethod;
import com.novystxr.classysk.api.util.ParserUtils;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.main.elements.methods.SecMethod;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.ArrayList;
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
@Since("1.0.0")
public class SecExprNewInstance extends SectionExpression<ClassInstance> {

    private static final Pattern VALID_NODE_PATTERN = Pattern.compile("("+ Classysk.NAME_PATTERN +"): (.+)");

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EXPRESSION,
            DefaultSyntaxInfos.Expression.builder(SecExprNewInstance.class, ClassInstance.class)
                .addPattern("[a] new [instance of] <"+ Classysk.CLASSNAME_PATTERN +">")
                .supplier(SecExprNewInstance::new)
                .priority(SyntaxInfo.COMBINED)
                .build()
        );
    }

    private SkriptClass skriptClass;
    private final Map<String, Expression<?>> fields = new HashMap<>();

    private AnonymousClass anonymous = null;

    private String name;

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, ParseResult result, @Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
        name = StringUtils.getLowerCase(result.regexes.getFirst());
        if (!ClassManager.classExists(name)) {
            Skript.error("Class named " + name + " does not exist");
            return false;
        }
        skriptClass = ClassManager.getClass(name);
        boolean inParent = SkriptMethod.getContextClass(getParser()) == skriptClass;

        List<SecMethod> methods = new ArrayList<>();
        List<SkriptMethod> abstractMethods = skriptClass.methodRegistry.getAbstract();
        if (sectionNode != null) {
            for (Node node : sectionNode) {
                String key = node.getKey();
                if (key == null) throw new IllegalStateException("Got node with null key");

                Matcher matcher = VALID_NODE_PATTERN.matcher(key);
                if (matcher.matches()) {
                    String fieldName = StringUtils.getConfigLowerCase(matcher.group(1));
                    String unparsedValue = matcher.group(2);

                    SkriptField field = skriptClass.getField(fieldName);
                    if (field == null) {
                        Skript.error("Could not find field from class: " + skriptClass.getEffectiveName());
                        return false;
                    }
                    if (field.isStatic()) {
                        Skript.error("Static field cannot be set on an instance");
                        return false;
                    }
                    if (field.accessType() == Modifier.PRIVATE && !inParent) {
                        Skript.error("Private fields can't be accessed here");
                        return false;
                    }
                    Expression<?> valueExpr = ParserUtils.parseExprNode(unparsedValue, node, field.type());
                    if (valueExpr == null) return false;

                    fields.put(fieldName, valueExpr);
                } else {
                    SecMethod secMethod = ParserUtils.parseNodeAsInfos(node, "Invalid entry: " + key, SecMethod.ANONYMOUS_INFO);
                    if (secMethod == null) return false;

                    if (!skriptClass.hasModifier(Modifier.ABSTRACT)) {
                        Skript.error("Only abstract classes can be implemented anonymously");
                        return false;
                    }
                    SkriptMethod target = skriptClass.getExactMethod(MethodIdentifier.from(secMethod.result), false);
                    SkriptMethod method = secMethod.result;
                    abstractMethods.remove(target);

                    if (target == null) {
                        Skript.error("Anonymous methods must override an existing method");
                        return false;
                    }
                    if (method.accessType() != null && target.accessType() != method.accessType()) {
                        Skript.error("Access type cannot be changed on an anonymous override.");
                        return false;
                    }
                    method.modifiers = Modifier.collect(target.accessType(), Modifier.OVERRIDE);
                    if (!method.validateOverride(target)) {
                        return false;
                    }
                    method.modifiers = Modifier.collect(target.accessType(), Modifier.OVERRIDE);
                    secMethod.result = new AnonymousMethod(method);

                    if (anonymous == null) {
                        anonymous = new AnonymousClass(name);
                    }
                    if (!secMethod.register(anonymous)) {
                        Skript.error("Method with that signature already exists");
                        return false;
                    }
                    methods.add(secMethod);
                }
            }
        }
        for (SecMethod secMethod : methods) {
            secMethod.loadTrigger();
        }
        if (!abstractMethods.isEmpty()) {
            Skript.error("%s abstract method(s) need to be implemented anonymously to create an instance of '%s'", abstractMethods.size(), StringUtils.titleCase(name));
            return false;
        }

        return true;
    }

    @Override
    protected ClassInstance @Nullable [] get(Event event) {
        ClassInstance newInstance;
        if (anonymous == null) {
            newInstance = skriptClass.createInstance();
        } else {
            newInstance = new AnonymousInstance(name, anonymous, event);
            anonymous.setupInstance(newInstance);
        }

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
