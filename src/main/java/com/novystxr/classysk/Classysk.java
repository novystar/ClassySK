package com.novystxr.classysk;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import com.novystxr.classysk.api.classes.CleanUpListener;
import com.novystxr.classysk.main.MainModule;
import org.bukkit.plugin.java.JavaPlugin;
import org.skriptlang.skript.addon.SkriptAddon;

public class Classysk extends JavaPlugin {

    public static final String namePattern = "[\\w_]+";

    @Override
    public void onEnable() {
        SkriptAddon addon = Skript.instance().registerAddon(Classysk.class, "ClassySK");
        ScriptLoader.eventRegistry().register(new CleanUpListener());
        addon.loadModules(new MainModule());
    }
}
