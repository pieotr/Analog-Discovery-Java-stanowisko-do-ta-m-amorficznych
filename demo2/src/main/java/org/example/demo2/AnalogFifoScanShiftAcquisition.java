package org.example.demo2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.example.demo2.controller.MainController;  // Kontroler odpowiedzialny za logikę UI

/**
 * GŁÓWNA KLASA APLIKACJI DO POMIARU PĘTLI HISTEREZY B-H MATERIAŁÓW MAGNETYCZNYCH
 *
 *
 * ARCHITEKTURA APLIKACJI:
 * ======================
 * Aplikacja wykorzystuje wzorzec MVC (Model-View-Controller) z FXML:
 * - Model: klasy przechowujące dane (AcquisitionConfig, PhysicalParameters, HysteresisData)
 * - View: plik FXML (main-view.fxml) definiujący wygląd UI
 * - Controller: MainController łączący model z widokiem
 * - Service: klasy pomocnicze (DataAcquisitionService, SignalProcessingService, StatisticsService)
 */
public class AnalogFifoScanShiftAcquisition extends Application {

    // Referencja do kontrolera głównego okna - potrzebna do prawidłowego zamknięcia aplikacji
    // Controller zarządza wszystkimi elementami UI i logiką pomiarową
    private MainController controller;

    /**
     * METODA START - PUNKT WEJŚCIA APLIKACJI JAVAFX
     *
     * Wywoływana automatycznie przez framework JavaFX po uruchomieniu aplikacji.
     */
    @Override
    public void start(Stage stage) throws Exception {
        // Tworzenie loadera FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main-view.fxml"));

        // Ładowanie FXML i tworzenie sceny
        Scene scene = new Scene(loader.load());

        // Potrzebujemy tej referencji aby móc wywołać metodę shutdown() przy zamykaniu
        controller = loader.getController();

        stage.setScene(scene);

        stage.setTitle("Pomiar Histerezy Magnetycznej B-H - Konfigurowalna akwizycja");

        stage.show();
    }

    /**
     * METODA STOP - CZYSZCZENIE ZASOBÓW PRZY ZAMYKANIU APLIKACJI
     *
     * Wywoływana automatycznie przez JavaFX gdy użytkownik zamyka okno lub wywołuje Platform.exit()
     *
     * WAŻNE DLA URZĄDZEŃ POMIAROWYCH:
     * ===============================
     * Prawidłowe zamknięcie jest krytyczne dla urządzeń DWF (Digilent Waveforms):
     * - Wyłączenie generatora sygnału (analogOut) - zapobiega ciągłemu wzbudzaniu cewki
     * - Reset wyjść cyfrowych - przywraca stan początkowy
     * - Zamknięcie komunikacji USB - zwalnia urządzenie dla innych aplikacji
     *
     * Brak prawidłowego zamknięcia może:
     * - Pozostawić cewkę wzbudzającą pod napięciem (przegrzanie!)
     * - Zablokować urządzenie dla kolejnych uruchomień
     */
    @Override
    public void stop() {
        // Sprawdzenie czy kontroler został poprawnie zainicjalizowany
        // (zabezpieczenie przed błędem gdy aplikacja jest zamykana przed pełnym startem)
        if (controller != null) {
            // Wywołanie metody shutdown() która:
            // 1. Wyłącza wyjście analogowe (generator sygnału)
            // 2. Resetuje wyjścia cyfrowe
            // 3. Zamyka połączenie z urządzeniem DWF
            controller.shutdown();
        }
    }

    /**
     * METODA MAIN - PUNKT WEJŚCIA PROGRAMU
     */
    public static void main(String[] args) {
        launch(args);
    }
}