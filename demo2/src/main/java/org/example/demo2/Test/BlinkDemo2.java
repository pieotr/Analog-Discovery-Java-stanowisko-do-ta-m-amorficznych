package org.example.demo2.Test;
import org.knowm.waveforms4j.DWF;

public class BlinkDemo2 {
    public static void main(String[] args) throws InterruptedException {

        DWF dwf = new DWF();

        boolean successful = dwf.FDwfDeviceOpen();
        System.out.println("successful: " + successful);
        //dwf.openDevice(0);              // open first AD2

        int ledMask = 1 << 0;   // DIO0

        // set DIO0 as output
        dwf.FDwfDigitalIOOutputEnableSet(ledMask);

        // HIGH 1 s
        dwf.FDwfDigitalIOOutputSet(ledMask);
        Thread.sleep(10000);

        // LOW 4 s
        dwf.FDwfDigitalIOOutputSet( 0);
        Thread.sleep(4000);

        // close device
        dwf.FDwfDeviceCloseAll();
}
}
