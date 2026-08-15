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
import java.util.List;
import java.util.function.Supplier;

public class ParserUtils {

    /**
     *
     * Parses expressions and section expressions.
     * This method properly modifies the section context for the given node if necessary.
     * This also converts unparsed literals so the resulting expression is safe to use.
     *
     * @param rawExpr Raw string to parse
     * @param node The node belonging to this syntax
     * @param types Types to convert to
     *
     * @return The resulting expression - null if it failed to parse or could not be converted
     *
     * @see SkriptParser#parseExpression(Class[])
     * @see SectionContext#modify(SectionNode, List, Supplier)
     */
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

    /**
     *
     * Parses a node as any of the given syntax infos.
     * Takes into account section context and filters the target syntaxes depending on node type.
     * For example a section node will only parse as infos of {@link Section}.
     * <br> <br>
     * This only supports Statements and Sections where the node's key is parsed in whole.
     * For expression parsing see {@link ParserUtils#parseExprNode(String, Node, Class[])}.
     *
     * @param node The target node, can be a SectionNode
     * @param defaultError The default error to print if the syntax didn't match
     * @param infos The target syntax infos
     * @return The parsed syntax element - null if failed to parse
     */
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

    /**
     *
     * Helper method to get an iterator of syntax infos that can be used with skript's parser methods
     *
     * @param infos The syntax infos to iterate
     * @param filterAs Syntax types which should be included, if empty nothing will be filtered
     *
     * @see SkriptParser#parse(String, Iterator, String)
     */
    @SuppressWarnings("unchecked")
    public static <T extends SyntaxElement> Iterator<SyntaxInfo<T>> infoIterator(SyntaxInfo<?>[] infos, Class<?>... filterAs) {
        return (Iterator<SyntaxInfo<T>>) (Iterator<?>) Arrays.stream(infos)
            .filter(info -> Arrays.stream(filterAs)
                .anyMatch(cls -> cls.isAssignableFrom(info.type())) || filterAs.length == 0)
            .iterator();
    }
 }
