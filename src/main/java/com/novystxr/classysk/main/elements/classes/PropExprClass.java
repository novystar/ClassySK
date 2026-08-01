package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.doc.*;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.ClassReference;
import org.jspecify.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Class of instance")
@Keywords({"get class", "from instance"})
@Description("Gets the class reference for the given instance, useful for comparisons")
@Example("if class of {_instance} is class of {_otherInstance}")
@Since("1.0.0")
public class PropExprClass extends SimplePropertyExpression<ClassInstance, ClassReference> {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EXPRESSION,
            infoBuilder(PropExprClass.class, ClassReference.class,
                "([skript] class|class reference)", "classinstance", true)
                .supplier(PropExprClass::new)
                .build()
        );
    }

    @Override
    public @Nullable ClassReference convert(ClassInstance instance) {
        return ClassReference.of(instance.getParent());
    }

    @Override
    protected String getPropertyName() {
        return "wrapper class";
    }

    @Override
    public Class<? extends ClassReference> getReturnType() {
        return ClassReference.class;
    }
}
