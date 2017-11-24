/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.SerialPortBuilder;
import org.openmuc.jrxtx.StopBits;

/**
 * Was tested with the Amber 8426M Wireless M-Bus stick.
 * 
 * @author Stefan Feuerhahn
 *
 */
public class WMBusSapAmber extends AbstractWMBusSap {

    private MessageReceiver receiver;
    private final String serialPortName;

    private class MessageReceiver extends Thread {

        private static final int MBUS_BL_CONTROL = 0x44;
        private static final int MESSAGE_FRAGEMENT_TIMEOUT = 500;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();

        private int discardCount = 0;

        @Override
        public void run() {

            try {

                while (!closed) {
                    task();
                }

            } catch (final Exception e) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        listener.stoppedListening(new IOException(e));
                    }
                });

            } finally {
                close();
                executor.shutdown();
            }

        }

        private void task() throws IOException {

            ByteBuffer discardBuffer = ByteBuffer.allocate(100);

            int b0, b1;
            while (true) {
                serialPort.setSerialPortTimeout(0);
                b0 = is.read();
                serialPort.setSerialPortTimeout(MESSAGE_FRAGEMENT_TIMEOUT);
                b1 = is.read();

                if ((b1 ^ MBUS_BL_CONTROL) == 0) {
                    break;
                }

                if (discardBuffer.capacity() - discardBuffer.position() < 2) {
                    discard(discardBuffer.array(), 0, discardBuffer.position());
                    discardBuffer.clear();
                }
                discardBuffer.put((byte) b0);
                discardBuffer.put((byte) b1);
            }

            int len = (b0 & 0xff) + 1;
            byte[] data = new byte[2 + len];

            data[0] = (byte) b0;
            data[1] = (byte) b1;

            is.read(data, 2, len - 2);

            notifyListener(data);

            if (discardBuffer.position() > 0) {
                discard(discardBuffer.array(), 0, discardBuffer.position());
            }
        }

        private void notifyListener(final byte[] data) {
            int rssi = data[data.length - 1] & 0xff;
            final Integer signalStrengthInDBm;
            int rssiOffset = 74;
            if (rssi >= 128) {
                signalStrengthInDBm = ((rssi - 256) / 2) - rssiOffset;
            }
            else {
                signalStrengthInDBm = (rssi / 2) - rssiOffset;
            }

            data[0] = (byte) (data[0] - 1);

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.newMessage(new WMBusMessage(data, signalStrengthInDBm, keyMap));
                }
            });
        }

        private void discard(byte[] data, int offset, int length) {
            discardCount++;
            final byte[] discardedBytes = Arrays.copyOfRange(data, offset, offset + length);

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.discardedBytes(discardedBytes);
                }
            });

            if (discardCount >= 5) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        reset();
                    }
                });
                discardCount = 0;
            }
        }

    }

    public WMBusSapAmber(String serialPortName, WMBusMode mode, WMBusListener listener) {
        super(mode, listener);
        this.serialPortName = serialPortName;

    }

    @Override
    public void open() throws IOException {
        if (!closed) {
            return;
        }
        this.serialPort = SerialPortBuilder.newBuilder(this.serialPortName)
                .setBaudRate(9600)
                .setDataBits(DataBits.DATABITS_8)
                .setStopBits(StopBits.STOPBITS_1)
                .setParity(Parity.NONE)
                .build();

        os = new DataOutputStream(serialPort.getOutputStream());
        is = new DataInputStream(serialPort.getInputStream());
        initializeWirelessTransceiver(mode);
        receiver = new MessageReceiver();
        closed = false;
        receiver.start();
    }

    /**
     * @param mode
     *            - the wMBus mode to be used for transmission
     * @throws IOException
     */
    private void initializeWirelessTransceiver(WMBusMode mode) throws IOException {
        switch (mode) {
        case S:
            amberSetReg((byte) 0x46, (byte) 0x03);
            break;
        case T:
            amberSetReg((byte) 0x46, (byte) 0x08); // T2-OTHER (correct for receiving station in T mode)
            break;
        default:
            throw new IOException("wMBUS Mode '" + mode.toString() + "' is not supported");
        }
        amberSetReg((byte) 0x45, (byte) 0x01); // Enable attaching RSSI to message
    }

    /**
     * writes a "CMD_SET_REQ" to the Amber module
     * 
     * @param cmd
     *            - register address of the Amber module
     * @param data
     *            - new value(s) for this register address(es)
     * @return - true=success, false=failure
     */
    private boolean writeCommand(byte cmd, byte[] data) {
        outputBuffer[0] = (byte) 0xff;
        outputBuffer[1] = cmd;
        outputBuffer[2] = (byte) data.length;

        System.arraycopy(data, 0, outputBuffer, 3, data.length);

        final int len = 3 + data.length;

        byte checksum = 0;
        for (int i = 0; i < len; i++) {
            checksum = (byte) (checksum ^ outputBuffer[i]);
        }

        outputBuffer[len] = checksum;

        try {
            os.write(outputBuffer, 0, data.length + 4);
            os.flush();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    private boolean amberSetReg(byte reg, byte value) {
        byte[] data = { reg, 0x01, value };

        writeCommand((byte) 0x09, data);

        try {
            serialPort.setSerialPortTimeout(500);
            is.read(inputBuffer);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Writes a reset command to the Amber module
     */
    public void reset() {
        writeCommand((byte) 0x05, new byte[] {});
    }

}
