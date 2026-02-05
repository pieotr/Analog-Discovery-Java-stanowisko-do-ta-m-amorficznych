# Stanowisko do pomiaru pętli histerezy materiałów magnetycznych

## Opis projektu

Aplikacja JavaFX do pomiarów i analizy pętli histerezy (B-H) materiałów magnetycznych przy użyciu urządzenia Digilent Analog Discovery. Program umożliwia precyzyjną akwizycję danych, cyfrowe przetwarzanie sygnałów oraz wizualizację charakterystyk magnetycznych badanych materiałów.

## Wymagania systemowe

### Sprzęt
- **Digilent Analog Discovery 2** lub kompatybilne urządzenie DWF
- Badany rdzeń magnetyczny z cewkami:
  - Cewka wzbudzająca (generowanie pola H)
  - Cewka pomiarowa (pomiar indukcji B)
  - Rezystor shuntowy do pomiaru prądu

### Oprogramowanie
- **Java JDK 17** lub nowsza
- **Maven 3.x** (do budowania projektu)
- **Digilent WaveForms SDK** (biblioteki natywne `libdwf.so`)
  - Linux: https://digilent.com/reference/software/waveforms/waveforms-sdk/start
  - Arch Linux: `yay -S digilent-waveforms`

### Zależności Maven
- JavaFX 17.0.14 (GUI)
- waveforms4j 0.0.11 (komunikacja z Analog Discovery)
- XChart 3.8.8 (wizualizacja wykresów)
- ControlsFX, FormsFX, ValidatorFX (dodatkowe kontrolki UI)

## Instalacja

### 1. Instalacja Digilent WaveForms SDK

**Linux (Arch-based):**
```bash
yay -S digilent-waveforms
```

**Linux (inne dystrybucje):**
- Pobierz SDK ze strony Digilent
- Zainstaluj pakiet DEB/RPM
- Sprawdź czy `libdwf.so` znajduje się w `/usr/lib` lub `/usr/local/lib`

### 2. Klonowanie i budowanie projektu

```bash
cd demo2
mvn clean install
```

### 3. Uruchomienie aplikacji

**Z IDE (IntelliJ IDEA / Eclipse):**
- Otwórz projekt w IDE
- Uruchom klasę `AnalogFifoScanShiftAcquisition.java`

**Z linii poleceń:**
```bash
mvn javafx:run
```

## Architektura aplikacji

### Wzorzec MVC (Model-View-Controller)

```
demo2/
├── src/main/java/org/example/demo2/
│   ├── AnalogFifoScanShiftAcquisition.java  # Główna klasa aplikacji
│   ├── controller/
│   │   └── MainController.java              # Logika UI i koordynacja
│   ├── model/
│   │   ├── AcquisitionConfig.java           # Parametry akwizycji
│   │   ├── PhysicalParameters.java          # Parametry geometryczne układu
│   │   └── HysteresisData.java              # Kontener danych pomiarowych
│   ├── service/
│   │   ├── DataAcquisitionService.java      # Komunikacja z DWF
│   │   ├── SignalProcessingService.java     # Przetwarzanie sygnałów
│   │   └── StatisticsService.java           # Obliczenia statystyczne
│   └── resources/
│       └── main-view.fxml                    # Definicja interfejsu UI
```

## Zasada działania

### 1. Pomiar natężenia pola magnetycznego H

```
Cewka wzbudzająca → Prąd I → Rezystor shuntowy → Pomiar napięcia V_shunt
```

**Wzór:**
```
H = (N_exc × I) / l_e
I = V_shunt / R_s

H = (N_exc / (l_e × R_s)) × V_shunt  [A/m]
```

Gdzie:
- `N_exc` - liczba zwojów cewki wzbudzającej
- `l_e` - długość drogi magnetycznej [m]
- `R_s` - rezystancja shuntowa [Ω]
- `V_shunt` - napięcie mierzone na shuncie [V]

### 2. Pomiar indukcji magnetycznej B

```
Zmienne pole B → Cewka pomiarowa → Napięcie indukowane V_ind → Całkowanie → B
```

**Prawo Faradaya:**
```
V_ind = -N_B × A_e × dB/dt
```

**Po scałkowaniu:**
```
B = (1 / (N_B × A_e)) × ∫V_ind dt  [T]
```

Gdzie:
- `N_B` - liczba zwojów cewki pomiarowej
- `A_e` - pole przekroju rdzenia [m²]
- `∫V_ind dt` - całka napięcia (filtr RC cyfrowy)

### 3. Cyfrowe całkowanie sygnału

Aplikacja implementuje **cyfrowy filtr RC** metodą trapezów:

```
τ = R × C = 800Ω × 470nF = 376μs
f_c = 1/(2πτ) ≈ 423 Hz

y[n] = a × y[n-1] + b × (x[n] + x[n-1])

a = (2τ - dt) / (2τ + dt)
b = dt / (2τ + dt)
```

**Zalety metody cyfrowej:**
- Brak dryftu analogowego
- Stabilność obliczeniowa
- Możliwość zmiany parametrów bez przebudowy sprzętu

### 4. Algorytm uśredniania pętli histerezy

1. **Detekcja kierunku** - rozdzielenie punktów na gałąź rosnącą i malejącą
2. **Binning** - podział osi X na 150 przedziałów
3. **Uśrednianie** - obliczenie średniej wartości Y w każdym przedziale
4. **Rysowanie** - połączenie punktów w zamkniętą pętlę

## Interfejs użytkownika

### Panel akwizycji danych
- **Częstotliwość próbkowania**: 100 Hz - 1 MHz
- **Czas akwizycji**: 0.01s - 10s
- **Rozmiar bufora**: 100 - 10000 próbek
- Szybkie ustawienia: 100ms, 1s, 5s

### Parametry fizyczne układu

**Cewka wzbudzająca:**
- `N_exc` - liczba zwojów (domyślnie: 100)
- `l_e` - długość drogi magnetycznej (domyślnie: 0.1 m)
- `R_s` - rezystancja shuntowa (domyślnie: 1.0 Ω)

**Cewka pomiarowa:**
- `N_B` - liczba zwojów (domyślnie: 50)
- `A_e` - pole przekroju rdzenia (domyślnie: 1e-4 m² = 1 cm²)

### Generator sygnału (wyjście analogowe)

Konfiguracja sygnału wzbudzającego:
- **Kształt fali**: Sinus, Prostokąt, Trójkąt, Stała
- **Częstotliwość**: 1 Hz - 20 kHz (domyślnie: 100 Hz)
- **Amplituda**: 0 V - 5 V (domyślnie: 2 V)
- **Offset**: -5 V do +5 V (domyślnie: 0 V)

### Wizualizacja danych

**Wykres 1: CH0 - Indukcja magnetyczna B(t)**
- Wykres czasowy scałkowanego sygnału
- Proporcjonalny do indukcji B

**Wykres 2: CH1 - Natężenie pola H(t)**
- Wykres czasowy napięcia na shuncie
- Proporcjonalny do prądu i natężenia H

**Wykres 3: Pętla histerezy B-H (punktowy)**
- Żółte punkty - surowy sygnał pomiarowy
- Fioletowe punkty - uśredniona pętla histerezy

**Canvas: Uśredniona pętla histerezy (linia ciągła)**
- Gładka krzywa histerezy
- Automatyczne skalowanie
- Siatka pomocnicza

### Statystyki pomiarowe

**Dla każdego kanału:**
- `Min` - wartość minimalna
- `Max` - wartość maksymalna
- `P2P` - amplituda peak-to-peak (Max - Min)
- `RMS` - wartość skuteczna

**Parametry magnetyczne:**
- `B_sat` [T] - nasycenie indukcyjne (maksymalna |B|)
- `B_r` [T] - indukcyjność remanentna (B przy H=0)
- `H_c` [A/m] - koercja (H przy B=0)

## Procedura pomiarowa

### 1. Konfiguracja sprzętowa

```
Analog Discovery 2:
├── AWG CH0 (wyjście analogowe) → Wzmacniacz → Cewka wzbudzająca
├── Scope CH0 (wejście 1) → Cewka pomiarowa (∫V → B)
└── Scope CH1 (wejście 2) → Rezystor shuntowy (V → I → H)
```

### 2. Procedura konfiguracji w aplikacji

1. **Uruchom aplikację**
2. **Wprowadź parametry geometryczne** rdzenia i cewek
3. **Skonfiguruj generator**:
   - Wybierz kształt fali (zazwyczaj SINUS)
   - Ustaw częstotliwość (50-100 Hz dla ferrytu)
   - Ustaw amplitudę (zacząć od małej, np. 1V)
4. **Naciśnij "Zastosuj i WŁĄCZ wyjście"**
5. **Ustaw parametry akwizycji**:
   - Czas akwizycji: minimum 2-3 okresy sygnału wzbudzającego
   - Dla 100 Hz → T = 0.01s → Czas akwizycji ≥ 0.03s
6. **Naciśnij "Rozpocznij pomiar"**
7. **Analiza wyników**:
   - Sprawdź czy pętla się zamyka
   - Odczytaj B_sat, B_r, H_c
   - Jeśli pętla jest niesymetryczna, zwiększ amplitudę generatora

### 3. Rozwiązywanie problemów

**Problem: Pusta pętla histerezy**
- Sprawdź połączenia cewek
- Zwiększ amplitudę generatora
- Sprawdź czy wyjście jest włączone

**Problem: Pętla niesymetryczna**
- Zwiększ czas akwizycji (więcej okresów)
- Zwiększ amplitudę wzbudzenia
- Sprawdź offset generatora (powinien być 0V)

**Problem: Zbyt duży szum**
- Zmniejsz zakres wejściowy (zmniejsz INPUT_RANGE_V)
- Użyj ekranowanych przewodów
- Zwiększ liczbę zwojów cewki pomiarowej

**Problem: Błąd "libdwf.so not found"**
- Zainstaluj Digilent WaveForms SDK
- Sprawdź ścieżkę: `ldconfig -p | grep libdwf`

## Teoria pomiaru histerezy

### Pętla histerezy

Pętla histerezy opisuje zależność indukcji magnetycznej **B** od natężenia pola **H** w materiale ferromagnetycznym. Pokazuje efekt histerezy magnetycznej - zjawisko opóźnienia zmian B względem zmian H.

```
    B↑
    │        ╱──╲ Nasycenie (+B_sat)
    │       ╱    ╲
B_r │      ╱      ╲
────┼─────┼───────┼────→ H
   -H_c   │       H_c
          │╲      ╱
          │ ╲____╱ Nasycenie (-B_sat)
```

**Kluczowe parametry:**
- **B_sat** - nasycenie indukcyjne: maksymalna wartość B przy dużym H
- **B_r** - remanencja: pozostała indukcja po wyłączeniu pola (H=0)
- **H_c** - koercja: pole potrzebne do całkowitego odmagnesowania (B=0)

### Zastosowania

**Charakteryzacja materiałów magnetycznych:**
- Ferryty (rdzenie transformatorów, dławików)
- Materiały miękkie (transformatory energetyczne)
- Materiały twarde (magnesy trwałe)
- Materiały amorficzne (zaawansowane zastosowania)

**Obliczanie strat mocy:**
```
P_loss = f × ∮ H dB  [W/m³]
```
Pole pętli histerezy = straty energii na cykl magnesowania

## Struktura kodu - kluczowe klasy

### MainController.java
Główny kontroler aplikacji - koordynuje wszystkie operacje:
- Inicjalizacja UI (wykresy, spinnery, pola tekstowe)
- Obsługa zdarzeń (przyciski, zmiany wartości)
- Akwizycja danych w wątku roboczym
- Aktualizacja wykresów i statystyk
- Algorytm uśredniania pętli histerezy
- Rysowanie na Canvas

### DataAcquisitionService.java
Komunikacja z urządzeniem Analog Discovery:
- Konfiguracja wejść analogowych (zakres, częstotliwość)
- Akwizycja danych z dwóch kanałów równocześnie
- Konfiguracja generatora sygnału (kształt, częstotliwość, amplituda)
- Zarządzanie stanem urządzenia

### SignalProcessingService.java
Przetwarzanie sygnałów:
- **Cyfrowe całkowanie** - symulacja filtru RC metodą trapezów
- **Interpolacja liniowa** - znajdowanie wartości B_r i H_c
- Szczegółowe komentarze teoretyczne

### StatisticsService.java
Obliczenia statystyczne:
- Min, Max, Peak-to-Peak
- RMS (wartość skuteczna)
- Formatowanie wyników

### Model classes
- **AcquisitionConfig** - parametry pomiarowe
- **PhysicalParameters** - parametry geometryczne, współczynniki skalowania
- **HysteresisData** - kontener danych pomiarowych

## Licencja

Projekt edukacyjny - do swobodnego użytku.

## Autor

Projekt stworzony na potrzeby stanowiska laboratoryjnego do badania materiałów magnetycznych.

## Bibliografia

1. Prawo indukcji Faradaya - https://en.wikipedia.org/wiki/Faraday%27s_law_of_induction
2. Pętla histerezy - https://en.wikipedia.org/wiki/Hysteresis
3. Digilent WaveForms SDK - https://digilent.com/reference/software/waveforms/waveforms-sdk/start
4. Metoda trapezów - https://en.wikipedia.org/wiki/Trapezoidal_rule
