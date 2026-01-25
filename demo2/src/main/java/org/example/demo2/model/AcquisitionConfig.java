package org.example.demo2.model;

/**
 * KLASA KONFIGURACJI AKWIZYCJI DANYCH
 * ====================================
 *
 * Przechowuje wszystkie parametry dotyczące procesu zbierania danych z urządzenia pomiarowego.
 * PARAMETRY AKWIZYCJI:
 * ===================
 * - sampleRateHz: częstotliwość próbkowania [Hz] - ile próbek na sekundę zbieramy
 *   Przykład: 10000 Hz = 10000 próbek/s = jedna próbka co 0.1 ms
 *
 * - bufferSize: rozmiar bufora [liczba próbek] - ile próbek przechowujemy w pamięci
 *   Większy bufor = dłuższy czas pomiaru = więcej danych = dokładniejsza analiza
 *
 * - acquisitionTime: czas akwizycji [s] - jak długo trwa pomiar
 *   Zależność: acquisitionTime = bufferSize / sampleRateHz
 *   Przykład: 4000 próbek / 10000 Hz = 0.4 s
 *
 * - inputRangeV: zakres wejściowy [V] - maksymalne napięcie wejściowe
 *   Dla DWF typowe zakresy: ±5V, ±10V, ±25V
 *   Mniejszy zakres = wyższa rozdzielczość pomiaru
 */
public class AcquisitionConfig {

    // ===== POLA KONFIGURACYJNE =====

    // Częstotliwość próbkowania w Hz (próbek na sekundę)
    private int sampleRateHz;

    // Rozmiar bufora - ile próbek może zmieścić się w pamięci
    private int bufferSize;

    // Czas trwania akwizycji w sekundach
    private double acquisitionTime;

    // Zakres napięcia wejściowego w woltach
    private double inputRangeV;

    // ===== STAŁE DOMYŚLNE I OGRANICZENIA =====

    // Domyślny zakres napięcia wejściowego: ±25V
    public static final double DEFAULT_INPUT_RANGE_V = 25.0;

    // Domyślna liczba punktów na wykresach
    public static final int DEFAULT_PLOT_POINTS = 10000;

    // Minimalny czas akwizycji: 10ms
    // Krótszy czas może nie wystarczyć do zebrania kompletnej pętli histerezy
    public static final double MIN_ACQUISITION_TIME = 0.01;

    // Maksymalny czas akwizycji: 10s
    // Dłuższy czas może powodować przepełnienie bufora i problemy z pamięcią
    public static final double MAX_ACQUISITION_TIME = 10.0;

    // Minimalny rozmiar bufora: 100 próbek
    // Mniej próbek nie pozwoli na sensowną analizę sygnału
    public static final int MIN_BUFFER_SIZE = 100;

    // Maksymalny rozmiar bufora: 10000 próbek
    // Ograniczenie wynikające z możliwości urządzenia DWF i pamięci RAM
    public static final int MAX_BUFFER_SIZE = 10000;

    /// Konstruktor Domyslny
    public AcquisitionConfig() {
        // Ustawienie częstotliwości próbkowania na 10 kHz
        this.sampleRateHz = 10_000;

        // Ustawienie rozmiaru bufora na 4000 próbek
        this.bufferSize = 4000;

        // Ustawienie czasu akwizycji na 0.4s (400ms)
        this.acquisitionTime = 0.4;

        // Ustawienie zakresu wejściowego na wartość domyślną (±25V)
        this.inputRangeV = DEFAULT_INPUT_RANGE_V;
    }

    // ===== GETTERY I SETTERY =====

    public int getSampleRateHz() {
        return sampleRateHz;
    }

    public void setSampleRateHz(int sampleRateHz) {
        this.sampleRateHz = sampleRateHz;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public double getAcquisitionTime() {
        return acquisitionTime;
    }

    public void setAcquisitionTime(double acquisitionTime) {
        this.acquisitionTime = acquisitionTime;
    }

    public double getInputRangeV() {
        return inputRangeV;
    }

    public void setInputRangeV(double inputRangeV) {
        this.inputRangeV = inputRangeV;
    }

    // ===== METODY OBLICZENIOWE ===== //
    public int calculateBufferFromTime() {
        // Mnożymy czas (w sekundach) przez częstotliwość (próbki/sekundę)
        // Wynik to liczba próbek potrzebnych do pokrycia zadanego czasu
        // Math.round() zaokrągla do najbliższej liczby całkowitej
        return (int) Math.round(acquisitionTime * sampleRateHz);
    }

    public double calculateTimeFromBuffer() {
        // Dzielimy liczbę próbek przez częstotliwość próbkowania
        // Wynik to czas w sekundach potrzebny na zebranie wszystkich próbek
        // Rzutowanie (double) zapewnia wynik zmiennoprzecinkowy
        return (double) bufferSize / sampleRateHz;
    }
}
