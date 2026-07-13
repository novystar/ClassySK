package com.novystxr.classysk.main.elements.fields;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.util.ClassInfoReference;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.AccessModifiable.AccessType;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.api.util.ExprUtils;
import com.novystxr.classysk.main.elements.classes.StructClass;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import static com.novystxr.classysk.api.AccessModifiable.AccessType.PRIVATE;
import static com.novystxr.classysk.api.AccessModifiable.AccessType.PUBLIC;

@Name("Field")
@Description({
    "Fields hold data of a specified type relating to a class. They can have default values",
    "See the [**Official Wiki**](https://github.com/novystar/ClassySK/wiki/Tutorials%3A-Classes-%26-Fields) for more information."
})
@Since("1.0.0")
public class EffField extends Effect {
    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EFFECT,
            SyntaxInfo.builder(EffField.class)
                .addPattern("(public|:private) [:static] <"+ Classysk.NAME_PATTERN +">\\: %*classinfo% [= %-objects%]")
                .supplier(EffField::new)
                .build()
        );
    }

    String fieldName;
    private FieldSignature signature;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {

        ParserInstance parserInstance = getParser();
        if (!parserInstance.isActive()) return false;

        if (!(parserInstance.getCurrentStructure() instanceof StructClass)) {
            Skript.error("Field definition can only be used within a class structure.");
            return false;
        }

        fieldName = StringUtils.getLowerCase(result.regexes.getFirst());

        AccessType accessType = result.hasTag("private") ? PRIVATE : PUBLIC;
        boolean isStatic = result.hasTag("static");

        ClassInfoReference reference = ExprUtils.getClassRef(exprs[0]);
        boolean isPlural = reference.isPlural().isTrue();
        Class<?> type = reference.getClassInfo().getC();

        Expression<?> defaultExpr = null;

        if (exprs[1] != null) {
            defaultExpr = exprs[1].getConvertedExpression(type);
            if (defaultExpr == null) {
                Skript.error("Default value can't convert to type: "+reference.getClassInfo());
                return false;
            }

            if (!defaultExpr.isSingle() && !isPlural) {
                Skript.error("Default value is plural but field only accept single values");
                return false;
            }
        }
        signature = new FieldSignature(fieldName, type, defaultExpr, accessType, isStatic, isPlural);
        return true;
    }

    public void registerField(SkriptClass skriptClass) {
        var result = skriptClass.fieldSignatures.putIfAbsent(fieldName, signature);
        if (result != null) {
            Skript.error("Field named '"+fieldName+"' already exists in this class");
        }
    }

    @Override
    protected void execute(Event event) {
        throw new IllegalStateException();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "field declaration";
    }
}
