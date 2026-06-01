package com.novystxr.classysk.api.classes;

import ch.njol.skript.lang.parser.ParserInstance;

public class ParserClassData extends ParserInstance.Data {

    public ParserClassData(ParserInstance parserInstance) {
        super(parserInstance);
    }

    public AbstractSkriptClass skriptClass;
}
