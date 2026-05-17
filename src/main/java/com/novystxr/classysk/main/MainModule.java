package com.novystxr.classysk.main;

import com.novystxr.classysk.main.elements.*;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.SkriptAddon;

public class MainModule implements AddonModule {

    @Override
    public void load(SkriptAddon addon) {
        Types.register();

        register(addon,
                StructClass::register,
                EffField::register,
                ExprAbstractClass::register,
                ExprNewClassInstance::register,
                ExprFieldAccess::register,
                ExprThisInstance::register,
                SecMethod::register,
                EffMethodCall::register
                );

    }

    @Override
    public String name() {
        return "Main";
    }

}
