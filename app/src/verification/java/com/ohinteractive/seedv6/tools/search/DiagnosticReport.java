package com.ohinteractive.seedv6.tools.search;

import java.io.PrintWriter;
import java.util.*;

/** Tool-only JSON Lines and population statistics; never used in a search hot path. */
final class DiagnosticReport {
    record Sample(String id, double value) {
        Sample {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Nonfinite sample: " + id);
        }
    }

    static Map<String, Object> fields(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    static Map<String, Object> statistics(List<Sample> samples) {
        if (samples.isEmpty()) return fields("count", 0);
        List<Sample> sorted = new ArrayList<>(samples);
        sorted.sort(Comparator.comparingDouble(Sample::value).thenComparing(Sample::id));
        // Scale first so finite extreme preactivations do not overflow variance or mean.
        double scale = sorted.stream().mapToDouble(s -> Math.abs(s.value())).max().orElseThrow();
        double mean = scale == 0 ? 0 : sorted.stream().mapToDouble(s -> s.value() / scale).average().orElseThrow();
        double variance = scale == 0 ? 0 : sorted.stream().mapToDouble(s -> {
            double difference = s.value() / scale - mean;
            return difference * difference;
        }).average().orElseThrow();
        var result = fields("count", samples.size(), "min", sorted.getFirst().value(),
                "max", sorted.getLast().value(), "mean", mean * scale,
                "populationStdDev", Math.sqrt(variance) * scale);
        for (int percentile : new int[]{1, 5, 25, 50, 75, 95, 99})
            result.put("p" + percentile, quantile(sorted, percentile / 100.0));
        result.put("lowest", extremes(sorted.subList(0, Math.min(10, sorted.size()))));
        result.put("highest", extremes(sorted.reversed().subList(0, Math.min(10, sorted.size()))));
        return result;
    }

    private static List<Map<String, Object>> extremes(List<Sample> samples) {
        return samples.stream().map(s -> fields("id", s.id(), "value", s.value())).toList();
    }

    /** Linear interpolation at index (n-1)*p, including endpoints. */
    static double quantile(List<Sample> sorted, double p) {
        double index = (sorted.size() - 1) * p;
        int low = (int) index, high = Math.min(low + 1, sorted.size() - 1);
        double fraction = index - low;
        return sorted.get(low).value() * (1 - fraction) + sorted.get(high).value() * fraction;
    }

    static void write(PrintWriter out, Object... pairs) {
        out.println(json(fields(pairs)));
        out.flush();
        if (out.checkError()) throw new IllegalStateException("Diagnostic output write failed.");
    }

    static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Number number) {
            if (!Double.isFinite(number.doubleValue())) throw new IllegalArgumentException("Nonfinite JSON number.");
            return number.toString();
        }
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringJoiner join = new StringJoiner(",", "{", "}");
            map.forEach((k, v) -> join.add(json(k.toString()) + ":" + json(v)));
            return join.toString();
        }
        if (value instanceof Iterable<?> list) {
            StringJoiner join = new StringJoiner(",", "[", "]");
            list.forEach(item -> join.add(json(item)));
            return join.toString();
        }
        StringBuilder text = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            if (c == '"' || c == '\\') text.append('\\').append(c);
            else if (c < 32) text.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
            else text.append(c);
        }
        return text.append('"').toString();
    }

    private DiagnosticReport() {}
}
