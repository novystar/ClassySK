package com.novystxr.classysk.api.util;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.ContextlessEvent;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public abstract class DefaultValue<T> extends SimpleExpression<T> {

    public abstract boolean parse();
    public abstract boolean parse(Node node);

    protected abstract Expression<? extends T> getExpr();

    public T[] getArray() {
        return getArray(ContextlessEvent.get());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean delayed, ParseResult result) {
        throw new IllegalStateException();
    }

    @Override
    protected T @Nullable [] get(Event event) {
        Expression<? extends T> expr = getExpr();
        return expr == null ? null : expr.getArray(event);
    }

    @Override
    public Class<? extends T> getReturnType() {
        Expression<? extends T> expr = getExpr();
        return expr == null ? null : expr.getReturnType();
    }

    @Override
    public boolean isSingle() {
        Expression<? extends T> expr = getExpr();
        return expr == null || expr.isSingle();
    }

    @Override
    public String toString(Event event, boolean debug) {
        Expression<? extends T> expr = getExpr();
        return expr == null ? "unparsed default" : toString(event, debug);
    }

    public static class Simple<T> extends DefaultValue<T> {
        public Literal<T> literal;

        public Simple(T[] value, Class<T> type) {
            this.literal = new SimpleLiteral<>(value, type, false);
        }

        @Override
        public boolean parse() {
            return true;
        }

        @Override
        public boolean parse(Node node) {
            return true;
        }

        @Override
        protected Expression<? extends T> getExpr() {
            return literal;
        }
    }

    public static class Dynamic<T> extends DefaultValue<T> {

        public final Class<T> parseAs;
        public final String rawExpr;
        public final boolean isPlural;

        public Expression<? extends T> parsedExpr;

        public Dynamic(String rawExpr, Class<T> parseAs, boolean isPlural) {
            this.rawExpr = rawExpr;
            this.parseAs = parseAs;
            this.isPlural = isPlural;
        }

        @Override
        public boolean parse() {
            SkriptParser parser = new SkriptParser(rawExpr, SkriptParser.ALL_FLAGS, ParseContext.DEFAULT);
            parsedExpr = LiteralUtils.defendExpression(parser.parseExpression(parseAs));

            if (parsedExpr == null || !LiteralUtils.canInitSafely(parsedExpr)) {
                Skript.error("Can't understand this expression: " + rawExpr);
                return false;
            }
            return checkPlural();
        }

        @Override
        public boolean parse(Node node) {
            parsedExpr = ParserUtils.parseExprNode(rawExpr, node, parseAs);
            return parsedExpr != null
                && checkPlural();
        }

        private boolean checkPlural() {
            if (!parsedExpr.isSingle() && !isPlural) {
                Skript.error("This expression cannot be plural");
                return false;
            }
            return true;
        }

        @Override
        protected Expression<? extends T> getExpr() {
            return parsedExpr;
        }
    }
}
