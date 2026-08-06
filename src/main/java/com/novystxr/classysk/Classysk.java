package com.novystxr.classysk;

import ch.njol.skript.Skript;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.Variables;
import com.novystxr.classysk.api.classes.TypedClassAdvice;
import com.novystxr.classysk.api.fields.SerializableField;
import com.novystxr.classysk.main.MainModule;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.util.Priority;

public class Classysk extends JavaPlugin {

    public static final String NAME_PATTERN = "[\\w_]+";
    public static final String CLASSNAME_PATTERN = "[A-Z]\\w*";

    public static final Priority SHADOW_REALM = Priority.after(SyntaxInfo.PATTERN_MATCHES_EVERYTHING);

    @Override
    public void onEnable() {
        ByteBuddyAgent.install();
        new ByteBuddy()
            .redefine(Classes.class)
            .visit(Advice.to(TypedClassAdvice.class).on(ElementMatchers.named("getClassInfoFromUserInput")))
            .make()
            .load(Classes.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

        Variables.yggdrasil.registerSingleClass(SerializableField.class, "SerializableField");

        SkriptAddon addon = Skript.instance().registerAddon(Classysk.class, "ClassySK");
        addon.loadModules(new MainModule());

        int pluginId = 31871;
        new Metrics(this, pluginId);
    }
}
