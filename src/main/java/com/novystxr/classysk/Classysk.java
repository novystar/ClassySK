package com.novystxr.classysk;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.variables.Variables;
import com.novystxr.classysk.api.classes.CleanUpListener;
import com.novystxr.classysk.api.fields.SerializableField;
import com.novystxr.classysk.main.MainModule;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.skriptlang.skript.addon.SkriptAddon;

public class Classysk extends JavaPlugin {

    public static final String NAME_PATTERN = "[\\w_]+";
    public static final String CLASSNAME_PATTERN = "[A-Z]\\w*";

    @Override
    public void onEnable() {
        Variables.yggdrasil.registerSingleClass(SerializableField.class, "SerializableField");

        SkriptAddon addon = Skript.instance().registerAddon(Classysk.class, "ClassySK");
        ScriptLoader.eventRegistry().register(new CleanUpListener());
        addon.loadModules(new MainModule());

        int pluginId = 31871;
        new Metrics(this, pluginId);
    }
}
