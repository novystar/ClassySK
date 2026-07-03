package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.SkriptLogger;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.classes.ClassOption;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.main.elements.fields.EffField;
import com.novystxr.classysk.main.elements.methods.SecMethod;
import org.bukkit.event.Event;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.*;

@Name("Class")
@Description({
    "Creates a class, see the [**Official Wiki**](https://github.com/novystar/ClassySK/wiki/Tutorials%3A-Classes-%26-Fields) for more detail",
    "",
    "`Options` - Optional section which is used to set certain rules about how your class should be used.",
    "- `storable (true)` - The class instance may be saved between restarts",
    "- `external creation (true)` - Instances can be created from outside of the class",
    "- `strict signature enforcement (false)` - Invalid fields will be aggressively removed from all existing instances when it's structure is updated",
    "- `private access on create (false)` - Private fields can be set within constructors from outside the class",
    "- `suppress runtime errors (false)` - Runtime errors regarding instances of this class will be ignored"
})
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
@Since("1.0")
public class StructClass extends Structure {
    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.STRUCTURE,
            SyntaxInfo.Structure.builder(StructClass.class)
                .addPattern("class <"+ Classysk.CLASSNAME_PATTERN +">")
                .supplier(StructClass::new)
                .nodeType(DefaultSyntaxInfos.Structure.NodeType.BOTH)
                .build()
        );
    }

    private EntryContainer entryContainer;
    private String name;

    private final List<SecMethod> methodSections = new ArrayList<>();

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @UnknownNullability EntryContainer entryContainer) {
        this.entryContainer = entryContainer;
        name = StringUtils.getLowerCase(parseResult.regexes.getFirst());

        if (classAlreadyExists()) {
            Skript.error("A class structure named '%s' already exists in a script", name);
            return false;
        }

        return true;
    }

    @Override
    public boolean preLoad() {
        Script script = getParser().getCurrentScript();
        SkriptClass newClass = ClassManager.getClass(name);

        // if no valid class exists, put a new one
        if (newClass != null && newClass.validateStructure()) {
            newClass.accessible = true;
        } else {
            newClass = new SkriptClass(name, script);
            ClassManager.createClass(name, newClass);
        }

        newClass.fieldSignatures.clear();
        newClass.methodRegistry.init();

        List<Node> nodes = this.entryContainer.getUnhandledNodes();

        // register fields
        for (Node node : nodes) {
            SkriptLogger.setNode(node);
            if (!(node instanceof SectionNode) && node.getKey() != null) {
                Effect effect = Effect.parse(node.getKey(), "Invalid field pattern");

                if (effect instanceof EffField fieldEffect) {
                    fieldEffect.registerField(newClass);
                }
            }
        }
        newClass.revalidateFields();
        EntryValidator optionValidator = ClassOption.getValidator();

        newClass.resetOptions();
        boolean hasOptions = false;

        // register methods & options
        for (Node node : nodes) {
            if (node.getKey() == null) continue;

            if (node instanceof SectionNode sectionNode) {
                if (node.getKey().equals("options")) {
                    if (hasOptions) {
                        Skript.error("Options section can only occur once");
                    } else {
                        ClassOption.setOptions(newClass, optionValidator.validate(sectionNode));
                        hasOptions = true;
                    }
                    continue;
                }
                Section section = Section.parse(node.getKey(), "Invalid method pattern", sectionNode, null);

                if (section instanceof SecMethod secMethod) {
                    secMethod.contextClass = newClass;
                    secMethod.registerMethod();
                    methodSections.add(secMethod);
                }
            }
        }
        ClassManager.checkAwaitingParent(newClass);
        return true;
    }

    @Override
    public boolean load() {
        // load method triggers after initial registration so it will always know about other methods within a class
        for (SecMethod secMethod : methodSections) {
            secMethod.loadTrigger();
        }
        methodSections.clear();
        return true;
    }

    @Override
    public void unload() {
        SkriptClass skriptClass = ClassManager.getClass(name);
        skriptClass.accessible = false;
    }

    private boolean classAlreadyExists() {
        boolean exists = false;

        for (Structure structure : getParser().getCurrentScript().getStructures()) {
            if (structure instanceof StructClass structClass) {
                if (structClass == this) continue;
                if (!structClass.name.equals(name)) continue;
                exists = true;
            }
        }
        if (ClassManager.classExists(name)) {
            if (ClassManager.getClass(name).getValidScript() == getParser().getCurrentScript()) exists = true;
        }
        return exists;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "Class "+name;
    }
}