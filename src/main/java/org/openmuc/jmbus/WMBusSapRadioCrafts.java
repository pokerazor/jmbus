/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.SerialPortBuilder;
import org.openmuc.jrxtx.StopBits;

/**
 * 
 * @author Stefan Feuerhahn
 * 
 */
public class WMBusSapRadioCrafts extends AbstractWMBusSap {

    /**
     * Radio Craft W-MBUS frame:
     * @formatter:off
     * +---+----+-----------+
     * | L | CI | APPL_DATA |
     * +---+----+-----------+
     *  ~ L is the length (not including the length byte itself)
     *  ~ CI is the Control Information byte
     * @formatter:on
     */

    private static final int ACK = 0x3E;
    private final String serialPortName;
    private final ExecutorService receiverService;

    private static final int FRAGMENT_TIMEOUT = 500;

    private class MessageReceiver implements Runnable {

        /**
         * Indicates message from primary station, function send/no reply (SND -N
         */
        private static final byte CONTROL_BYTE = 0x44;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();

        @Override
        public void run() {
            try {

                byte[] discardBuffer = new byte[BUFFER_LENGTH];
                int bufferPointer = 0;
                while (!closed) {

                    byte b0, b1;
                    while (true) {
                        serialPort.setSerialPortTimeout(0);
                        b0 = is.readByte();
                        serialPort.setSerialPortTimeout(FRAGMENT_TIMEOUT);

                        try {
                            // this may time out.
                            b1 = is.readByte();
                        } catch (InterruptedIOException e) {
                            continue;
                        }

                        if (b1 == CONTROL_BYTE) {
                            break;
                        }

                        discardBuffer[bufferPointer++] = b0;
                        discardBuffer[bufferPointer++] = b1;

                        if (bufferPointer - 2 >= discardBuffer.length) {
                            discard(discardBuffer, 0, bufferPointer);
                            bufferPointer = 0;
                        }

                    }

                    int messageLength = b0 & 0xff;

                    final byte[] messageData = new byte[messageLength + 1];

                    messageData[0] = b0;
                    messageData[1] = b1;

                    int len = messageData.length - 2;
                    try {
                        int numReadBytes = is.read(messageData, 2, len);

                        if (len == numReadBytes) {
                            notifyListener(messageData);
                        }
                        else {
                            discard(messageData, 0, numReadBytes + 2);
                        }
                    } catch (InterruptedIOException e) {
                        discard(messageData, 0, 2);
                    }

                }
            } catch (final IOException e) {
                if (!closed) {
                    executor.execute(new Runnable() {

                        @Override
                        public void run() {
                            Thread.currentThread().setName("Notify stopped listening thread.");
                            listener.stoppedListening(new IOException(e));
                        }
                    });
                }

            } finally {
                close();
                executor.shutdown();
            }

        }

        private void notifyListener(final byte[] messageBytes) {
            messageBytes[0] = (byte) (messageBytes[0] - 1);
            int rssi = messageBytes[messageBytes.length - 1] & 0xff;

            final int signalStrengthInDBm = (rssi * -1) / 2;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.newMessage(new WMBusMessage(messageBytes, signalStrengthInDBm, keyMap));
                }
            });
        }

        private void discard(byte[] buffer, int offset, int length) {
            final byte[] discardedBytes = Arrays.copyOfRange(buffer, offset, offset + length);

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.discardedBytes(discardedBytes);
                }
            });
        }

    }

    public WMBusSapRadioCrafts(String serialPortName, WMBusMode mode, WMBusListener listener) {
        super(mode, listener);
        this.serialPortName = serialPortName;
        this.receiverService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void open() throws IOException {
        if (!closed) {
            return;
        }
        this.serialPort = SerialPortBuilder.newBuilder(this.serialPortName)
                .setBaudRate(19200)
                .setDataBits(DataBits.DATABITS_8)
                .setStopBits(StopBits.STOPBITS_1)
                .setParity(Parity.NONE)
                .build();

        this.os = new DataOutputStream(serialPort.getOutputStream());
        this.is = new DataInputStream(serialPort.getInputStream());

        initializeWirelessTransceiver(mode);
        this.closed = false;

        this.receiverService.execute(new MessageReceiver());
    }

    @Override
    public void close() {
        super.close();
        receiverService.shutdown();
    }

    /**
     * @param mode
     *            - the wMBus mode to be used for transmission
     * @throws IOException
     */
    private void initializeWirelessTransceiver(WMBusMode mode) throws IOException {
        // enter config mode
        sendByteInConfigMode(0x00);

        switch (mode) {
        case S:

            /* Set S mode */
            sendByteInConfigMode(0x4d);
            os.write(0x03);
            os.write(0x00);
            sendByteInConfigMode(0xff);

            /* Set master mode */
            sendByteInConfigMode(0x4d);
            os.write(0x12);
            os.write(0x01);
            sendByteInConfigMode(0xff);

            /* Get RSSI information with corresponding message */
            sendByteInConfigMode(0x4d);
            os.write(0x05);
            os.write(0x01);
            sendByteInConfigMode(0xff);

            // /* Set Auto Answer Register */
            // sendByteInConfigMode(0x41);
            // sendByteInConfigMode(0xff);
            break;
        case T:
            /* Set T2 mode */
            sendByteInConfigMode(0x4d);
            os.write(0x03);
            os.write(0x02);
            sendByteInConfigMode(0xff);

            /* Set master mode */
            sendByteInConfigMode(0x4d);
            os.write(0x12);
            os.write(0x01);
            sendByteInConfigMode(0xff);

            /* Get RSSI information with corresponding message */
            sendByteInConfigMode(0x4d);
            os.write(0x05);
            os.write(0x01);
            sendByteInConfigMode(0xff);

            // /* Set Auto Answer Register */
            // sendByteInConfigMode(0x41);
            // sendByteInConfigMode(0xff);
            break;
        default:
            throw new IOException("wMBUS Mode '" + mode.toString() + "' is not supported");
        }

        // leave config mode
        os.write(0x58);

    }

    private void sendByteInConfigMode(int b) throws IOException {

        discardNoise();

        os.write(b);
        os.flush();

        serialPort.setSerialPortTimeout(FRAGMENT_TIMEOUT);

        if (is.read() != ACK) {
            throw new IOException("sendByteInConfigMode failed");
        }

    }

    private void discardNoise() throws IOException {
        if (is.available() > 0) {
            is.read(inputBuffer);
        }
    }

}
