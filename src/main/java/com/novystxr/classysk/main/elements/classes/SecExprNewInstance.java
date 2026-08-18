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
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.methods.AnonymousMethod;
import com.novystxr.classysk.api.methods.MethodRegistry.MethodIdentifier;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodSignature;
import com.novystxr.classysk.api.util.ParserUtils;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.main.elements.methods.SecMethod;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
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
                .build()
        );
    }

    private SkriptClass skriptClass;
    private final Map<String, Expression<?>> fields = new HashMap<>();

    private AnonymousClass anonymous = null;
    private final List<SecMethod> methods = new ArrayList<>();

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

        if (sectionNode != null) {
            for (Node node : sectionNode) {
                String key = node.getKey();
                if (key == null) throw new IllegalStateException("Got node with null key");

                Matcher matcher = VALID_NODE_PATTERN.matcher(key);
                if (matcher.matches()) {
                    String fieldName = StringUtils.getConfigLowerCase(matcher.group(1));
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
                    if (signature.accessType() == Modifier.PRIVATE && !inParent) {
                        Skript.error("Private fields can't be accessed here");
                        return false;
                    }
                    Expression<?> valueExpr = ParserUtils.parseExprNode(unparsedValue, node, signature.type());
                    if (valueExpr == null) return false;

                    fields.put(fieldName, valueExpr);
                } else {
                    SecMethod secMethod = ParserUtils.parseNodeAsInfos(node, "Invalid field name: " + key, SecMethod.ANONYMOUS_INFO);
                    if (secMethod == null) return false;

                    SkriptMethod target = skriptClass.getExactMethod(MethodIdentifier.from(secMethod.signature), false);
                    if (target == null) {
                        Skript.error("This method does not implement any from the target class.");
                        return false;
                    }
                    MethodSignature overridden = target.signature;
                    MethodSignature signature = secMethod.signature.withModifiers(overridden.accessType(), Modifier.OVERRIDE);

                    if (!overridden.hasModifier(Modifier.ABSTRACT)) {
                        Skript.error("Only abstract methods can be implemented here");
                        return false;
                    }
                    if (anonymous == null) {
                        anonymous = new AnonymousClass(name);
                    }
                    if (!secMethod.registerMethod(anonymous, new AnonymousMethod(signature, anonymous))) {
                        Skript.error("Method with that signature already exists");
                        return false;
                    }
                    methods.add(secMethod);
                }
            }
        }
        if (anonymous != null) {
            anonymous.methodRegistry.validateOverrides(skriptClass);
            for (SecMethod secMethod : methods) {
                secMethod.loadTrigger();
            }
        } else if (skriptClass.methodRegistry.hasAbstract()) {
            Skript.error("This class contains unimplemented methods so it cannot be instantiated directly. Implement it here or in a separate subclass.");
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
