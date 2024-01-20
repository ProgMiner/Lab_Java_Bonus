package ru.byprogminer.servertester.config.loader;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Parsers {

    private static final Pattern INTEGER_PATTERN = Pattern.compile("^[+-]?\\d+");

    private Parsers() {
        throw new UnsupportedOperationException("not instantiable");
    }

    public static String string(String pattern, String str) {
        if (str.startsWith(pattern)) {
            return str.substring(pattern.length());
        }

        throw new IllegalArgumentException("expected: " + pattern + ", got: " + str);
    }

    public static ParseResult<String> regexp(Pattern pattern, String str) {
        final Matcher m = pattern.matcher(str);

        if (!m.find() || m.start() != 0) {
            throw new IllegalArgumentException("expected pattern: " + pattern + ", got: " + str);
        }

        final String result = m.group();
        return new ParseResult<>(result, str.substring(result.length()));
    }

    public static ParseResult<Long> integer(String str) {
        final ParseResult<String> result = regexp(INTEGER_PATTERN, str);

        return new ParseResult<>(Long.parseLong(result.value), result.rest);
    }

    public static ParseResult<Duration> duration(String str) {
        final ParseResult<Long> first = integer(str);
        str = first.rest;

        final ParseResult<List<Long>> rest = many(s -> integer(string(":", s)), str);
        str = rest.rest;

        final List<Long> components = new ArrayList<>();
        components.add(first.value);
        components.addAll(rest.value);

        final ParseResult<Long> ms = optional(s -> integer(string(".", s)), str).map(v -> v == null ? 0 : v);
        str = ms.rest;

        final long seconds;
        switch (components.size()) {
            case 1:
                seconds = components.get(0);
                break;

            case 2:
                seconds = components.get(0) * 60 + components.get(1);
                break;

            case 3:
                seconds = (components.get(0) * 60 + components.get(1)) * 60 + components.get(2);
                break;

            default:
                throw new IllegalArgumentException("unsupported amount of duration components: " + components.size());
        }

        return new ParseResult<>(Duration.ofSeconds(seconds, ms.value * 1000_000), str);
    }

    public static <T> ParseResult<T> optional(Function<String, ParseResult<T>> parser, String str) {
        try {
            return parser.apply(str);
        } catch (Exception e) {
            return new ParseResult<>(null, str);
        }
    }

    public static <T> ParseResult<List<T>> many(Function<String, ParseResult<T>> parser, String str) {
        final List<T> result = new ArrayList<>();

        while (true) {
            try {
                final ParseResult<T> current = parser.apply(str);

                result.add(current.value);
                str = current.rest;
            } catch (Exception e) {
                break;
            }
        }

        return new ParseResult<>(result, str);
    }

    public static void finish(String rest) {
        if (!rest.trim().isEmpty()) {
            throw new IllegalArgumentException("unexpected characters on tail: " + rest);
        }
    }
}
