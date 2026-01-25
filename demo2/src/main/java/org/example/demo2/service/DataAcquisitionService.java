package org.example.demo2.service;

import org.example.demo2.model.AcquisitionConfig;
import org.example.demo2.model.HysteresisData;
import org.knowm.waveforms4j.DWF;

/**
 * Service for data acquisition from DWF device.
 */
public class DataAcquisitionService {
    private static final int IN_CH0 = 0;
    private static final int IN_CH1 = 1;
    private static final int OUT_CH0 = 0;

    private final DWF dwf;
    private final SignalProcessingService signalProcessingService;
    private volatile boolean acquiring = false;

    public DataAcquisitionService(DWF dwf) {
        this.dwf = dwf;
        this.signalProcessingService = new SignalProcessingService();

        boolean successful = dwf.FDwfDeviceOpen();  // sprawdzenie czy udało się połączyć z analog discovery
        System.out.println("successful: " + successful); // wypisanie tego w konsoli

    }

    public boolean isAcquiring() {
        return acquiring;
    }

    public void configureAnalogIn(AcquisitionConfig config) {
        dwf.FDwfAnalogInChannelEnableSet(IN_CH0, true);
        dwf.FDwfAnalogInChannelEnableSet(IN_CH1, true);
        dwf.FDwfAnalogInChannelRangeSet(IN_CH0, config.getInputRangeV());
        dwf.FDwfAnalogInChannelRangeSet(IN_CH1, config.getInputRangeV());
        dwf.FDwfAnalogInFrequencySet(config.getSampleRateHz());
        dwf.FDwfAnalogInBufferSizeSet(config.getBufferSize());
    }

    public HysteresisData acquire(AcquisitionConfig config) throws Exception {
        if (acquiring) {
            throw new IllegalStateException("Acquisition already in progress");
        }

        acquiring = true;
        try {
            configureAnalogIn(config);

            double[] ch0 = new double[config.getBufferSize()];
            double[] ch1 = new double[config.getBufferSize()];
            int collected = 0;

            dwf.FDwfAnalogInConfigure(false, true);

            while (collected < config.getBufferSize()) {
                dwf.FDwfAnalogInStatus(true);
                int n = dwf.FDwfAnalogInStatusSamplesValid();
                if (n <= 0) continue;

                int r = Math.min(n, config.getBufferSize() - collected);

                System.arraycopy(dwf.FDwfAnalogInStatusData(IN_CH0, r), 0, ch0, collected, r);
                System.arraycopy(dwf.FDwfAnalogInStatusData(IN_CH1, r), 0, ch1, collected, r);

                collected += r;
            }

            double[] ch0Int = signalProcessingService.integrate(ch0, config.getSampleRateHz());

            return new HysteresisData(ch0, ch1, ch0Int);
        } finally {
            acquiring = false;
        }
    }

    public void configureAnalogOut(String wave, double frequency, double amplitude, double offset) {
        int func = switch (wave) {
            case "PROSTOKĄT" -> 2;
            case "TRÓJKĄT" -> 3;
            case "STAŁA" -> 4;
            default -> 1; // SINUS
        };

        dwf.FDwfAnalogOutNodeEnableSet(OUT_CH0, true);
        dwf.FDwfAnalogOutNodeFunctionSet(OUT_CH0, func);
        dwf.FDwfAnalogOutNodeFrequencySet(OUT_CH0, frequency);
        dwf.FDwfAnalogOutNodeAmplitudeSet(OUT_CH0, amplitude);
        dwf.FDwfAnalogOutNodeOffsetSet(OUT_CH0, offset);
    }

    public void enableAnalogOut(boolean enable) {
        dwf.FDwfAnalogOutConfigure(OUT_CH0, enable);
    }

    public void reset() {
        dwf.FDwfAnalogOutConfigure(OUT_CH0, false);
        dwf.FDwfDigitalOutReset();
    }
}
