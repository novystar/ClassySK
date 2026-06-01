package com.novystxr.classysk.api.classes;

import ch.njol.skript.ScriptLoader.ScriptLoadEvent;
import ch.njol.skript.lang.parser.ParserInstance;
import com.novystxr.classysk.api.util.Logger;
import org.skriptlang.skript.lang.script.Script;

import java.util.Collection;

public class CleanUpListener implements ScriptLoadEvent {

    @Override
    public void onLoad(ParserInstance parser, Script script) {

        Collection<AbstractSkriptClass> classes = ClassManager.getClasses();

        for (AbstractSkriptClass skriptClass : classes) {
            if (skriptClass.getValidScript() == script && !skriptClass.accessible) {
                ClassManager.removeClass(skriptClass.name);
                Logger.log("Cleaned up class<AQUA>", skriptClass.name);
            }
        }

    }
}
