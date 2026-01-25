package org.example.demo2.service;

/**
 * Service for computing signal statistics.
 */
public class StatisticsService {

    public static class Stats {
        private final double min;
        private final double max;
        private final double p2p;
        private final double rms;

        public Stats(double min, double max, double p2p, double rms) {
            this.min = min;
            this.max = max;
            this.p2p = p2p;
            this.rms = rms;
        }

        public String getMin() {
            return String.format("%.4f", min);
        }

        public String getMax() {
            return String.format("%.4f", max);
        }

        public String getP2p() {
            return String.format("%.4f", p2p);
        }

        public String getRms() {
            return String.format("%.4f", rms);
        }
    }

    public Stats compute(double[] v) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sumSq = 0;

        for (double x : v) {
            min = Math.min(min, x);
            max = Math.max(max, x);
            sumSq += x * x;
        }

        double rms = Math.sqrt(sumSq / v.length);
        return new Stats(min, max, max - min, rms);
    }
}
