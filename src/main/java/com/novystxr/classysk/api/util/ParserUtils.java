package com.novystxr.classysk.api.util;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.Section.SectionContext;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.LiteralUtils;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

public class ParserUtils {

    public static @Nullable Expression<?> parseExprNode(String rawExpr, Node node, Class<?>... types) {
        SectionContext context = ParserInstance.get().getData(SectionContext.class);

        SectionNode secNode = (node instanceof SectionNode) ? (SectionNode) node : null;
        SkriptParser parser = new SkriptParser(rawExpr, SkriptParser.ALL_FLAGS, ParseContext.DEFAULT);

        Expression<?> expr = context.modify(secNode, null, () -> (Expression<?>) parser.parseExpression(types));
        expr = LiteralUtils.defendExpression(expr);

        if (!(expr instanceof SectionExpression<?>) && secNode != null) {
            Skript.error("This expression is not usable as a section");
            return null;
        }
        return LiteralUtils.canInitSafely(expr) ? expr : null;
    }

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
            .filter(info -> Arrays.stream(filterAs)
                .anyMatch(cls -> cls.isAssignableFrom(info.type())) || filterAs.length == 0)
            .iterator();
    }
 }
