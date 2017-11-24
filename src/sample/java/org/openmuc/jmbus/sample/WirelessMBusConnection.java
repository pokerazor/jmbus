package org.openmuc.jmbus.sample;

import java.io.IOException;

import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.wireless.WMBusConnection;
import org.openmuc.jmbus.wireless.WMBusConnection.WMBusSerialBuilder;
import org.openmuc.jmbus.wireless.WMBusConnection.WMBusSerialBuilder.WMBusManufacturer;
import org.openmuc.jmbus.wireless.WMBusListener;
import org.openmuc.jmbus.wireless.WMBusMessage;
import org.openmuc.jmbus.wireless.WMBusMode;

public class WirelessMBusConnection {

    public static class MyWMBusListener implements WMBusListener {

        @Override
        public void newMessage(WMBusMessage message) {
            // TODO Auto-generated method stub

        }

        @Override
        public void discardedBytes(byte[] bytes) {
            // TODO Auto-generated method stub

        }

        @Override
        public void stoppedListening(IOException cause) {
            // TODO Auto-generated method stub

        }

    }

    public static void newConnection() throws IOException {
        // TODO set these values
        SecondaryAddress address = null;
        byte[] key = null;

        // tag::todoc[]
        WMBusManufacturer wmBusManufacturer = WMBusManufacturer.AMBER;
        WMBusListener listener = new MyWMBusListener();
        String serialPortName = "/dev/ttyUSB0";
        WMBusSerialBuilder builder = new WMBusSerialBuilder(wmBusManufacturer, listener, serialPortName)
                .setMode(WMBusMode.S);

        try (WMBusConnection wmBusConnection = builder.build()) {
            wmBusConnection.addKey(address, key);
        }

        // end::todoc[]
    }

}
