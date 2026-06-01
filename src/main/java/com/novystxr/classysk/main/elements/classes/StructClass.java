package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import com.novystxr.classysk.api.classes.AbstractSkriptClass;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.ParserClassData;
import com.novystxr.classysk.api.event.FieldRegistrationEvent;
import com.novystxr.classysk.api.event.MethodRegistrationEvent;
import com.novystxr.classysk.api.util.ClassyStringUtils;
import com.novystxr.classysk.main.elements.fields.EffField;
import com.novystxr.classysk.main.elements.methods.SecMethod;
import org.bukkit.event.Event;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;

import java.util.*;

public class StructClass extends Structure {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.STRUCTURE,
                SyntaxInfo.Structure.builder(StructClass.class)
                        .addPattern("class <(\\w+)>")
                        .supplier(StructClass::new)
                        .nodeType(DefaultSyntaxInfos.Structure.NodeType.BOTH)
                        .build()

        );
    }

    private EntryContainer entryContainer;
    private String name;

    public String getName() {
        return name;
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @UnknownNullability EntryContainer entryContainer) {

        this.entryContainer = entryContainer;
        name = ClassyStringUtils.getLowerCase(parseResult.regexes.getFirst());

        if (classAlreadyExists()) {
            Skript.error("A class structure named '%s' already exists in a script", name);
            return false;
        }

        if (name.equals("instance")) {
            Skript.error("A class can't be named 'instance' as this would create conflicts");
            return false;
        }

        return true;
    }

    @Override
    public boolean preLoad() {
        Script script = getParser().getCurrentScript();
        AbstractSkriptClass abstractSkriptClass = null;

        // check for existing class and validate
        if (ClassManager.classExists(name)) {
            abstractSkriptClass = ClassManager.getClass(name);

            if (abstractSkriptClass.validateStructure()) {
                abstractSkriptClass.accessible = true;
            } else {
                abstractSkriptClass = null;
            }
        }

        // if no existing class or validation failed, new class
        if (abstractSkriptClass == null) {
            abstractSkriptClass = new AbstractSkriptClass(name, script);
            ClassManager.createClass(abstractSkriptClass);
        }

        Map<String, FieldSignature> fieldSignatures = new HashMap<>();
        List<SecMethod> methods = new ArrayList<>();
        abstractSkriptClass.initMethodRegistry();

        List<Node> nodes = this.entryContainer.getUnhandledNodes();

        // validate fields
        for (Node node : nodes) {
            if (!(node instanceof SectionNode) && node.getKey() != null) {
                Effect effect = Effect.parse(node.getKey(), "Invalid field declaration");

                if (effect instanceof EffField fieldEffect) {
                    FieldSignature signature = fieldEffect.getSignature(new FieldRegistrationEvent(abstractSkriptClass));

                    if (fieldSignatures.containsValue(signature)) {
                        Skript.error("You cannot have duplicate field signatures.");
                        continue;
                    }

                    if (signature != null) fieldSignatures.put(signature.name(), signature);
                } else {
                    Skript.error("You can only define fields and methods here");
                }
            }
        }
        // validate methods
        for (Node node : nodes) {
            if (node instanceof SectionNode sectionNode) {
                if (node.getKey() == null) continue;

                Section section = Section.parse(node.getKey(), null, sectionNode, null);

                if (section instanceof SecMethod secMethod) {
                    secMethod.walk(new MethodRegistrationEvent(abstractSkriptClass));
                    methods.add(secMethod);
                } else {
                    Skript.error("Invalid Method Declaration");
                }
            }
        }
        // evaluate method triggers after initial registration so it will always know about other methods within a class
        // requires extra parser data for context class during validate time access validation
        ParserClassData data = getParser().getData(ParserClassData.class);
        data.skriptClass = abstractSkriptClass;

        for (SecMethod secMethod : methods) {
            secMethod.evaluateTrigger();
        }
        data.skriptClass = null;

        if (!fieldSignatures.isEmpty()) {
            abstractSkriptClass.updateFieldSignatureMap(fieldSignatures);
        }
        return true;
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

    @Override
    public boolean load() {
        return true;
    }

    @Override
    public void unload() {
        AbstractSkriptClass abstractSkriptClass = ClassManager.getClass(name);
        abstractSkriptClass.accessible = false;

    }

    @Override
    public String toString(Event event, boolean debug) {
        return "Class";
    }



}
