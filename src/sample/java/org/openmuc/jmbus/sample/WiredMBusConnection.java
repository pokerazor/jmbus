package org.openmuc.jmbus.sample;

import java.io.IOException;

import org.openmuc.jmbus.MBusConnection;
import org.openmuc.jmbus.MBusConnection.MBusSerialBuilder;
import org.openmuc.jmbus.VariableDataStructure;

public class WiredMBusConnection {

    public static void newConnection() throws IOException {
        // tag::todoc[]
        MBusSerialBuilder builder = MBusConnection.newSerialBuilder("/dev/ttyS0").setBaudrate(2400);
        try (MBusConnection mBusConnection = builder.build()) {
            // read/write
        }
        // end::todoc[]
    }

    @SuppressWarnings("unused")
    public static void read() throws IOException {
        MBusSerialBuilder builder = MBusConnection.newSerialBuilder("/dev/ttyS0").setBaudrate(2400);
        try (MBusConnection mBusConnection = builder.build()) {
            // tag::readtodoc[]
            int primaryAddress = 1;
            VariableDataStructure vds = mBusConnection.read(primaryAddress);
            // do something with the vds
            // end::readtodoc[]
        }
    }

    public static void write() throws IOException {
        MBusSerialBuilder builder = MBusConnection.newSerialBuilder("/dev/ttyS0").setBaudrate(2400);
        try (MBusConnection mBusConnection = builder.build()) {
            // tag::writetodoc[]
            int primaryAddress = 5;
            byte[] data = { 0x01, 0x7a, 0x09 };
            mBusConnection.write(primaryAddress, data);
            // end::writetodoc[]
        }
    }
}
