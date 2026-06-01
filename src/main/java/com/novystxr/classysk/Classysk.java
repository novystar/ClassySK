package com.novystxr.classysk;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.parser.ParserInstance;
import com.novystxr.classysk.api.classes.CleanUpListener;
import com.novystxr.classysk.api.classes.ParserClassData;
import com.novystxr.classysk.main.MainModule;
import org.bukkit.plugin.java.JavaPlugin;
import org.skriptlang.skript.addon.SkriptAddon;

public final class Classysk extends JavaPlugin {

    @Override
    public void onEnable() {
        SkriptAddon addon = Skript.instance().registerAddon(Classysk.class, "ClassySK");
        ParserInstance.registerData(ParserClassData.class, ParserClassData::new);
        ScriptLoader.eventRegistry().register(new CleanUpListener());
        addon.loadModules(new MainModule());
    }
}
