package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import com.novystxr.classysk.api.AbstractSkriptClass;
import com.novystxr.classysk.api.ClassManager;
import com.novystxr.classysk.api.event.FieldRegistrationEvent;
import com.novystxr.classysk.api.event.MethodRegistrationEvent;
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
import com.novystxr.classysk.api.SkriptField.FieldSignature;

import java.util.*;
import java.util.regex.MatchResult;

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
        MatchResult regex = parseResult.regexes.getFirst();
        name = regex.group(1).trim().toLowerCase(Locale.ENGLISH);

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

        Map<String, FieldSignature> fieldSignatures = new TreeMap<>();
        abstractSkriptClass.initMethodSignatures();

        for (Node node : this.entryContainer.getUnhandledNodes()) {

            if (node instanceof SectionNode sectionNode) {

                if (node.getKey() == null) continue;

                Section section = Section.parse(node.getKey(), null, sectionNode, null);

                if (section instanceof SecMethod secMethod) {
                    secMethod.walk(new MethodRegistrationEvent(abstractSkriptClass));
                } else {
                    Skript.error("Invalid Method Declaration");
                }

            } else if (node.getKey() != null) {

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

        if (!fieldSignatures.isEmpty()) {
            abstractSkriptClass.updateFieldSignatureMap(fieldSignatures);
        }

        return true;
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
