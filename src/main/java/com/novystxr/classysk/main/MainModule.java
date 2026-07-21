package com.novystxr.classysk.main;

import ch.njol.skript.Skript;
import ch.njol.skript.variables.Variables;
import com.novystxr.classysk.api.util.Logger;
import com.novystxr.classysk.main.elements.*;
import com.novystxr.classysk.main.elements.classes.*;
import com.novystxr.classysk.main.elements.fields.EffField;
import com.novystxr.classysk.main.elements.fields.ExprFieldAccess;
import com.novystxr.classysk.main.elements.methods.EffMethodCall;
import com.novystxr.classysk.main.elements.methods.ExprMethodCall;
import com.novystxr.classysk.main.elements.methods.SecMethod;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.SkriptAddon;

public class MainModule implements AddonModule {

    // Should not be called in release builds
    public void onlyForDocs(SkriptAddon addon) {
        register(addon,
            SecMethod::register,
            EffField::register
        );

        if (Skript.testing()) {
            Variables.setVariable("-failedTest", true, null, false);
        }
        Logger.severe("Addon registered syntax that should not be registered in release builds");
    }

    @Override
    public void load(SkriptAddon addon) {
        Types.register(addon);

        register(addon,
            StructClass::register,
            SecExprNewInstance::register,
            ExprFieldAccess::register,
            ExprSelf::register,
            EffMethodCall::register,
            ExprMethodCall::register,
            CondInstanceOf::register,
            PropExprClass::register,
            ExprClass::register
            );

        onlyForDocs(addon);
    }

    @Override
    public String name() {
        return "Main";
    }

}
