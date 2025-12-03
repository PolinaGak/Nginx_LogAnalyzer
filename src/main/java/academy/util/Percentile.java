package academy.util;

import java.util.List;

public class Percentile {

    /** Возвращает 95-й перцентиль из списка значений. Точность — 2 знака после запятой. */
    public static double p95(List<Long> values) {
        if (values.isEmpty()) return 0.0;
        if (values.size() == 1) return values.get(0);

        List<Long> sorted = values.stream().sorted().toList();
        double index = 0.95 * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return round(sorted.get(lower));
        }
        double fraction = index - lower;
        double result = sorted.get(lower) * (1 - fraction) + sorted.get(upper) * fraction;
        return round(result);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
