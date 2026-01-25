package org.example.demo2.model;

/**
 * KLASA PARAMETRÓW FIZYCZNYCH UKŁADU POMIAROWEGO
 * ==============================================
 *
 * Przechowuje parametry geometryczne i elektryczne układu pomiarowego pętli histerezy.
 * Parametry te są niezbędne do przeliczenia surowych napięć z pomiarów na wielkości fizyczne.
 *
 * FIZYKA POMIARÓW MAGNETYCZNYCH:
 * ==============================
 *
 * POMIAR NATĘŻENIA POLA MAGNETYCZNEGO H:
 * ----------------------------------------
 * 1. Cewka wzbudzająca (N_exc zwojów) wytwarza pole magnetyczne
 * 2. Prąd przez cewkę mierzymy za pomocą rezystora shuntowego (R_s)
 * 3. Napięcie na boczniku: V_shunt = I * R_s  (prawo Ohma)
 * 4. Prąd przez cewkę: I = V_shunt / R_s
 * 5. Natężenie pola w rdzeniu: H = (N_exc * I) / l_e  [A/m]
 *
 * WZÓR: H = (N_exc / (l_e * R_s)) * V_shunt
 *
 * Gdzie:
 * - N_exc = liczba zwojów cewki wzbudzającej
 * - l_e = długość drogi magnetycznej w rdzeniu [m]
 * - R_s = rezystancja shuntowa [Ω]
 * - V_shunt = mierzone napięcie na shuncie [V]
 *
 * POMIAR INDUKCJI MAGNETYCZNEJ B:
 * --------------------------------
 * 1. Cewka pomiarowa (N_B zwojów) otacza rdzeń magnetyczny
 * 2. Zmienne pole magnetyczne indukuje napięcie w cewce (prawo Faradaya)
 * 3. Napięcie indukowane: V_ind = -N_B * A_e * dB/dt
 * 4. Całkowanie napięcia daje indukcję: B = ∫(V_ind)dt / (N_B * A_e)
 *
 * WZÓR: B = (1 / (N_B * A_e)) * ∫V_ind dt
 *
 * Gdzie:
 * - N_B = liczba zwojów cewki pomiarowej
 * - A_e = pole przekroju rdzenia [m²]
 * - V_ind = mierzone napięcie indukowane [V]
 * - ∫V_ind dt = całka napięcia (wykonana przez filtr RC lub numerycznie)
 *
 */
public class PhysicalParameters {

    // ===== PARAMETRY CEWKI WZBUDZAJĄCEJ (DO POMIARU H) =====

    /**
     * LICZBA ZWOJÓW CEWKI WZBUDZAJĄCEJ (N_exc)
     * ========================================
     * Cewka nawinięta na rdzeń, przez którą przepuszczamy prąd zmienny
     * aby wytworzyć zmienne pole magnetyczne.
     *
     * Domyślnie: 100 zwojów
     * Typowy zakres: 10-1000 zwojów (zależy od geometrii rdzenia)
     *
     * FIZYKA: Więcej zwojów = silniejsze pole przy tym samym prądzie
     * H = N * I / l_e, więc H ~ N_exc
     */
    private double turnsExc = 100.0;

    /**
     * DŁUGOŚĆ DROGI MAGNETYCZNEJ (l_e)
     * =================================
     * Długość zamkniętej ścieżki strumienia magnetycznego w rdzeniu [m].
     *
     * Dla toroidu: l_e = 2π * r_średni
     * Dla rdzenia E: l_e ≈ długość całej zamkniętej pętli
     *
     * Domyślnie: 0.1 m (10 cm)
     * Typowy zakres: 0.05 - 0.5 m
     *
     * FIZYKA: Dłuższa droga = słabsze pole przy tym samym prądzie
     * H = N * I / l_e, więc H ~ 1/l_e
     */
    private double pathLen = 0.1;

    /**
     * REZYSTANCJA bocznika (R_s)
     * ==========================
     * Rezystor pomiarowy włączony szeregowo z cewką wzbudzającą [Ω].
     * Spadek napięcia na boczniku jest proporcjonalny do prądu (V = I * R).
     *
     * Domyślnie: 1.0 Ω
     * Typowy zakres: 0.1 - 10 Ω
     *
     */
    private double shunt = 1.0;

    // ===== PARAMETRY CEWKI POMIAROWEJ (DO POMIARU B) =====

    /**
     * LICZBA ZWOJÓW CEWKI POMIAROWEJ (N_B)
     * ====================================
     * Cewka nawinięta na rdzeń, w której indukuje się napięcie.
     *
     * Domyślnie: 50 zwojów
     *
     * FIZYKA: Prawo indukcji Faradaya: V_ind = -N_B * A_e * dB/dt
     * Więcej zwojów = wyższe napięcie indukowane = łatwiejszy pomiar
     * Ale też większa czułość na zakłócenia!
     */
    private double turnsB = 50.0;

    /**
     * POLE PRZEKROJU RDZENIA (A_e)
     * ============================
     * Pole powierzchni przekroju poprzecznego rdzenia magnetycznego [m²].
     *
     * Domyślnie: 1e-4 m² = 1 cm²
     * Typowy zakres: 1e-5 do 1e-3 m² (0.1 cm² do 10 cm²)
     *
     * POMIAR: A_e = szerokość × wysokość przekroju rdzenia
     * Przykład: rdzeń 10mm × 10mm → A_e = 0.01m × 0.01m = 1e-4 m²
     *
     * FIZYKA: Większy przekrój = więcej linii pola = silniejsza indukcja
     * Strumień magnetyczny: Φ = B * A_e
     */
    private double area = 1e-4;

    // ===== METODY OBLICZENIOWE - WSPÓŁCZYNNIKI SKALOWANIA =====

    /**
     * WSPÓŁCZYNNIK SKALOWANIA DLA NATĘŻENIA POLA H
     * ============================================
     *
     * Oblicza współczynnik konwersji z napięcia na shuncie [V] na natężenie pola [A/m].
     * współczynnik skalowania [(A/m)/V] - ile A/m na 1 volt
     */
    public double hScale() {
        // Wzór: k_H = N_exc / (l_e * R_s)
        // Licznik: liczba zwojów cewki wzbudzającej
        // Mianownik: długość drogi magnetycznej × rezystancja shuntowa
        return turnsExc / (pathLen * shunt);
    }

    /**
     * WSPÓŁCZYNNIK SKALOWANIA DLA INDUKCJI MAGNETYCZNEJ B
     * ==================================================
     * Oblicza współczynnik konwersji z scałkowanego napięcia [V·s] na indukcję [T].
     * współczynnik skalowania [T/(V·s)] - ile Tesla na 1 volt-sekundę
     */
    public double bScale() {
        // Wzór: k_B = 1 / (N_B * A_e)
        // Licznik: 1 (stała)
        // Mianownik: liczba zwojów cewki pomiarowej × pole przekroju rdzenia
        return 1.0 / (turnsB * area);
    }

    // ===== GETTERY I SETTERY =====

    public double getTurnsExc() {
        return turnsExc;
    }

    public void setTurnsExc(double turnsExc) {
        this.turnsExc = turnsExc;
    }

    public double getPathLen() {
        return pathLen;
    }

    public void setPathLen(double pathLen) {
        this.pathLen = pathLen;
    }

    public double getShunt() {
        return shunt;
    }

    public void setShunt(double shunt) {
        this.shunt = shunt;
    }

    public double getTurnsB() {
        return turnsB;
    }

    public void setTurnsB(double turnsB) {
        this.turnsB = turnsB;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }
}
