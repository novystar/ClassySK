package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.*;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.*;
import com.novystxr.classysk.api.AccessModifiable.AccessType;
import com.novystxr.classysk.api.methods.MethodParser;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodSignature;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.event.MethodRegistrationEvent;
import com.novystxr.classysk.api.event.MethodRunEvent;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.main.elements.classes.StructClass;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.*;

@Name("Method")
@Keywords("function")
@Description({
    "Methods are functions belonging to a specific class. The signature is very similar to that of regular functions.",
    "See the [**Official Wiki**](https://github.com/novystar/ClassySK/wiki/Tutorials%3A-Methods-%26-Access-Modifiers) for more information."
})
@Example("""
    class Example:
    \tpublic points: number
    
    \tprivate static sayMessage(message: text):
    \t\tbroadcast {_message} # our message argument is available in '{_message}'
    
    \tpublic giveApple(amount: number) :: item:
    \t\treturn {_amount} of apple
    
    \tpublic getPoints() returns number:
    \t\treturn self::points
    """)
@Example("""
    Counter::increment(1)
    set {_count} to Counter::getValue()
    """)
@Example("""
    {_myClass}::setPlayer(player)
    set {_player} to {_myClass}::getPlayer()
    """)
@Since("1.0")
public class SecMethod extends Section implements ReturnHandler<Object> {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.SECTION,
                SyntaxInfo.builder(SecMethod.class)
                        .addPattern("(public|:private) [:static] <"+ Classysk.NAME_PATTERN +">\\([args:<.+>]\\) [return:(\\:\\:|returns) <.+>]")
                        .supplier(SecMethod::new)
                        .build()
        );
    }

    private SectionNode sectionNode;
    private MethodSignature signature;

    private SkriptMethod skriptMethod;
    public SkriptClass contextClass;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        if (!(getParser().getCurrentStructure() instanceof StructClass)) {
            Skript.error("Method declaration can only be used within a class structure.");
            return false;
        }
        boolean returnPlural = false;
        Class<?> returnType = null;

        if (parseResult.hasTag("return")) {
            String ref = parseResult.regexes.get(1).group().trim();
            returnType = Classes.getClassFromUserInput(ref);
            if (returnType == null) {
                Skript.error("Could not resolve type: "+ref);
                return false;
            }
            returnPlural = Utils.isPlural(ref).plural();
        }
        String methodName = StringUtils.getLowerCase(parseResult.regexes.get(0));
        SequencedMap<String, MethodArgument> args = null;

        // parse arguments
        if (parseResult.hasTag("args")) {
            String argsString = parseResult.regexes.get(1).group();

            if (!argsString.isEmpty()) {
                args = MethodParser.parseArguments(argsString);
                if (args == null) {
                    return false;
                }
            }
        }
        AccessType accessType = parseResult.hasTag("private") ? AccessModifiable.AccessType.PRIVATE : AccessModifiable.AccessType.PUBLIC;
        boolean isStatic = parseResult.hasTag("static");

        this.sectionNode = sectionNode;
        this.signature = new MethodSignature(methodName, args, accessType, isStatic, returnType, returnPlural);
        return true;
    }

    // here we are just doing registration
    @Override
    public @Nullable TriggerItem walk(Event event) {
        if (event instanceof MethodRegistrationEvent regEvent) {

            SkriptClass skriptClass = regEvent.skriptClass;
            skriptMethod = new SkriptMethod(signature, skriptClass);

            boolean registered = skriptClass.methodRegistry.registerMethod(skriptMethod);
            if (!registered) {
                Skript.error("Method with this signature already exists in class: "+ skriptClass.name);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void evaluateTrigger() {
        Trigger trigger;

        if (signature.returnType() != null) {
            trigger = loadReturnableSectionCode(sectionNode, "method body", new Class[]{MethodRunEvent.class});
        } else {
            trigger = loadCode(sectionNode, "method body", MethodRunEvent.class);
        }
        skriptMethod.setTrigger(trigger);
    }

    @Override
    public void returnValues(Event event, Expression<?> value) {
        if (event instanceof MethodRunEvent runEvent) {
            runEvent.returnObject = value.getArray(event);
        }
    }

    @Override
    public boolean isSingleReturnValue() {
        return !signature.returnPlural();
    }

    @Override
    public @Nullable Class<?> returnValueType() {
        return signature.returnType();
        }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "method declaration";
    }
}
