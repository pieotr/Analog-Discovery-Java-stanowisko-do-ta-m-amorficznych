package org.example.demo2.controller;

import javafx.application.Platform;  // Dla operacji na wątku UI w JavaFX
import javafx.fxml.FXML;             // Adnotacja do wstrzykiwania elementów FXML
import javafx.fxml.Initializable;    // Interfejs do inicjalizacji kontrolera
import javafx.geometry.Insets;       // Marginesy i dopełnienia w layoutach
import javafx.geometry.Pos;          // Pozycjonowanie elementów
import javafx.scene.Node;            // Podstawowa klasa węzła w scenie JavaFX
import javafx.scene.canvas.Canvas;   // Komponent do rysowania bitmap
import javafx.scene.canvas.GraphicsContext; // Kontekst graficzny Canvas
import javafx.scene.chart.*;         // Komponenty wykresów (LineChart, ScatterChart)
import javafx.scene.control.*;       // Kontrolki UI (Label, Spinner, TextField itp.)
import javafx.scene.layout.HBox;     // Kontener poziomy
import javafx.scene.paint.Color;     // Kolory w JavaFX
import javafx.scene.shape.Rectangle; // Prostokąt do rysowania
import org.example.demo2.model.AcquisitionConfig; // Model konfiguracji akwizycji
import org.example.demo2.model.HysteresisData;    // Model danych histerezy
import org.example.demo2.model.PhysicalParameters; // Model parametrów fizycznych
import org.example.demo2.service.DataAcquisitionService; // Serwis akwizycji danych
import org.example.demo2.service.SignalProcessingService; // Serwis przetwarzania sygnałów
import org.example.demo2.service.StatisticsService;       // Serwis statystyk
import org.knowm.waveforms4j.DWF;    // Biblioteka do komunikacji z Analog Discovery

import java.net.URL;                 // Klasa reprezentująca URL (dla Initializable)
import java.util.ArrayList;          // Lista dynamiczna
import java.util.List;               // Interfejs listy
import java.util.ResourceBundle;     // Bundle zasobów (dla Initializable)

/**
 * Główny kontroler aplikacji pomiaru histerezy.
 * Odpowiada za zarządzanie interfejsem użytkownika i koordynację pomiędzy
 * komponentami (model, serwisy, widok).
 */
public class MainController implements Initializable {

    // Wstrzyknięte komponenty FXML - widok definiowany w pliku FXML
    @FXML private Spinner<Integer> sampleRateSpinner;
    @FXML private Spinner<Double> acquisitionTimeSpinner;
    @FXML private Spinner<Integer> bufferSizeSpinner;
    @FXML private Label infoLabel;

    /// sekcja danych z pomiarów
    @FXML private LineChart<Number, Number> timeChartCH0; // Wykres czasowy dla kanału 0 (napięcie → indukcja B)
    @FXML private LineChart<Number, Number> timeChartCH1; // Wykres czasowy dla kanału 1 (prąd I)
    @FXML private ScatterChart<Number, Number> xyChart;   // Wykres XY (histereza B-H)
    @FXML private Canvas hysteresisCanvas;                // Canvas do rysowania pętli histerezy
    @FXML private javafx.scene.layout.Pane canvasContainer; // Kontener dla Canvas
    @FXML private HBox customLegend;

    @FXML private Label min0, max0, p2p0, rms0;           // Etykiety statystyk dla kanału 0
    @FXML private Label min1, max1, p2p1, rms1;           // Etykiety statystyk dla kanału 1
    @FXML private Label bsatLabel, brLabel, hcLabel;      // Etykiety parametrów obliczanych z histerezy

    @FXML private TextField turnsExcField, pathLenField, shuntField; // Pola parametrów cewki wzbudzającej
    @FXML private TextField turnsBField, areaField;                   // Pola parametrów cewki pomiarowej

    /// sekcja danych do generatora
    @FXML private ComboBox<String> waveBox;               // ComboBox do wyboru kształtu fali wyjściowej
    @FXML private Spinner<Double> freqSpinner;
    @FXML private Spinner<Double> ampSpinner;
    @FXML private Spinner<Double> offsetSpinner;

    /// zmienne pomocnicze do uzupełniania wykresów
    // Serie danych dla wykresów
    private XYChart.Series<Number, Number> timeCh0;
    private XYChart.Series<Number, Number> timeCh1;
    private XYChart.Series<Number, Number> xyRaw;
    private XYChart.Series<Number, Number> xyAvg;

    // Serwisy i modele
    private final DWF dwf = new DWF();                    // Instancja biblioteki WaveForms (komunikacja z urządzeniem)
    private DataAcquisitionService acquisitionService;
    private StatisticsService statisticsService;
    private SignalProcessingService signalProcessingService;
    private AcquisitionConfig config;
    private PhysicalParameters physicalParams;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inicjalizacja wszystkich komponentów przy uruchomieniu kontrolera
        config = new AcquisitionConfig();
        physicalParams = new PhysicalParameters();
        statisticsService = new StatisticsService();
        signalProcessingService = new SignalProcessingService();
        acquisitionService = new DataAcquisitionService(dwf);

        initializeCharts();
        initializeSpinners();
        initializeFields();
        setupCustomLegend();
        setupCanvasBinding();
        updateInfoLabel();
    }

    private void setupCanvasBinding() {
        // Zbindowanie rozmiaru Canvas do rozmiaru kontenera Pane
        canvasContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            hysteresisCanvas.setWidth(newVal.doubleValue());
        });
        canvasContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            hysteresisCanvas.setHeight(newVal.doubleValue());
        });
    }

    private void initializeCharts() {
        // Konfiguracja wykresu CH0 (napięcie całkowane - indukcja magnetyczna B)
        timeCh0 = new XYChart.Series<>();                          // Tworzenie nowej serii danych
        timeCh0.setName("CH0 (∫V → B)");                           // Nazwa serii wyświetlana w legendzie
        timeChartCH0.getData().add(timeCh0);                       // Dodanie serii do wykresu
        timeChartCH0.setAnimated(false);                           // Wyłączenie animacji dla lepszej wydajności
        timeChartCH0.setCreateSymbols(false);                      // Wyłączenie symboli punktów (tylko linia)

        // Konfiguracja wykresu CH1 (napięcie na boczniku - prąd I)
        timeCh1 = new XYChart.Series<>();
        timeCh1.setName("CH1 (Vshunt → I)");
        timeChartCH1.getData().add(timeCh1);
        timeChartCH1.setAnimated(false);
        timeChartCH1.setCreateSymbols(false);

        // Konfiguracja wykresu XY (zależność B od H - pętla histerezy)
        xyRaw = new XYChart.Series<>();
        xyRaw.setName("Punkty pomiarowe");                         // Surowy sygnał pomiarowy
        xyAvg = new XYChart.Series<>();
        xyAvg.setName("Uśredniona pętla");                         // Uśredniona pętla histerezy
        xyChart.getData().addAll(xyRaw, xyAvg);                    // Dodanie obu serii do wykresu
        xyChart.setAnimated(false);
        xyChart.setLegendVisible(false);                           // Ukrycie standardowej legendy (używamy własnej)
    }

    private void initializeSpinners() {
        //--- konfiguracja pobierania danych ---//
        // Konfiguracja spinnera częstotliwości próbkowania (od 100 Hz do 1 MHz)
        sampleRateSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 1_000_000, config.getSampleRateHz(), 100));
        sampleRateSpinner.setEditable(true);                       // Pozwala na ręczne wpisanie wartości
        sampleRateSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {  // Listener na zmianę wartości
            config.setSampleRateHz(newVal);                        // Aktualizacja konfiguracji
            updateAcquisitionParameters();                         // Przeliczenie zależnych parametrów
        });

        // Konfiguracja spinnera czasu akwizycji (od 0.01s do 10s)
        acquisitionTimeSpinner.setValueFactory(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(
                        AcquisitionConfig.MIN_ACQUISITION_TIME,
                        AcquisitionConfig.MAX_ACQUISITION_TIME,
                        config.getAcquisitionTime(), 0.01));        // Krok 0.01s przy zmianie z przycisku spinera
        acquisitionTimeSpinner.setEditable(true);
        acquisitionTimeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            config.setAcquisitionTime(newVal);                     // Aktualizacja czasu w konfiguracji
            calculateBufferFromTime();                             // Przeliczenie rozmiaru bufora z czasu
        });

        // Konfiguracja spinnera rozmiaru bufora (od 100 do 1,000,000 próbek)
        bufferSizeSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        AcquisitionConfig.MIN_BUFFER_SIZE,         // Minimalny bufor (100)
                        AcquisitionConfig.MAX_BUFFER_SIZE,         // Maksymalny bufor (1,000,000) -- ograniczony z powodu problemow z wydajnością
                        config.getBufferSize(), 100));
        bufferSizeSpinner.setEditable(true);
        bufferSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            config.setBufferSize(newVal);                          // Aktualizacja rozmiaru bufora
            calculateTimeFromBuffer();                             // Przeliczenie czasu z rozmiaru bufora
        });



        //--- konfiguracja generatora ---//
        // Konfiguracja spinnerów dla generatora sygnałów (wyjście analogowe)
        freqSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(1.0, 20_000.0, 100.0, 10.0));
        freqSpinner.setEditable(true);                             // Częstotliwość od 1Hz do 20kHz, domyślnie 100Hz krok 10hz

        ampSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 5.0, 2.0, 0.1));
        ampSpinner.setEditable(true);                              // Amplituda od 0 do 5V, domyślnie 2V

        offsetSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-5.0, 5.0, 0.0, 0.1));
        offsetSpinner.setEditable(true);                           // Offset od -5V do 5V, domyślnie 0V
    }

    private void initializeFields() {
        // Wypełnienie pól tekstowych wartościami domyślnymi z parametrów fizycznych
        turnsExcField.setText(String.valueOf(physicalParams.getTurnsExc()));
        pathLenField.setText(String.valueOf(physicalParams.getPathLen()));
        shuntField.setText(String.valueOf(physicalParams.getShunt()));
        turnsBField.setText(String.valueOf(physicalParams.getTurnsB()));
        areaField.setText(String.valueOf(physicalParams.getArea()));

        // Konfiguracja ComboBox z kształtami fal wyjściowych
        waveBox.getItems().addAll("SINUS", "PROSTOKĄT", "TRÓJKĄT", "STAŁA"); // Dostępne kształty fal
        waveBox.setValue("SINUS");                                           // Domyślny kształt - sinus
    }

    private void setupCustomLegend() {
        // Tworzenie prostokąta jako symbolu dla surowych punktów pomiarowych
        Rectangle rawSymbol = new Rectangle(15, 15);                          // Prostokąt 15x15 pikseli
        rawSymbol.setFill(Color.YELLOW);                                      // Wypełnienie żółte
        rawSymbol.setStroke(Color.BLACK);                                     // Obramowanie czarne
        rawSymbol.setStrokeWidth(1);                                          // Grubość obramowania 1px

        Label rawLabel = new Label("Punkty pomiarowe (surowy sygnał)");       // Opis surowych punktów
        rawLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");    // Styl pogrubionej czcionki

        // Tworzenie prostokąta jako symbolu dla uśrednionej pętli histerezy
        Rectangle avgSymbol = new Rectangle(15, 15);
        avgSymbol.setFill(Color.PURPLE);                                      // Wypełnienie fioletowe
        avgSymbol.setStroke(Color.WHITE);                                     // Obramowanie białe
        avgSymbol.setStrokeWidth(1);

        Label avgLabel = new Label("Uśredniona pętla histerezy");             // Opis uśrednionej pętli
        avgLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

        // Dodanie symboli i etykiet do kontenera HBox
        customLegend.getChildren().addAll(rawSymbol, rawLabel, avgSymbol, avgLabel);
        customLegend.setAlignment(Pos.CENTER);                                // Wyśrodkowanie elementów
        customLegend.setSpacing(15);                                          // Odstęp 15 pikseli między elementami
        customLegend.setPadding(new Insets(10));
    }

    private void calculateBufferFromTime() {
        int calculatedBuffer = config.calculateBufferFromTime(); // bufor = czas * częstotliwość

        // Sprawdzenie czy obliczony bufor mieści się w dopuszczalnych granicach
        if (calculatedBuffer < AcquisitionConfig.MIN_BUFFER_SIZE) {
            calculatedBuffer = AcquisitionConfig.MIN_BUFFER_SIZE;            // Ustaw minimum
            acquisitionTimeSpinner.getValueFactory().setValue(              // Korekta czasu
                    (double) AcquisitionConfig.MIN_BUFFER_SIZE / config.getSampleRateHz());
        } else if (calculatedBuffer > AcquisitionConfig.MAX_BUFFER_SIZE) {
            calculatedBuffer = AcquisitionConfig.MAX_BUFFER_SIZE;           // Ustaw maximum
            acquisitionTimeSpinner.getValueFactory().setValue(              // Korekta czasu
                    (double) AcquisitionConfig.MAX_BUFFER_SIZE / config.getSampleRateHz());
        }

        config.setBufferSize(calculatedBuffer);                             // Zapisanie nowego rozmiaru bufora
        bufferSizeSpinner.getValueFactory().setValue(config.getBufferSize()); // Aktualizacja spinnera
        updateInfoLabel();                                                  // Odświeżenie informacji
    }

    private void calculateTimeFromBuffer() {
        // Oblicza czas akwizycji na podstawie rozmiaru bufora i częstotliwości próbkowania
        double time = config.calculateTimeFromBuffer(); // czas = bufor / częstotliwość
        config.setAcquisitionTime(time);                 // Zapisanie nowego czasu
        acquisitionTimeSpinner.getValueFactory().setValue(time); // Aktualizacja spinnera
        updateInfoLabel();                               // Odświeżenie informacji
    }

    private void updateAcquisitionParameters() {
        // Aktualizacja parametrów akwizycji po zmianie częstotliwości próbkowania
        if (config.getSampleRateHz() <= 0) {
            config.setSampleRateHz(1000);                // Zabezpieczenie przed błędną wartością
            sampleRateSpinner.getValueFactory().setValue(1000); // Ustaw domyślną 1000 Hz
        }
        calculateBufferFromTime();                       // Przeliczenie bufora dla nowej częstotliwości
        updateInfoLabel();                               // Odświeżenie informacji
    }

    private void updateInfoLabel() {
        // Aktualizacja etykiety z informacjami o bieżących parametrach akwizycji
        double actualTime = config.calculateTimeFromBuffer(); // Rzeczywisty czas obliczony z bufora
        infoLabel.setText(String.format("Bufor: %d próbek, Czas: %.3fs, Częstotliwość: %d Hz",
                config.getBufferSize(), actualTime, config.getSampleRateHz()));
    }

    @FXML
    private void handleAcquire() {
        // Obsługa przycisku "Acquire" - rozpoczyna akwizycję danych
        updateAcquisitionParameters();                   // Aktualizacja parametrów przed pomiarem

        // Uruchomienie akwizycji w osobnym wątku, aby nie blokować interfejsu użytkownika
        new Thread(() -> {
            try {
                // Akwizycja danych z urządzenia (pomiar napięć na obu kanałach)
                HysteresisData data = acquisitionService.acquire(config);

                // Aktualizacja UI musi być wykonana w wątku JavaFX (Platform.runLater)
                Platform.runLater(() -> {
                    updateStats(data.getCh0Integrated(), data.getCh1Data());        // Obliczenie statystyk
                    updateTimeChart(data.getCh0Integrated(), data.getCh1Data());    // Rysowanie wykresów czasowych
                    updateXYChart(data.getCh0Integrated(), data.getCh1Data());      // Rysowanie wykresu XY
                    drawHysteresisLoop(data.getCh0Integrated(), data.getCh1Data()); // Rysowanie pętli na Canvas
                });
            } catch (Exception e) {
                e.printStackTrace();                   // Obsługa błędów
            }
        }).start();                                    // Start wątku akwizycji
    }

    @FXML
    private void handleQuick100ms() {
        // Ustawienie szybkiego czasu akwizycji 100ms (przydatne do testów)
        acquisitionTimeSpinner.getValueFactory().setValue(0.1);
    }

    @FXML
    private void handleQuick1s() {
        // Ustawienie szybkiego czasu akwizycji 1s
        acquisitionTimeSpinner.getValueFactory().setValue(1.0);
    }

    @FXML
    private void handleQuick5s() {
        // Ustawienie szybkiego czasu akwizycji 5s
        acquisitionTimeSpinner.getValueFactory().setValue(5.0);
    }

    @FXML
    private void handleApplyAndOn() {
        // Obsługa przycisku "Apply & On" - konfiguruje i włącza generator sygnałów
        updatePhysicalParameters();                    // Aktualizacja parametrów fizycznych z pól tekstowych

        // Konfiguracja generatora sygnałów (wyjście analogowe) z wybranymi parametrami
        acquisitionService.configureAnalogOut(
                waveBox.getValue(),
                freqSpinner.getValue(),
                ampSpinner.getValue(),
                offsetSpinner.getValue());

        acquisitionService.enableAnalogOut(true);       // Włączenie wyjścia analogowego
    }

    @FXML
    private void handleOutputOff() {
        // Obsługa przycisku "Output Off" - wyłącza generator sygnałów
        acquisitionService.enableAnalogOut(false);      // Wyłączenie wyjścia analogowego
    }

    private void updatePhysicalParameters() {
        // Aktualizacja parametrów fizycznych na podstawie wartości z pól tekstowych
        try {
            physicalParams.setTurnsExc(Double.parseDouble(turnsExcField.getText()));
            physicalParams.setPathLen(Double.parseDouble(pathLenField.getText()));
            physicalParams.setShunt(Double.parseDouble(shuntField.getText()));
            physicalParams.setTurnsB(Double.parseDouble(turnsBField.getText()));
            physicalParams.setArea(Double.parseDouble(areaField.getText()));
        } catch (NumberFormatException e) {
            // W przypadku błędu konwersji (nieprawidłowy format) zachowujemy poprzednie wartości
        }
    }

    private void updateStats(double[] ch0, double[] ch1) {
        // Obliczenie statystyk dla obu kanałów i aktualizacja etykiet UI
        StatisticsService.Stats stats0 = statisticsService.compute(ch0); // obliczane w statisticsService
        StatisticsService.Stats stats1 = statisticsService.compute(ch1);

        // Aktualizacja etykiet kanału 0
        min0.setText(stats0.getMin());
        max0.setText(stats0.getMax());
        p2p0.setText(stats0.getP2p());
        rms0.setText(stats0.getRms());

        // Aktualizacja etykiet kanału 1
        min1.setText(stats1.getMin());
        max1.setText(stats1.getMax());
        p2p1.setText(stats1.getP2p());
        rms1.setText(stats1.getRms());
    }

    private void updateTimeChart(double[] ch0, double[] ch1) {
        // Aktualizacja wykresów czasowych - redukcja liczby punktów dla wydajności
        timeCh0.getData().clear();  // Wyczyszczenie poprzednich danych
        timeCh1.getData().clear();

        // Obliczenie kroku próbkowania dla wykresu (redukcja do ~DEFAULT_PLOT_POINTS punktów)
        int step = Math.max(1, ch0.length / AcquisitionConfig.DEFAULT_PLOT_POINTS);

        // Dodanie punktów do wykresów z uwzględnieniem kroku próbkowania
        for (int i = 0, p = 0; i < ch0.length; i += step, p++) {
            timeCh0.getData().add(new XYChart.Data<>(p, ch0[i])); // Indeks p jako czas (próbki)
            timeCh1.getData().add(new XYChart.Data<>(p, ch1[i]));
        }
    }

    private void updateXYChart(double[] xSig, double[] ySig) {
        // Aktualizacja wykresu XY (pętla histerezy) z algorytmem uśredniania
        xyRaw.getData().clear();  //usuwanie starych danych
        xyAvg.getData().clear();

        int step = Math.max(1, xSig.length / AcquisitionConfig.DEFAULT_PLOT_POINTS); // Krok redukcji

        // Klasa pomocnicza do przechowywania punktów z informacją o kierunku
        class DataPoint {
            double x, y;  // Współrzędne punktu
            int direction; // Kierunek: +1 (rosnące x), -1 (malejące x)
        }

        List<DataPoint> points = new ArrayList<>();  // Lista wszystkich punktów

        // Przeglądanie sygnałów z detekcją kierunku zmiany
        for (int i = step; i < xSig.length; i += step) {
            double dx = xSig[i] - xSig[i - step];  // Różnica x między kolejnymi punktami
            if (Math.abs(dx) < 1e-9) continue;     // Pomijanie punktów bez zmiany (pionowe linie)

            DataPoint dp = new DataPoint();
            dp.x = xSig[i];                        // Wartość x (proporcjonalna do H)
            dp.y = ySig[i];                        // Wartość y (proporcjonalna do B)
            dp.direction = dx > 0 ? 1 : -1;        // Określenie kierunku (rosnąco/malejąco)
            points.add(dp);                        // Dodanie do listy

            // Dodanie surowego punktu do wykresu
            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(dp.x, dp.y);
            xyRaw.getData().add(dataPoint);
        }

        if (points.isEmpty()) return;  // Brak punktów do przetworzenia

        // Algorytm uśredniania: podział osi x na przedziały (biny)
        int bins = 150;  // Liczba przedziałów dla uśredniania
        double xmin = points.stream().mapToDouble(v -> v.x).min().orElse(0);  // Minimalna wartość x
        double xmax = points.stream().mapToDouble(v -> v.x).max().orElse(1);  // Maksymalna wartość x
        double dx = (xmax - xmin) / (bins - 1);  // Szerokość przedziału

        // Tablice do sumowania wartości y w przedziałach dla obu kierunków
        double[] sumRising = new double[bins], sumFalling = new double[bins];
        int[] countRising = new int[bins], countFalling = new int[bins];

        // Przydzielanie punktów do przedziałów w zależności od kierunku
        for (DataPoint dp : points) {
            int b = (int)((dp.x - xmin) / dx);  // Indeks przedziału
            if (b < 0 || b >= bins) continue;   // Zabezpieczenie przed wyjściem poza zakres
            if (dp.direction > 0) {              // Punkty z rosnącym x (górna gałąź)
                sumRising[b] += dp.y;
                countRising[b]++;
            } else {                             // Punkty z malejącym x (dolna gałąź)
                sumFalling[b] += dp.y;
                countFalling[b]++;
            }
        }

        // Listy dla uśrednionych punktów (dla każdej gałęzi osobno)
        List<Double> risingX = new ArrayList<>();
        List<Double> risingY = new ArrayList<>();
        List<Double> fallingX = new ArrayList<>();
        List<Double> fallingY = new ArrayList<>();

        // Obliczenie uśrednionych wartości y w każdym przedziale
        for (int i = 0; i < bins; i++) {
            double x = xmin + i * dx;  // Środek przedziału na osi x
            if (countRising[i] > 0) {  // Jeśli są punkty w przedziale dla rosnącego kierunku
                risingX.add(x);
                risingY.add(sumRising[i] / countRising[i]);  // Średnia y
                xyAvg.getData().add(new XYChart.Data<>(x, sumRising[i] / countRising[i]));
            }
            if (countFalling[i] > 0) { // Jeśli są punkty w przedziale dla malejącego kierunku
                fallingX.add(x);
                fallingY.add(sumFalling[i] / countFalling[i]); // Średnia y
                xyAvg.getData().add(new XYChart.Data<>(x, sumFalling[i] / countFalling[i]));
            }
        }

        // Aktualizacja stylów punktów na wykresie w wątku JavaFX
        Platform.runLater(() -> {
            // Stylowanie surowych punktów (żółte z czarną obwódką)
            for (XYChart.Data<Number, Number> data : xyRaw.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    node.setStyle("-fx-background-color: yellow, black; " +
                            "-fx-background-insets: 0, 1; " +
                            "-fx-background-radius: 3px; " +
                            "-fx-padding: 3px;");
                }
            }

            // Stylowanie uśrednionych punktów (fioletowe z białą obwódką)
            for (XYChart.Data<Number, Number> data : xyAvg.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    node.setStyle("-fx-background-color: purple, white; " +
                            "-fx-background-insets: 0, 1; " +
                            "-fx-background-radius: 3px; " +
                            "-fx-padding: 3px;");
                }
            }
        });

        // Obliczenie parametrów histerezy z uśrednionych danych
        computeHysteresisStats(risingX, risingY, fallingX, fallingY);
    }

    private void drawHysteresisLoop(double[] xSig, double[] ySig) {
        // Rysowanie pętli histerezy na Canvas (bardziej kontrolowane niż wykres)
        GraphicsContext g = hysteresisCanvas.getGraphicsContext2D(); // Kontekst graficzny
        double w = hysteresisCanvas.getWidth();   // Szerokość Canvas
        double h = hysteresisCanvas.getHeight();  // Wysokość Canvas

        g.clearRect(0, 0, w, h);  // Wyczyszczenie Canvas
        g.setFill(Color.BLACK);    // Tło czarne
        g.fillRect(0, 0, w, h);

        int step = Math.max(1, xSig.length / AcquisitionConfig.DEFAULT_PLOT_POINTS); // Krok redukcji

        class DataPoint {
            double x, y;
            int direction;
        }

        List<DataPoint> points = new ArrayList<>();

        // Analogiczny algorytm jak w updateXYChart, ale dla rysowania na Canvas
        for (int i = step; i < xSig.length; i += step) {
            double dx = xSig[i] - xSig[i - step];
            if (Math.abs(dx) < 1e-9) continue;

            DataPoint dp = new DataPoint();
            dp.x = xSig[i];
            dp.y = ySig[i];
            dp.direction = dx > 0 ? 1 : -1;
            points.add(dp);
        }

        if (points.isEmpty()) return;

        // Uśrednianie jak w updateXYChart
        int bins = 150;
        double xmin = points.stream().mapToDouble(v -> v.x).min().orElse(0);
        double xmax = points.stream().mapToDouble(v -> v.x).max().orElse(1);
        double dx = (xmax - xmin) / (bins - 1);

        double[] sumRising = new double[bins], sumFalling = new double[bins];
        int[] countRising = new int[bins], countFalling = new int[bins];

        for (DataPoint dp : points) {
            int b = (int)((dp.x - xmin) / dx);
            if (b < 0 || b >= bins) continue;
            if (dp.direction > 0) {
                sumRising[b] += dp.y;
                countRising[b]++;
            } else {
                sumFalling[b] += dp.y;
                countFalling[b]++;
            }
        }

        List<Double> risingX = new ArrayList<>();
        List<Double> risingY = new ArrayList<>();
        List<Double> fallingX = new ArrayList<>();
        List<Double> fallingY = new ArrayList<>();

        for (int i = 0; i < bins; i++) {
            double x = xmin + i * dx;
            if (countRising[i] > 0) {
                risingX.add(x);
                risingY.add(sumRising[i] / countRising[i]);
            }
            if (countFalling[i] > 0) {
                fallingX.add(x);
                fallingY.add(sumFalling[i] / countFalling[i]);
            }
        }

        System.out.println("DEBUG Canvas: risingX.size()=" + risingX.size() + ", fallingX.size()=" + fallingX.size());
        System.out.println("DEBUG Canvas: width=" + w + ", height=" + h);

        if (risingX.isEmpty() && fallingX.isEmpty()) {
            System.out.println("DEBUG Canvas: Both lists empty, returning");
            return;
        }

        // Obliczenie zakresów dla skalowania na Canvas
        double canvasXmin = !risingX.isEmpty() && !fallingX.isEmpty() ?
            Math.min(risingX.get(0), fallingX.get(0)) :
            !risingX.isEmpty() ? risingX.get(0) : fallingX.get(0);
        double canvasXmax = !risingX.isEmpty() && !fallingX.isEmpty() ?
            Math.max(risingX.get(risingX.size() - 1), fallingX.get(fallingX.size() - 1)) :
            !risingX.isEmpty() ? risingX.get(risingX.size() - 1) : fallingX.get(fallingX.size() - 1);
        double canvasYmin = Math.min(
                risingY.stream().min(Double::compare).orElse(0.0),
                fallingY.stream().min(Double::compare).orElse(0.0)
        );
        double canvasYmax = Math.max(
                risingY.stream().max(Double::compare).orElse(1.0),
                fallingY.stream().max(Double::compare).orElse(1.0)
        );

        // Współczynniki skalowania (transformacja współrzędnych świata rzeczywistego do pikseli)
        double sx = w / (canvasXmax - canvasXmin);  // Skala dla osi X
        double sy = h / (canvasYmax - canvasYmin);  // Skala dla osi Y

        System.out.println("DEBUG Canvas: xmin=" + canvasXmin + ", xmax=" + canvasXmax);
        System.out.println("DEBUG Canvas: ymin=" + canvasYmin + ", ymax=" + canvasYmax);
        System.out.println("DEBUG Canvas: sx=" + sx + ", sy=" + sy);

        // Rysowanie uśrednionej pętli histerezy
        g.setStroke(Color.PURPLE);  // Kolor fioletowy
        g.setLineWidth(3);          // Grubość linii 3 piksele
        g.beginPath();              // Rozpoczęcie ścieżki

        // Rysowanie górnej gałęzi (rosnące x)
        if (!risingX.isEmpty()) {
            for (int i = 0; i < risingX.size(); i++) {
                double x = (risingX.get(i) - canvasXmin) * sx;      // Transformacja x do pikseli
                double y = h - (risingY.get(i) - canvasYmin) * sy;  // Transformacja y (odwrócenie osi Y)
                if (i == 0) g.moveTo(x, y);  // Pierwszy punkt - przesunięcie bez rysowania
                else g.lineTo(x, y);         // Kolejne punkty - rysowanie linii
            }
        }

        // Rysowanie dolnej gałęzi (malejące x) w odwrotnej kolejności dla zamkniętej pętli
        if (!fallingX.isEmpty()) {
            if (risingX.isEmpty()) {
                g.moveTo((fallingX.get(fallingX.size() - 1) - canvasXmin) * sx,
                        h - (fallingY.get(fallingY.size() - 1) - canvasYmin) * sy);
            }
            for (int i = fallingX.size() - 1; i >= 0; i--) {
                double x = (fallingX.get(i) - canvasXmin) * sx;
                double y = h - (fallingY.get(i) - canvasYmin) * sy;
                g.lineTo(x, y);
            }
        }

        g.closePath();  // Zamknięcie ścieżki (powrót do pierwszego punktu)
        g.stroke();     // Narysowanie ścieżki

        // Rysowanie siatki pomocniczej
        g.setStroke(Color.GRAY);  // Kolor szary
        g.setLineWidth(0.5);      // Cienkie linie

        // Linie poziome
        for (int i = 0; i <= 5; i++) {
            double y = h - (i * h / 5);  // Równomierne rozmieszczenie 5 linii
            g.strokeLine(0, y, w, y);    // Linia przez całą szerokość
        }

        // Linie pionowe
        for (int i = 0; i <= 5; i++) {
            double x = i * w / 5;        // Równomierne rozmieszczenie 5 linii
            g.strokeLine(x, 0, x, h);    // Linia przez całą wysokość
        }

        // Dodanie opisów osi
        g.setFill(Color.WHITE);
        g.setFont(javafx.scene.text.Font.font("Arial", 12));
        g.fillText("B [T]", 10, 20);                   // Oś Y - indukcja magnetyczna
        g.fillText("H [A/m]", w - 50, h - 10);         // Oś X - natężenie pola magnetycznego
    }

    private void computeHysteresisStats(List<Double> rx, List<Double> ry,
                                        List<Double> fx, List<Double> fy) {
        // Obliczenie kluczowych parametrów pętli histerezy
        updatePhysicalParameters();  // Aktualizacja parametrów fizycznych przed obliczeniami

        double hScale = physicalParams.hScale();  // Skalowanie z napięcia do H [A/m]
        double bScale = physicalParams.bScale();  // Skalowanie z napięcia do B [T]

        // Obliczenie nasycenia indukcyjnego (Bsat) - maksymalna wartość |B|
        double bsat = 0;
        for (double y : ry) bsat = Math.max(bsat, Math.abs(y * bScale));  // Dla górnej gałęzi
        for (double y : fy) bsat = Math.max(bsat, Math.abs(y * bScale));  // Dla dolnej gałęzi

        // Obliczenie indukcyjności remanentnej (Br) - wartość B przy H=0
        double br = signalProcessingService.interpolateAtX(rx, ry, 0) * bScale;

        // Obliczenie koercji (Hc) - wartość H przy B=0
        double hc = signalProcessingService.interpolateAtY(rx, ry, 0) * hScale;

        // Aktualizacja etykiet UI z zaokrągleniem do 4 miejsc po przecinku
        bsatLabel.setText(String.format("%.4f", bsat));  // Indukcyjność nasycenia [T]
        brLabel.setText(String.format("%.4f", br));      // Indukcyjność remanentna [T]
        hcLabel.setText(String.format("%.4f", hc));      // Koercja [A/m]
    }

    public void shutdown() {
        // Metoda wywoływana przy zamykaniu aplikacji - sprzątanie zasobów
        acquisitionService.reset();       // Zatrzymanie akwizycji i wyłączenie generatora
        dwf.FDwfDeviceCloseAll();         // Zamknięcie połączenia z urządzeniem DWF
    }
}