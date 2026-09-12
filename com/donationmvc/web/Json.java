package com.donationmvc.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {}

    public static String encode(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return encodeString((String) value);
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 9.0e15) {
                long l = ((Number) value).longValue();
                String out = String.valueOf(l);
                if (d == (double) l) return out;
            }
            return String.valueOf(d);
        }
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(encodeString(String.valueOf(e.getKey()))).append(':').append(encode(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (value instanceof Iterable) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object o : (Iterable<?>) value) {
                if (!first) sb.append(',');
                first = false;
                sb.append(encode(o));
            }
            return sb.append(']').toString();
        }
        return encodeString(String.valueOf(value));
    }

    private static String encodeString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
                        if (i + 1 < s.length() && Character.isSurrogatePair(c, s.charAt(i + 1))) {
                            sb.append(c).append(s.charAt(i + 1));
                            i++;
                        } else {
                            sb.append('?');
                        }
                    } else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    public static Map<String, Object> obj() { return new LinkedHashMap<>(); }

    public static List<Object> arr() { return new ArrayList<>(); }

    public static Object parse(String json) {
        Parser p = new Parser(json);
        Object v = p.parseValue();
        p.skipWs();
        if (!p.done()) throw new IllegalArgumentException("Trailing characters in JSON");
        return v;
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String json) { this.s = json; }

        boolean done() { return i >= s.length(); }

        void skipWs() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') i++;
                else break;
            }
        }

        char peek() {
            skipWs();
            if (done()) throw new IllegalArgumentException("Unexpected end of JSON");
            return s.charAt(i);
        }

        Object parseValue() {
            char c = peek();
            switch (c) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return parseString();
                case 't': expect("true"); return Boolean.TRUE;
                case 'f': expect("false"); return Boolean.FALSE;
                case 'n': expect("null"); return null;
                default: return parseNumber();
            }
        }

        void expect(String token) {
            skipWs();
            if (!s.regionMatches(i, token, 0, token.length()))
                throw new IllegalArgumentException("Invalid token at " + i);
            i += token.length();
        }

        Map<String, Object> parseObject() {
            skipWs();
            i++;
            Map<String, Object> map = new LinkedHashMap<>();
            skipWs();
            if (!done() && s.charAt(i) == '}') { i++; return map; }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                if (done() || s.charAt(i) != ':') throw new IllegalArgumentException("Expected ':'");
                i++;
                Object val = parseValue();
                map.put(key, val);
                skipWs();
                if (done()) throw new IllegalArgumentException("Unclosed object");
                char c = s.charAt(i);
                i++;
                if (c == '}') return map;
                if (c != ',') throw new IllegalArgumentException("Expected ',' or '}'");
            }
        }

        List<Object> parseArray() {
            skipWs();
            i++;
            List<Object> list = new ArrayList<>();
            skipWs();
            if (!done() && s.charAt(i) == ']') { i++; return list; }
            while (true) {
                list.add(parseValue());
                skipWs();
                if (done()) throw new IllegalArgumentException("Unclosed array");
                char c = s.charAt(i);
                i++;
                if (c == ']') return list;
                if (c != ',') throw new IllegalArgumentException("Expected ',' or ']'");
            }
        }

        String parseString() {
            skipWs();
            if (done() || s.charAt(i) != '"') throw new IllegalArgumentException("Expected string");
            i++;
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '"') { i++; return sb.toString(); }
                if (c == '\\') {
                    i++;
                    if (i >= s.length()) break;
                    char e = s.charAt(i);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (i + 4 < s.length()) {
                                try {
                                    sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                                    i += 4;
                                } catch (NumberFormatException ex) {
                                    throw new IllegalArgumentException("Bad unicode escape");
                                }
                            }
                            break;
                        default: sb.append(e);
                    }
                    i++;
                } else {
                    sb.append(c);
                    i++;
                }
            }
            throw new IllegalArgumentException("Unclosed string");
        }

        Number parseNumber() {
            skipWs();
            int start = i;
            if (!done() && s.charAt(i) == '-') i++;
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.' || s.charAt(i) == 'e' || s.charAt(i) == 'E' || s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
            String num = s.substring(start, i);
            if (num.indexOf('.') >= 0 || num.indexOf('e') >= 0 || num.indexOf('E') >= 0) {
                return Double.parseDouble(num);
            }
            try {
                return Long.parseLong(num);
            } catch (NumberFormatException e) {
                return Double.parseDouble(num);
            }
        }
    }
}