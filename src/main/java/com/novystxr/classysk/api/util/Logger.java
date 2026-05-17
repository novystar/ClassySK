package com.novystxr.classysk.api.util;

import com.novystxr.classysk.Classysk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Logger {

    private static final ConsoleCommandSender console;
    private static final java.util.logging.Logger logger;

    private static final MiniMessage mm;
    private static final String prefix = "<GRAY>[<#83A4FF>ClassySK<GRAY>] <WHITE>";

    private static Component buildMessage(Object... objects) {
        StringBuilder builder = new StringBuilder();

        builder.append(prefix);

        for (Object object : objects) {
            builder.append(object);
            builder.append(" ");
        }

        return mm.deserialize(builder.toString());
    }

    public static void log(Object... values) {
        console.sendMessage((buildMessage(values)));
    }

    public static void info(String msg) {
        logger.info(msg);
    }

    public static void severe(String msg) {
        logger.severe(msg);
    }

    public static void warning(String msg) {
        logger.warning(msg);
    }


    static {
        JavaPlugin plugin = Classysk.getPlugin(Classysk.class);

        console = Bukkit.getServer().getConsoleSender();
        logger = plugin.getLogger();

        mm = MiniMessage.miniMessage();
    }
}
