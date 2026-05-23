package com.novystxr.classysk.api;

import ch.njol.skript.ScriptLoader.ScriptLoadEvent;
import ch.njol.skript.lang.parser.ParserInstance;
import com.novystxr.classysk.api.util.Logger;
import org.skriptlang.skript.lang.script.Script;

import java.util.List;

public class CleanUpListener implements ScriptLoadEvent {

    @Override
    public void onLoad(ParserInstance parser, Script script) {

        List<AbstractSkriptClass> inaccessibleClasses = ClassManager.getInaccessibleScriptClasses(script);
        for (AbstractSkriptClass skriptClass : inaccessibleClasses) {
            ClassManager.removeClass(skriptClass.name);
            Logger.log("Cleaned up class:", skriptClass.name);
        }

    }
}
