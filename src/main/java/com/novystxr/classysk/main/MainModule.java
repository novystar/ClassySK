package com.novystxr.classysk.main;

import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.main.elements.*;
import com.novystxr.classysk.main.elements.classes.*;
import com.novystxr.classysk.main.elements.fields.*;
import com.novystxr.classysk.main.elements.methods.*;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.SkriptAddon;

public class MainModule implements AddonModule {

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

        if (Classysk.TYPES_ALLOWED)
            register(addon, ExprTypedInstance::register);

        //register(addon, SecMethod::register, EffField::register); // docs only
    }

    @Override
    public String name() {
        return "Main";
    }

}
