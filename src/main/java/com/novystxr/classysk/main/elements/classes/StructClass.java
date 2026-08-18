package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.util.ParserUtils;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.main.elements.fields.EffField;
import com.novystxr.classysk.main.elements.methods.SecMethod;
import org.bukkit.event.Event;
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
                .addPattern("[:final] class <"+ CLASSNAME_PATTERN +"> [:extends <" + CLASSNAME_PATTERN + ">]")
                .nodeType(NodeType.BOTH)
                .supplier(StructClass::new)
                .build()
        );
    }

    private final List<SecMethod> methodSections = new ArrayList<>();
    private SkriptClass newClass;

    private String name;
    private String extendsName;

    @Override
    public boolean init(Literal<?>[] args, int pattern, ParseResult result, @UnknownNullability EntryContainer entryContainer) {
        name = StringUtils.getLowerCase(result.regexes.getFirst());
        extendsName = result.hasTag("extends") ? StringUtils.getLowerCase(result.regexes.get(1)) : null;
        boolean isFinal = result.hasTag("final");

        if (ClassManager.classExists(name)) {
            Skript.error("A class named '%s' already exists", name);
            return false;
        }
        newClass = new SkriptClass(name, extendsName, isFinal);

        for (Node node : entryContainer.getUnhandledNodes()) {
            var element = ParserUtils.parseNodeAsInfos(node, "Could not recognize entry: "+node.getKey(), EffField.INFO, SecMethod.INFO);

            if (element instanceof EffField effField) {
                String fieldName = effField.name;
                if (newClass.fieldSignatures.putIfAbsent(fieldName, effField.getSignature(name)) != null) {
                    Skript.error("Field named '"+fieldName+"' already exists in this class");
                    return false;
                }
            } else if (element instanceof SecMethod secMethod) {
                if (secMethod.registerMethod(newClass)) {
                    methodSections.add(secMethod);
                } else {
                    Skript.error("Method with that signature already exists in this class");
                    return false;
                }
            } else {
                return false;
            }
        }

        if (isFinal && newClass.methodRegistry.hasAbstract()) {
            Skript.error("A final class cannot have abstract methods.");
            return false;
        }
        ClassManager.registerClass(newClass);

        if (cyclic()) {
            Skript.error("Cyclic inheritance is not allowed. (feeding back into itself)");
            unregisterClass();
            return false;
        }
        return true;
    }

    @Override
    public boolean preLoad() {
        SkriptClass extendsClass = newClass.getExtends();
        if (extendsName != null) {
            if (checkFieldOverrides(extendsClass)) {
                Skript.error("Field names must be unique to their inheritors");
                unregisterClass();
                return false;
            }
            if (extendsClass == null) {
                Skript.error("Class named '%s' does not exist", StringUtils.titleCase(newClass.extendsName));
                unregisterClass();
                return false;
            }
            if (extendsClass.isFinal) {
                Skript.error("Can't extend a class that is final");
                unregisterClass();
                return false;
            }
            if (extendsClass == newClass) {
                Skript.error("A class cannot extend itself");
                unregisterClass();
                return false;
            }
        }
        if (!newClass.getRegistry().validateOverrides(extendsClass)) {
            unregisterClass();
            return false;
        }
        ClassManager.checkAwaitingParent(newClass);
        ClassManager.revalidateFields(newClass);
        return true;
    }

    @Override
    public boolean load() {
        for (SecMethod secMethod : methodSections) {
            secMethod.loadTrigger();
        }
        methodSections.clear();
        return true;
    }

    @Override
    public void unload() {
        unregisterClass();
    }

    private void unregisterClass() {
        ClassManager.removeClass(name);
    }

    private boolean checkFieldOverrides(SkriptClass extendsClass) {
        return newClass.fieldSignatures.keySet().stream()
            .anyMatch(name -> extendsClass.getFieldSignature(name) != null);
    }

    private boolean cyclic() {
        Set<SkriptClass> matches = new HashSet<>();
        return newClass.inheritanceStream()
            .anyMatch(target -> !matches.add(target));
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "Class "+name;
    }
}