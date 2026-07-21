package com.novystxr.classysk.api.util;

import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.Section.SectionContext;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.SkriptLogger;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;

import java.util.Iterator;
import java.util.stream.Stream;

public class ParserUtils {

    public static <T extends SyntaxElement> @Nullable T parseNodeAsInfos(Node node, @Nullable String defaultError, SyntaxInfo<?>... infos) {
        SkriptLogger.setNode(node);
        String expr = node.getKey();
        if (expr == null) {
            return null;
        }
        if (node instanceof SectionNode secNode) {
            SectionContext sectionContext = ParserInstance.get().getData(SectionContext.class);
            return sectionContext.modify(secNode, null, () -> SkriptParser.parse(expr, infoIterator(infos, Section.class), defaultError));

        } else {
            return SkriptParser.parse(expr, infoIterator(infos, Statement.class, EffectSection.class), defaultError);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends SyntaxElement> Iterator<SyntaxInfo<T>> infoIterator(SyntaxInfo<?>[] infos, Class<?>... filterAs) {
        return (Iterator<SyntaxInfo<T>>) (Iterator<?>) Stream.of(infos)
            .filter(info -> {
                Class<?> type = info.type();
                for (Class<?> filterClass : filterAs) {
                    if (filterClass.isAssignableFrom(type))
                        return true;
                }
                return filterAs.length == 0;
            })
            .iterator();
    }
 }
