package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.validate.SectionValidator;
import ch.njol.skript.lang.*;
import ch.njol.skript.log.SkriptLogger;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.classes.ClassOption;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.event.FieldRegistrationEvent;
import com.novystxr.classysk.api.event.MethodRegistrationEvent;
import com.novystxr.classysk.api.util.ClassyStringUtils;
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
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;

import java.util.*;

public class StructClass extends Structure {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.STRUCTURE,
                SyntaxInfo.Structure.builder(StructClass.class)
                        .addPattern("class <"+ Classysk.classNamePattern +">")
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
        return true;
    }

    @Override
    public boolean preLoad() {
        Script script = getParser().getCurrentScript();
        SkriptClass newClass = null;

        // check for existing class and validate
        if (ClassManager.classExists(name)) {
            newClass = ClassManager.getClass(name);

            if (newClass.validateStructure()) {
                newClass.accessible = true;
            } else {
                newClass = null;
            }
        }

        // if no existing class or validation failed, new class
        if (newClass == null) {
            newClass = new SkriptClass(name, script);
            ClassManager.createClass(newClass);
        }

        Map<String, FieldSignature> fieldSignatures = new HashMap<>();
        List<SecMethod> methods = new ArrayList<>();
        newClass.initMethodRegistry();

        List<Node> nodes = this.entryContainer.getUnhandledNodes();

        // validate fields
        for (Node node : nodes) {
            SkriptLogger.setNode(node);
            if (!(node instanceof SectionNode) && node.getKey() != null) {
                Effect effect = Effect.parse(node.getKey(), "Invalid field declaration");

                if (effect instanceof EffField fieldEffect) {
                    FieldSignature signature = fieldEffect.getSignature(new FieldRegistrationEvent(newClass));

                    if (fieldSignatures.containsValue(signature)) {
                        Skript.error("You cannot have duplicate field signatures.");
                        continue;
                    }

                    if (signature != null) fieldSignatures.put(signature.name(), signature);
                }
            }
        }
        EntryValidator optionValidator = ClassOption.getValidator();
        // validate methods
        for (Node node : nodes) {
            if (node.getKey() == null) continue;
            if (node instanceof SectionNode sectionNode) {
                if (node.getKey().equals("options")) {
                    ClassOption.setOptions(newClass, optionValidator.validate(sectionNode));
                    continue;
                } else {
                    newClass.resetOptions();
                }

                Section section = Section.parse(node.getKey(), null, sectionNode, null);

                if (section instanceof SecMethod secMethod) {
                    secMethod.contextClass = newClass;
                    secMethod.walk(new MethodRegistrationEvent(newClass));
                    methods.add(secMethod);
                }
            }
        }
        // evaluate method triggers after initial registration so it will always know about other methods within a class
        if (!fieldSignatures.isEmpty()) {
            newClass.updateFieldSignatureMap(fieldSignatures);
        }
        ClassManager.checkAwaitingParent(newClass);

        for (SecMethod secMethod : methods) {
            secMethod.evaluateTrigger();
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
        SkriptClass skriptClass = ClassManager.getClass(name);
        skriptClass.accessible = false;

    }

    @Override
    public String toString(Event event, boolean debug) {
        return "Class";
    }
}
