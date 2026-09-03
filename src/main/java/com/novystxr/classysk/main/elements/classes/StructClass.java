package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.log.SkriptLogger;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.methods.MethodRegistry.MethodIdentifier;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.api.util.ParserUtils;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.main.elements.fields.EffField;
import com.novystxr.classysk.main.elements.methods.SecMethod;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.DefaultSyntaxInfos.Structure.NodeType;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.*;

import static com.novystxr.classysk.Classysk.CLASSNAME_PATTERN;

@Name("Class")
@Description("Creates a class with the specified fields and methods, see the [**Official Wiki**](https://github.com/novystar/ClassySK/wiki/Tutorials%3A-Classes-%26-Fields) for more detail")
@Examples("""
    class PlayerStats:
    \tpublic kills: integer
    \tpublic deaths: integer
    \tpublic xp: number

    \tprivate static levelXP: number = 100

    \tpublic getRatio() :: number:
    \t\treturn self::kills/self::deaths

    \tpublic getRequiredXP() :: number:
    \t\tset {_levelXP} to PlayerStats::levelXP
    \t\treturn {_levelXP} - mod(self::xp, {_levelXP})

    \tpublic getLevel() :: number:
    \t\treturn 1 + floor(self::xp / PlayerStats::levelXP)
    """)
@Since("1.0.0")
public class StructClass extends Structure {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.STRUCTURE,
            SyntaxInfo.Structure.builder(StructClass.class)
                .addPattern("[:final|:abstract] class <"+ CLASSNAME_PATTERN +"> [:extends <" + CLASSNAME_PATTERN + ">]")
                .nodeType(NodeType.BOTH)
                .supplier(StructClass::new)
                .build()
        );
    }

    private final List<SecMethod> methodSyntaxes = new ArrayList<>();
    private final List<EffField> fieldSyntaxes = new ArrayList<>();

    private SkriptClass newClass;

    private String name;
    private String extendsName;
    private Node node;

    @Override
    public boolean init(Literal<?>[] args, int pattern, ParseResult result, @UnknownNullability EntryContainer entryContainer) {
        name = StringUtils.getLowerCase(result.regexes.getFirst());
        extendsName = result.hasTag("extends") ? StringUtils.getLowerCase(result.regexes.get(1)) : null;

        if (ClassManager.classExists(name)) {
            Skript.error("A class named '%s' already exists", name);
            return false;
        }
        newClass = new SkriptClass(name, extendsName, Modifier.collect(result.tags));

        for (Node node : entryContainer.getUnhandledNodes()) {
            var element = ParserUtils.parseNodeAsInfos(node, "Could not recognize entry: "+node.getKey(), EffField.INFO, SecMethod.INFO);

            if (element instanceof EffField effField) {
                String fieldName = effField.name;
                if (newClass.fields.putIfAbsent(fieldName, effField.withOrigin(name)) == null) {
                    fieldSyntaxes.add(effField);
                } else {
                    Skript.error("Field named '"+fieldName+"' already exists in this class");
                    return false;
                }
            } else if (element instanceof SecMethod secMethod) {
                if (secMethod.result.hasModifier(Modifier.ABSTRACT) && !newClass.hasModifier(Modifier.ABSTRACT)) {
                    Skript.error("Abstract methods can't be used here");
                    return false;
                }
                if (secMethod.result.hasModifier(Modifier.OVERRIDE) && extendsName == null) {
                    Skript.error("This class does not extend any other");
                    return false;
                }
                if (secMethod.register(newClass, name)) {
                    methodSyntaxes.add(secMethod);
                } else {
                    Skript.error("Method with that signature already exists in this class");
                    return false;
                }
            } else {
                return false;
            }
        }
        if (newClass.hasModifier(Modifier.FINAL) && newClass.methodRegistry.hasAbstract()) {
            Skript.error("A final class cannot have abstract methods.");
            return false;
        }
        ClassManager.registerClass(newClass);

        if (cyclic()) {
            Skript.error("Cyclic inheritance is not allowed. (feeding back into itself)");
            unregisterClass();
            return false;
        }
        this.node = getParser().getNode();
        return true;
    }

    @Override
    public boolean preLoad() {
        if (!parseDefaults()) {
            unregisterClass();
            return false;
        }
        SkriptClass target = newClass.getExtends();
        if (target != null) {
            if (target.hasModifier(Modifier.FINAL)) {
                Skript.error("Can't extend a class that is final");
                unregisterClass();
                return false;
            }
            if (target == newClass) {
                Skript.error("A class cannot extend itself");
                unregisterClass();
                return false;
            }
            if (!validateOverrides(target)) {
                unregisterClass();
                return false;
            }
        } else if (extendsName != null) {
            Skript.error("Class named '%s' does not exist", StringUtils.titleCase(newClass.extendsName));
            unregisterClass();
            return false;
        }
        fieldSyntaxes.clear();
        ClassManager.checkAwaitingParent(newClass);
        ClassManager.revalidateFields(newClass);
        return true;
    }

    @Override
    public boolean load() {
        for (SecMethod method : methodSyntaxes) {
            method.loadTrigger();
        }
        methodSyntaxes.clear();
        return true;
    }

    @Override
    public void unload() {
        unregisterClass();
    }

    private void unregisterClass() {
        ClassManager.removeClass(name);
    }

    private boolean cyclic() {
        Set<SkriptClass> matches = new HashSet<>();
        return newClass.inheritanceStream()
            .anyMatch(target -> !matches.add(target));
    }

    private boolean parseDefaults() {
        for (EffField field : fieldSyntaxes) {
            if (!field.parseDefault())
                return false;
        }
        for (SecMethod method : methodSyntaxes) {
            if (!method.parseDefaults())
                return false;
        }
        SkriptLogger.setNode(node);
        return true;
    }

    private boolean validateOverrides(@NotNull SkriptClass target) {
        for (EffField field : fieldSyntaxes) {
            SkriptLogger.setNode(field.getNode());

            if (target.fieldExists(field.name)) {
                Skript.error("Field names must be unique to any super classes.");
                return false;
            }
        }
        List<SkriptMethod> abstractMethods = target.methodRegistry.getAbstract();
        for (SecMethod methodSyntax : methodSyntaxes) {
            SkriptLogger.setNode(methodSyntax.getNode());

            SkriptMethod method = methodSyntax.result;
            SkriptMethod overridden = target.getExactMethod(MethodIdentifier.from(method), false);

            if (overridden == null) {
                if (method.hasModifier(Modifier.OVERRIDE)) {
                    Skript.error("This method does not override any from it's extending class");
                    return false;
                }
                continue;
            }
            abstractMethods.remove(overridden);

            if (!method.validateOverride(overridden)) {
                return false;
            }
        }
        SkriptLogger.setNode(node);
        if (!abstractMethods.isEmpty()) {
            Skript.error("%s abstract method(s) from the super class were not implemented.", abstractMethods.size());
            return false;
        }
        return true;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "Class "+name;
    }
}