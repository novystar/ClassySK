package com.novystxr.classysk.main;

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

    @Override
    public void load(SkriptAddon addon) {
        Types.register(addon);

        register(addon,
            StructClass::register,
            EffField::register,
            SecExprNewInstance::register,
            ExprFieldAccess::register,
            ExprSelf::register,
            SecMethod::register,
            EffMethodCall::register,
            ExprMethodCall::register,
            CondInstanceOf::register,
            PropExprClass::register,
            ExprClass::register
            );

    }

    @Override
    public String name() {
        return "Main";
    }

}
