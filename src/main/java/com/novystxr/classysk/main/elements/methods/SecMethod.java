package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.ClassInfoReference;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.*;
import com.novystxr.classysk.api.AccessModifiable.AccessType;
import com.novystxr.classysk.api.methods.MethodParser;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodSignature;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.event.MethodRunEvent;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.api.util.ExprUtils;
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
                .addPattern("(public|:private) [:static] <"+ Classysk.NAME_PATTERN +">\\([args:<.+>]\\) [(\\:\\:|returns) %-*classinfo%]")
                .supplier(SecMethod::new)
                .build()
        );
    }

    private SectionNode sectionNode;
    private MethodSignature signature;

    private SkriptMethod skriptMethod;
    public SkriptClass contextClass;

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        if (!(getParser().getCurrentStructure() instanceof StructClass)) {
            Skript.error("Method declaration can only be used within a class structure.");
            return false;
        }
        boolean returnPlural = false;
        Class<?> returnType = null;

        if (exprs[0] != null) {
            ClassInfoReference reference = ExprUtils.getClassRef(exprs[0]);
            returnPlural = reference.isPlural().isTrue();
            returnType = reference.getClassInfo().getC();
        }
        String methodName = StringUtils.getLowerCase(result.regexes.get(0));
        SequencedMap<String, MethodArgument> args = null;

        // parse arguments
        if (result.hasTag("args")) {
            String argsString = result.regexes.get(1).group();

            if (!argsString.isEmpty()) {
                args = MethodParser.parseArguments(argsString);
                if (args == null) {
                    return false;
                }
            }
        }
        AccessType accessType = result.hasTag("private") ? AccessModifiable.AccessType.PRIVATE : AccessModifiable.AccessType.PUBLIC;
        boolean isStatic = result.hasTag("static");

        this.sectionNode = sectionNode;
        this.signature = new MethodSignature(methodName, args, accessType, isStatic, returnType, returnPlural);
        return true;
    }

    public void registerMethod() {
        skriptMethod = new SkriptMethod(signature);

        boolean registered = contextClass.methodRegistry.registerMethod(skriptMethod);
        if (!registered) {
            Skript.error("Method with this signature already exists in class: "+ contextClass.name);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadTrigger() {
        Trigger trigger;

        if (signature.returnType() != null) {
            trigger = loadReturnableSectionCode(sectionNode, "method body", new Class[]{MethodRunEvent.class});
        } else {
            trigger = loadCode(sectionNode, "method body", MethodRunEvent.class);
        }
        skriptMethod.setTrigger(trigger);
    }

    @Override
    public @Nullable TriggerItem walk(Event event) {
        throw new IllegalStateException();
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
