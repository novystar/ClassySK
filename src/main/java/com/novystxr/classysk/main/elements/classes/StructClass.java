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
                .addPattern("class <"+ CLASSNAME_PATTERN +"> [:extends <"+CLASSNAME_PATTERN+">]")
                .nodeType(NodeType.BOTH)
                .supplier(StructClass::new)
                .build()
        );
    }

    private final List<SecMethod> methodSections = new ArrayList<>();
    private SkriptClass newClass;

    private String name;

    @Override
    public boolean init(Literal<?>[] args, int pattern, ParseResult result, @UnknownNullability EntryContainer entryContainer) {
        this.name = StringUtils.getLowerCase(result.regexes.getFirst());

        if (ClassManager.classExists(name)) {
            Skript.error("A class structure named '%s' already exists in a script", name);
            return false;
        }
        newClass = new SkriptClass(name);

        newClass.extendsName = result.hasTag("extends")
            ? StringUtils.getLowerCase(result.regexes.get(1)) : null;

        for (Node node : entryContainer.getUnhandledNodes()) {
            var element = ParserUtils.parseNodeAsInfos(node, "Could not recognize entry: "+node.getKey(), EffField.INFO, SecMethod.INFO);

            if (element instanceof EffField effField) {
                String fieldName = effField.fieldName;
                if (newClass.fieldSignatures.putIfAbsent(fieldName, effField.signature) != null) {
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
        ClassManager.registerClass(newClass);
        return true;
    }

    @Override
    public boolean preLoad() {
        if (newClass.extendsName != null) {
            SkriptClass extendsClass = newClass.extendsClass();
            if (extendsClass == null) {
                Skript.error("Class named '%s' does not exist", StringUtils.titleCase(newClass.extendsName));
                unregisterClass();
                return false;
            }
            if (extendsClass == newClass) {
                Skript.error("A class cannot extend itself");
                unregisterClass();
                return false;
            }
            if (cyclic()) {
                Skript.error("Cyclic inheritance is not allowed. (feeding back into itself)");
                unregisterClass();
                return false;
            }
        }
        if (!newClass.methodRegistry.validateOverrides(newClass.extendsClass())) {
            unregisterClass();
            return false;
        }
        return true;
    }

    @Override
    public boolean load() {
        ClassManager.checkAwaitingParent(newClass);
        ClassManager.revalidateFields(newClass);

        // load method triggers after initial registration so it will always know about other methods within a class
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

    private boolean cyclic() {
        List<SkriptClass> matches = new ArrayList<>();
        return newClass.inheritanceStream().anyMatch(target -> {
            if (matches.contains(target))
                return true;
            matches.add(target);
            return false;
        });
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "Class "+name;
    }
}