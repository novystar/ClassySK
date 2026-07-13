package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.doc.*;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.SkriptClassWrapper;
import org.jspecify.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Class of instance")
@Keywords({"get class", "from instance"})
@Description("Gets the wrapper class for the given instance, useful for comparisons")
@Example("if class of {_instance} is class of {_otherInstance}")
@Since("1.0.0")
public class PropExprClass extends SimplePropertyExpression<ClassInstance, SkriptClassWrapper> {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EXPRESSION,
            infoBuilder(PropExprClass.class, SkriptClassWrapper.class,
                "[skript|wrapper] class", "classinstance", true)
                .supplier(PropExprClass::new)
                .priority(SyntaxInfo.COMBINED)
                .build()
        );
    }

    @Override
    public @Nullable SkriptClassWrapper convert(ClassInstance instance) {
        return SkriptClassWrapper.of(instance.getParent());
    }

    @Override
    protected String getPropertyName() {
        return "wrapper class";
    }

    @Override
    public Class<? extends SkriptClassWrapper> getReturnType() {
        return SkriptClassWrapper.class;
    }
}
