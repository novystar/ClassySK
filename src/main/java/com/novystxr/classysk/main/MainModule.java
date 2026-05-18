package com.novystxr.classysk.main;

import com.novystxr.classysk.main.elements.*;
import com.novystxr.classysk.main.elements.classes.ExprAbstractClass;
import com.novystxr.classysk.main.elements.classes.ExprNewClassInstance;
import com.novystxr.classysk.main.elements.classes.ExprThisInstance;
import com.novystxr.classysk.main.elements.classes.StructClass;
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
        Types.register();

        register(addon,
                StructClass::register,
                EffField::register,
                ExprAbstractClass::register,
                ExprNewClassInstance::register,
                ExprFieldAccess::register,
                ExprThisInstance::register,
                SecMethod::register,
                EffMethodCall::register,
                ExprMethodCall::register
                );

    }

    @Override
    public String name() {
        return "Main";
    }

}
