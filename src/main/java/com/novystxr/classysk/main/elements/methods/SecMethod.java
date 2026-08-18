package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.ClassInfoReference;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.methods.MethodParser;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodSignature;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.event.MethodRunEvent;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.api.util.ExprUtils;
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
@Since("1.0.0")
public class SecMethod extends EffectSection implements ReturnHandler<Object> {

    public static void register(SyntaxRegistry registry) {
        //noinspection ThrowableInstanceNeverThrown
        Skript.exception(new IllegalStateException("SecMethod should not be registered"));

        registry.register(SyntaxRegistry.SECTION, INFO);
    }

    public static SyntaxInfo<SecMethod> ANONYMOUS_INFO = SyntaxInfo.builder(SecMethod.class)
        .addPattern("[override] <"+ Classysk.NAME_PATTERN +">\\([args:<.+>]\\) [(\\:\\:|returns|->) %-*classinfo%]")
        .supplier(SecMethod::new)
        .build();

    public static SyntaxInfo<SecMethod> INFO = SyntaxInfo.builder(SecMethod.class)
        .addPattern("(:public|:protected|:private) [:final] [:static|:abstract|:override] <"+ Classysk.NAME_PATTERN +">\\([args:<.+>]\\) [(\\:\\:|returns|->) %-*classinfo%]")
        .supplier(SecMethod::new)
        .build();

    private SectionNode sectionNode;
    public MethodSignature signature;

    private SkriptMethod skriptMethod;
    public SkriptClass contextClass;

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        Modifier[] modifiers = Modifier.collect(result.tags);

        if (modifiers[2] == Modifier.FINAL && modifiers[1] != null && modifiers[1] != Modifier.OVERRIDE) {
            Skript.error("Modifier 'final' cannot be combined with '%s'.", modifiers[1].name().toLowerCase(Locale.ENGLISH));
            return false;
        }
        if (modifiers[1] == Modifier.ABSTRACT && sectionNode != null) {
            Skript.error("Abstract methods cannot have a body.");
            return false;
        }
        if (modifiers[1] != Modifier.ABSTRACT && sectionNode == null) {
            Skript.error("This method has no body. If you meant to leave it unimplemented, mark it as 'abstract'.");
            return false;
        }
        if (modifiers[1] == Modifier.ABSTRACT && modifiers[0] == Modifier.PRIVATE) {
            Skript.error("A private method can't be overridden, so it cannot be abstract.");
            return false;
        }
        if (modifiers[0] == Modifier.PRIVATE && modifiers[2] == Modifier.FINAL) {
            Skript.warning("Modifier 'final' is redundant in private methods.");
        }

        boolean returnPlural = false;
        Class<?> returnType = null;

        if (exprs[0] != null) {
            ClassInfoReference reference = ExprUtils.getClassRef(exprs[0]);
            returnPlural = reference.isPlural().isTrue();
            returnType = reference.getClassInfo().getC();
        }
        String methodName = StringUtils.getConfigLowerCase(result.regexes.get(0));
        SequencedMap<String, MethodArgument> args = new LinkedHashMap<>();

        if (result.hasTag("args")) {
            String argsString = result.regexes.get(1).group();
            args = MethodParser.parseArguments(argsString);
            if (args == null) {
                return false;
            }
        }

        this.signature = new MethodSignature(methodName, args, modifiers, returnType, returnPlural);
        this.sectionNode = sectionNode;
        return true;
    }

    public boolean registerMethod(SkriptClass contextClass, SkriptMethod method) {
        this.skriptMethod = method;
        this.contextClass = contextClass;
        return contextClass.methodRegistry.registerMethod(method);
    }

    public boolean registerMethod(SkriptClass contextClass) {
        return registerMethod(contextClass, signature);
    }

    public boolean registerMethod(SkriptClass contextClass, MethodSignature signature) {
        this.contextClass = contextClass;
        skriptMethod = new SkriptMethod(signature, contextClass.name);

        return contextClass.methodRegistry.registerMethod(skriptMethod);
    }

    @SuppressWarnings("unchecked")
    public void loadTrigger() {
        if (sectionNode == null) return;
        Trigger trigger;

        if (signature.type() != null) {
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
        return !signature.isPlural();
    }

    @Override
    public @Nullable Class<?> returnValueType() {
        return signature.type();
        }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "method declaration";
    }
}
