package com.novystxr.classysk;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import com.novystxr.classysk.api.CleanUpListener;
import com.novystxr.classysk.main.MainModule;
import org.bukkit.plugin.java.JavaPlugin;
import org.skriptlang.skript.addon.SkriptAddon;

public final class Classysk extends JavaPlugin {

    @Override
    public void onEnable() {
        SkriptAddon addon = Skript.instance().registerAddon(Classysk.class, "classySk");
        ScriptLoader.eventRegistry().register(new CleanUpListener());
        addon.loadModules(new MainModule());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
