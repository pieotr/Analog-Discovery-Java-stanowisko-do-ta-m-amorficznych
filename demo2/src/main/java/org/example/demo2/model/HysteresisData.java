package org.example.demo2.model;

/**
 * KLASA PRZECHOWUJĄCA DANE POMIAROWE PĘTLI HISTEREZY
 *
 * dane są przygotowane przez signal processing serice zanim  tu są przechowane. to tylko kontener na nie
 */
public class HysteresisData {

    /**
     * SUROWE DANE Z KANAŁU CH0 - CEWKA POMIAROWA
     */
    private final double[] ch0Data;

    /**
     * SUROWE DANE Z KANAŁU CH1 - POMIAR PRĄDU (SHUNT)
     */
    private final double[] ch1Data;

    /**
     * SCAŁKOWANE DANE Z KANAŁU CH0 - INDUKCJA MAGNETYCZNA
     */
    private final double[] ch0Integrated;


    public HysteresisData(double[] ch0Data, double[] ch1Data, double[] ch0Integrated) {
        // Zapisanie referencji do tablic
        this.ch0Data = ch0Data;
        this.ch1Data = ch1Data;
        this.ch0Integrated = ch0Integrated;
    }

    public double[] getCh0Data() {
        return ch0Data;
    }

    public double[] getCh1Data() {
        return ch1Data;
    }

    public double[] getCh0Integrated() {
        return ch0Integrated;
    }
}
