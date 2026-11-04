package io.annovibe.llm.json;

import java.util.*;

/**
 * Lightweight, zero-dependency JSON parser and serializer.
 * Designed to eliminate third-party dependency conflicts in annotation processor environments.
 */
public final class SimpleJson {

    private SimpleJson() {}

    public static String stringify(Object obj) {
        StringBuilder sb = new StringBuilder();
        serialize(obj, sb);
        return sb.toString();
    }

    private static void serialize(Object obj, StringBuilder sb) {
        if (obj == null) {
            sb.append("null");
        } else if (obj instanceof String s) {
            sb.append('"');
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\b' -> sb.append("\\b");
                    case '\f' -> sb.append("\\f");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c < 32) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                    }
                }
            }
            sb.append('"');
        } else if (obj instanceof Number || obj instanceof Boolean) {
            sb.append(obj);
        } else if (obj instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                serialize(String.valueOf(entry.getKey()), sb);
                sb.append(':');
                serialize(entry.getValue(), sb);
            }
            sb.append('}');
        } else if (obj instanceof List<?> list) {
            sb.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(',');
                first = false;
                serialize(item, sb);
            }
            sb.append(']');
        } else {
            serialize(obj.toString(), sb);
        }
    }

    public static Object parse(String json) {
        if (json == null) return null;
        Parser parser = new Parser(json);
        return parser.parseValue();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object val = parse(json);
        if (val instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("Expected JSON object, got: " + (val == null ? "null" : val.getClass()));
    }

    private static class Parser {
        private final String src;
        private int pos = 0;

        Parser(String src) {
            this.src = src;
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            skipWhitespace();
            if (pos >= src.length()) return '\0';
            return src.charAt(pos);
        }

        private char next() {
            skipWhitespace();
            if (pos >= src.length()) throw new IllegalArgumentException("Unexpected end of JSON at " + pos);
            return src.charAt(pos++);
        }

        Object parseValue() {
            char c = peek();
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if (Character.isDigit(c) || c == '-') return parseNumber();
            throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + pos);
        }

        private Map<String, Object> parseObject() {
            next(); // consume '{'
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                next();
                return map;
            }
            while (true) {
                String key = parseString();
                skipWhitespace();
                char colon = next();
                if (colon != ':') throw new IllegalArgumentException("Expected ':' at " + pos);
                Object val = parseValue();
                map.put(key, val);
                skipWhitespace();
                char nextChar = next();
                if (nextChar == '}') break;
                if (nextChar != ',') throw new IllegalArgumentException("Expected ',' or '}' at " + pos);
            }
            return map;
        }

        private List<Object> parseArray() {
            next(); // consume '['
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                next();
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                char nextChar = next();
                if (nextChar == ']') break;
                if (nextChar != ',') throw new IllegalArgumentException("Expected ',' or ']' at " + pos);
            }
            return list;
        }

        private String parseString() {
            skipWhitespace();
            if (pos >= src.length() || src.charAt(pos) != '"') {
                throw new IllegalArgumentException("Expected '\"' at " + pos);
            }
            pos++; // consume opening quote
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (pos >= src.length()) throw new IllegalArgumentException("Unterminated escape sequence");
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 > src.length()) throw new IllegalArgumentException("Invalid unicode escape");
                            String hex = src.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                        }
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("Unterminated string starting before " + pos);
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            } else if (src.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Expected boolean at " + pos);
        }

        private Object parseNull() {
            if (src.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("Expected null at " + pos);
        }

        private Number parseNumber() {
            skipWhitespace();
            int start = pos;
            if (src.charAt(pos) == '-') pos++;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.' || src.charAt(pos) == 'e' || src.charAt(pos) == 'E' || src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                pos++;
            }
            String numStr = src.substring(start, pos);
            if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                return Double.parseDouble(numStr);
            }
            try {
                return Long.parseLong(numStr);
            } catch (NumberFormatException e) {
                return Double.parseDouble(numStr);
            }
        }
    }
}
