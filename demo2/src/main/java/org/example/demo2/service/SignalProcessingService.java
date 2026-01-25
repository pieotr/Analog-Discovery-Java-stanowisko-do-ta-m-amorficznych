package org.example.demo2.service;

/**
 * SERWIS PRZETWARZANIA SYGNAŁÓW
 * ==============================
 *
 * Klasa odpowiedzialna za przetwarzanie cyfrowe sygnałów pomiarowych.
 * Główne zadania:
 * 1. Całkowanie sygnału z cewki pomiarowej (symulacja filtru RC)
 * 2. Interpolacja liniowa do znajdowania wartości charakterystycznych
 *
 * TEORIA - DLACZEGO CAŁKUJEMY SYGNAŁ?
 * ====================================
 *
 * Prawo indukcji Faradaya mówi, że napięcie indukowane w cewce jest proporcjonalne
 * do szybkości zmian strumienia magnetycznego:
 *
 * V_ind = -N * dΦ/dt = -N * A * dB/dt
 *
 * Gdzie:
 * - V_ind = napięcie indukowane [V]
 * - N = liczba zwojów cewki
 * - Φ = strumień magnetyczny [Wb]
 * - B = indukcja magnetyczna [T]
 * - A = pole przekroju [m²]
 *
 * Aby otrzymać B z V_ind, musimy SCAŁKOWAĆ napięcie:
 *
 * dB = -V_ind * dt / (N * A)
 * B(t) = -∫V_ind dt / (N * A) + B(0)
 *
 * METODY CAŁKOWANIA:
 * ==================
 *
 * 1. ANALOGOWE (hardware):
 *    - Filtr RC (rezystor + kondensator)
 *    - V_out = (1/RC) * ∫V_in dt
 *    - Proste, ale wymaga kalibracji i ma dryft
 *
 * 2. CYFROWE (software - ta implementacja):
 *    - Symulacja filtru RC algorytmem rekurencyjnym
 *    - Dokładne, stabilne, bez dryftu
 *    - Możliwość zmiany parametrów bez przebudowy sprzętu
 */
public class SignalProcessingService {

    /**
     * CYFROWE CAŁKOWANIE SYGNAŁU - SYMULACJA FILTRU RC
     * =================================================
     *
     * Implementuje cyfrowy filtr całkujący bazujący na metodzie trapezów
     * i symulujący działanie analogowego filtru RC pierwszego rzędu.
     *
     * TEORIA FILTRU RC:
     * =================
     *
     * Analogowy filtr RC:
     *
     *    V_in ──[R]───┬───> V_out
     *                 │
     *                [C]
     *                 │
     *                GND
     *
     * Równanie różniczkowe filtru RC:
     * τ * dV_out/dt + V_out = V_in
     *
     * Gdzie τ = R * C (stała czasowa)
     *
     * DYSKRETYZACJA - METODA TRAPEZÓW:
     * =================================
     *
     * Zastępujemy pochodną różnicą skończoną (metoda trapezów):
     *
     * dV_out/dt ≈ (V_out[n] - V_out[n-1]) / dt
     *
     * Podstawiamy do równania filtru:
     * τ * (V_out[n] - V_out[n-1])/dt + V_out[n] = (V_in[n] + V_in[n-1])/2
     *
     * Przekształcamy aby wyrazić V_out[n]:
     * (2τ + dt) * V_out[n] = (2τ - dt) * V_out[n-1] + dt * (V_in[n] + V_in[n-1])
     *
     * V_out[n] = [(2τ - dt)/(2τ + dt)] * V_out[n-1] + [dt/(2τ + dt)] * (V_in[n] + V_in[n-1])
     *
     * Definiujemy współczynniki:
     * a = (2τ - dt) / (2τ + dt)  ← współczynnik wyjścia poprzedniego
     * b = dt / (2τ + dt)         ← współczynnik wejść
     *
     * WZÓR REKURENCYJNY:
     * y[n] = a * y[n-1] + b * (x[n] + x[n-1])
     *
     * ANALIZA WSPÓŁCZYNNIKÓW:
     * =======================
     *
     * Dla τ >> dt (wolny filtr, niska częstotliwość odcięcia):
     * - a ≈ 1 - dt/(2τ) ≈ 1      (silna pamięć poprzednich wartości)
     * - b ≈ dt/(2τ) ≈ 0          (małe wzmocnienie wejścia)
     * - Zachowanie: silne całkowanie, wolna odpowiedź
     *
     * Dla τ << dt (szybki filtr, wysoka częstotliwość odcięcia):
     * - a ≈ -1                    (odwrócenie fazy)
     * - b ≈ dt/(2τ) >> 1          (duże wzmocnienie)
     * - Zachowanie: słabe całkowanie, szybka odpowiedź
     *
     * WYBÓR STAŁEJ CZASOWEJ τ:
     * ========================
     *
     * W naszej aplikacji: τ = 800.0 * 470e-9 = 376 μs
     * Częstotliwość odcięcia: f_c = 1/(2π*τ) ≈ 423 Hz
     *
     * To oznacza, że:
     * - Sygnały poniżej 423 Hz są dobrze całkowane
     * - Sygnały powyżej 423 Hz są tłumione
     *
     * Dla częstotliwości pracy 50-100 Hz to dobry wybór
     *
     * @param v tablica surowych próbek napięcia z cewki pomiarowej [V]
     *          (wejście do całkowania)
     * @param sampleRateHz częstotliwość próbkowania [Hz]
     *                     (potrzebna do obliczenia dt = 1/sampleRateHz)
     * @return tablica scałkowanych wartości (proporcjonalnych do B)
     *         (wyjście z całkowania)
     */
    public double[] integrate(double[] v, int sampleRateHz) {
        // ===== PARAMETRY FILTRU RC =====

        // Stała czasowa filtru RC: τ = R * C
        // Wartość dobrana empirycznie dla pomiarów magnetycznych
        // R = 800 Ω, C = 470 nF → τ = 376 μs
        double tau = 800.0 * 470e-9;

        // Krok czasowy między próbkami: dt = 1 / częstotliwość
        // Dla 10 kHz: dt = 0.0001s = 0.1ms
        double dt = 1.0 / sampleRateHz;

        // ===== OBLICZENIE WSPÓŁCZYNNIKÓW FILTRU =====

        // Współczynnik 'a' - waga poprzedniej wartości wyjścia
        // a = (2τ - dt) / (2τ + dt)
        // Im większe τ, tym a bliższe 1 (silniejsza "pamięć")
        double a = (2 * tau - dt) / (2 * tau + dt);

        // Współczynnik 'b' - waga wartości wejściowych
        // b = dt / (2τ + dt)
        // Im większe τ, tym mniejsze b (słabsze wzmocnienie wejścia)
        double b = dt / (2 * tau + dt);

        // ===== INICJALIZACJA =====

        // Tablica wyjściowa - ta sama długość co wejściowa
        double[] out = new double[v.length];

        // Zmienne stanu filtru (pamięć poprzednich wartości)
        double yPrev = 0;  // Poprzednia wartość wyjścia y[n-1]
        double xPrev = 0;  // Poprzednia wartość wejścia x[n-1]

        // ===== PĘTLA GŁÓWNA - FILTRACJA REKURENCYJNA =====

        // Przechodzimy przez wszystkie próbki
        for (int i = 0; i < v.length; i++) {
            // WZÓR REKURENCYJNY FILTRU:
            // y[n] = a * y[n-1] + b * (x[n] + x[n-1])
            //
            // Gdzie:
            // - y[n] = obecna wartość wyjścia (to co obliczamy)
            // - y[n-1] = poprzednia wartość wyjścia (yPrev)
            // - x[n] = obecna wartość wejścia (v[i])
            // - x[n-1] = poprzednia wartość wejścia (xPrev)

            // Obliczenie aktualnej wartości wyjścia
            // Składa się z dwóch części:
            // 1. a * yPrev - wpływ poprzedniego wyjścia (pamięć filtru)
            // 2. b * (v[i] + xPrev) - wpływ wejść (metoda trapezów)
            double y = a * yPrev + b * (v[i] + xPrev);

            // Zapisanie wyniku do tablicy wyjściowej
            out[i] = y;

            // Aktualizacja zmiennych stanu dla następnej iteracji
            yPrev = y;      // Obecne wyjście staje się poprzednim
            xPrev = v[i];   // Obecne wejście staje się poprzednim
        }

        // Zwrócenie scałkowanego sygnału
        // INTERPRETACJA FIZYCZNA:
        // Każdy element out[i] jest proporcjonalny do indukcji magnetycznej B
        // w momencie czasu t = i * dt
        // Aby otrzymać rzeczywiste wartości B [T], należy przemnożyć przez bScale()
        return out;
    }

    /**
     * INTERPOLACJA LINIOWA - ZNAJDOWANIE WARTOŚCI DLA ZADANEJ WSPÓŁRZĘDNEJ X
     * ======================================================================
     *
     * Znajduje wartość y dla zadanego x używając interpolacji liniowej między punktami.
     *
     */
    public double interpolateAtX(java.util.List<Double> x, java.util.List<Double> y, double x0) {
        // Przechodzimy przez wszystkie pary sąsiednich punktów
        // Szukamy pary (x[i-1], x[i]) która "otacza" wartość x0
        for (int i = 1; i < x.size(); i++) {
            // Sprawdzenie czy x0 leży między x[i-1] a x[i]
            // Uwzględniamy dwa przypadki:
            // 1. x rosnące: x[i-1] ≤ x0 ≤ x[i]
            // 2. x malejące: x[i-1] ≥ x0 ≥ x[i]
            if ((x.get(i - 1) <= x0 && x.get(i) >= x0) ||
                (x.get(i - 1) >= x0 && x.get(i) <= x0)) {

                // Znaleźliśmy właściwy przedział!
                // Obliczamy parametr interpolacji t
                // t = 0: jesteśmy w punkcie i-1
                // t = 1: jesteśmy w punkcie i
                // t = 0.5: jesteśmy w połowie drogi
                double t = (x0 - x.get(i - 1)) / (x.get(i) - x.get(i - 1));

                // Interpolacja liniowa wartości y
                // y0 = y[i-1] + t * (y[i] - y[i-1])
                return y.get(i - 1) + t * (y.get(i) - y.get(i - 1));
            }
        }

        // Jeśli nie znaleziono przedziału (x0 poza zakresem danych), zwracamy 0
        // W praktyce oznacza to, że punkt nie został zmierzony
        return 0;
    }

    /**
     * INTERPOLACJA LINIOWA - ZNAJDOWANIE WSPÓŁRZĘDNEJ X DLA ZADANEJ WARTOŚCI Y
     * ========================================================================
     */
    public double interpolateAtY(java.util.List<Double> x, java.util.List<Double> y, double y0) {
        // Przechodzimy przez wszystkie pary sąsiednich punktów
        // Szukamy pary (y[i-1], y[i]) która "otacza" wartość y0
        for (int i = 1; i < y.size(); i++) {
            // Sprawdzenie czy y0 leży między y[i-1] a y[i]
            // Uwzględniamy dwa przypadki:
            // 1. y rosnące: y[i-1] ≤ y0 ≤ y[i]
            // 2. y malejące: y[i-1] ≥ y0 ≥ y[i]
            if ((y.get(i - 1) <= y0 && y.get(i) >= y0) ||
                (y.get(i - 1) >= y0 && y.get(i) <= y0)) {

                // Znaleźliśmy właściwy przedział
                // Obliczamy parametr interpolacji t względem y
                // t = 0: jesteśmy w punkcie i-1
                // t = 1: jesteśmy w punkcie i
                double t = (y0 - y.get(i - 1)) / (y.get(i) - y.get(i - 1));

                // Interpolacja liniowa wartości x
                // x0 = x[i-1] + t * (x[i] - x[i-1])
                return x.get(i - 1) + t * (x.get(i) - x.get(i - 1));
            }
        }

        // Jeśli nie znaleziono przedziału (y0 poza zakresem danych), zwracamy 0
        // Może to oznaczać, że materiał nie osiągnął wystarczającego namagnesowania
        return 0;
    }
}
