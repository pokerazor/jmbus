/*
 * Copyright 2010-16 Fraunhofer ISE
 *
 * This file is part of jMBus.
 * For more information visit http://www.openmuc.org
 *
 * jMBus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jMBus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jMBus.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.jmbus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.openmuc.jmbus.MBusMessage.MessageType;

import gnu.io.SerialPort;

/**
 * M-Bus Application Layer Service Access Point - Use this access point to communicate using the M-Bus wired protocol.
 * 
 * @author Stefan Feuerhahn
 * 
 */
public class MBusSap {

    // 261 is the maximum size of a long frame
    private final static int MAX_MESSAGE_SIZE = 261;

    private final SerialTransceiver serialTransceiver;

    private final byte[] outputBuffer = new byte[MAX_MESSAGE_SIZE];

    private final byte[] dataRecordsAsBytes = new byte[MAX_MESSAGE_SIZE];

    private final boolean[] frameCountBits;

    private DataOutputStream os = null;
    private DataInputStream is = null;

    private int timeout = 500;
    private SecondaryAddress secondaryAddress = null;

    /**
     * Creates an M-Bus Service Access Point that is used to read meters.
     * 
     * @param serialPortName
     *            examples for serial port identifiers are on Linux "/dev/ttyS0" or "/dev/ttyUSB0" and on Windows "COM1"
     * @param baudRate
     *            the baud rate to use.
     */
    public MBusSap(String serialPortName, int baudRate) {
        serialTransceiver = new SerialTransceiver(serialPortName, baudRate, SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
        frameCountBits = new boolean[254];
        for (int i = 0; i < frameCountBits.length; i++) {
            frameCountBits[i] = true;
        }
    }

    /**
     * Opens the serial port. The serial port needs to be opened before attempting to read a device.
     * 
     * @throws IOException
     *             if any kind of error occurs opening the serial port.
     */
    public void open() throws IOException {
        serialTransceiver.open();
        os = serialTransceiver.getOutputStream();
        is = serialTransceiver.getInputStream();
    }

    /**
     * Closes the serial port.
     */
    public void close() {
        serialTransceiver.close();
    }

    /**
     * Sets the maximum time in ms to wait for new data from the remote device.
     * 
     * @param timeout
     *            the maximum time in ms to wait for new data. Must be greater then 0.
     */
    public void setTimeout(int timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout may not be 0");
        }
        this.timeout = timeout;
    }

    /**
     * Returns the timeout in ms.
     * 
     * @return the timeout in ms.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Reads a meter using primary addressing. Sends a data request (REQ_UD2) to the remote device and returns the
     * variable data structure from the received RSP_UD frame.
     * 
     * @param primaryAddress
     *            the primary address of the meter to read. For secondary address use 0xfd.
     * @return the variable data structure from the received RSP_UD frame
     * @throws IOException
     *             if any kind of error (including timeout) occurs while trying to read the remote device. Note that the
     *             connection is not closed when an IOException is thrown.
     * @throws TimeoutException
     *             if no response at all (not even a single byte) was received from the meter within the timeout span.
     */
    public VariableDataStructure read(int primaryAddress) throws IOException, TimeoutException {

        if (serialTransceiver.isClosed() == true) {
            throw new IllegalStateException("Serial port is not open.");
        }

        if (frameCountBits[primaryAddress]) {
            sendShortMessage(primaryAddress, 0x7b);
            frameCountBits[primaryAddress] = false;
        }
        else {
            sendShortMessage(primaryAddress, 0x5b);
            frameCountBits[primaryAddress] = true;
        }

        MBusMessage mBusMessage = receiveMessage();

        if (mBusMessage.getMessageType() != MessageType.RSP_UD) {
            throw new IOException(
                    "Received wrong kind of message. Expected RSP_UD but got: " + mBusMessage.getMessageType());
        }

        if (mBusMessage.getAddressField() != primaryAddress) {
            // throw new IOException("Received RSP_UD message with unexpected address field. Expected " + primaryAddress
            // + " but received " + mBusMessage.getAddressField());
        }

        try {
            mBusMessage.getVariableDataResponse().decode();
        } catch (DecodingException e) {
            throw new IOException("Error decoding incoming RSP_UD message.", e);
        }

        return mBusMessage.getVariableDataResponse();

    }

    /**
     * Writes to a meter using primary addressing. Sends a data send (SND_UD) to the remote device and returns a true if
     * slave sends a 0x7e else false
     * 
     * @param primaryAddress
     *            the primary address of the meter to write. For secondary address use 0xfd.
     * @param data
     *            the data to sends to the meter.
     * @return true if data could be sent else false
     * @throws IOException
     *             if any kind of error (including timeout) occurs while trying to read the remote device. Note that the
     *             connection is not closed when an IOException is thrown.
     * @throws TimeoutException
     *             if no response at all (not even a single byte) was received from the meter within the timeout span.
     */
    public boolean write(int primaryAddress, byte[] data) throws IOException, TimeoutException {

        boolean ret;
        if (data == null) {
            data = new byte[] {};
        }
        ret = sendLongMessage(primaryAddress, 0x73, 0x51, data.length, data);
        MBusMessage mBusMessage = receiveMessage();

        if (mBusMessage.getMessageType() != MessageType.SINGLE_CHARACTER) {
            throw new IOException("unable to select component");
        }
        return ret;
    }

    /**
     * [alpha]<br>
     * Scans if any device response to the given wildcard.
     * 
     * @param wildcard
     *            secondary address wildcard e.g. f1ffffffffffffff
     * @return true ifany device responsed else false
     */
    public boolean scanSelection(SecondaryAddress wildcard) {

        ByteBuffer bf = ByteBuffer.allocate(8);
        byte[] ba = new byte[8];

        bf.order(ByteOrder.LITTLE_ENDIAN);

        bf.put(wildcard.asByteArray());

        bf.position(0);
        bf.get(ba, 0, 8);

        boolean ret = false;
        try {
            if (sendLongMessage(0xfd, 0x53, 0x52, 8, ba)) {

                MBusMessage mBusMessage = receiveMessage();

                if (mBusMessage.getMessageType() == MessageType.SINGLE_CHARACTER) {
                    ret = true;
                }
            }
        } catch (IOException e) {
            ret = true;
        } catch (TimeoutException e) {
            ret = false;
        }
        return ret;
    }

    /**
     * Selects the meter with the specified secondary address. After this the meter can be read on primary address 0xfd.
     * 
     * @param secondaryAddress
     *            the secondary address of the meter to select.
     * @throws IOException
     *             if any kind of error (including timeout) occurs while trying to read the remote device. Note that the
     *             connection is not closed when an IOException is thrown.
     * @throws TimeoutException
     *             if no response at all (not even a single byte) was received from the meter within the timeout span.
     */
    public void selectComponent(SecondaryAddress secondaryAddress) throws IOException, TimeoutException {
        this.secondaryAddress = secondaryAddress;
        componentSelection(false);
    }

    /**
     * Deselects the previously selected meter.
     * 
     * @throws IOException
     *             if any kind of error (including timeout) occurs while trying to read the remote device. Note that the
     *             connection is not closed when an IOException is thrown.
     * @throws TimeoutException
     *             if no response at all (not even a single byte) was received from the meter within the timeout span.
     */
    public void deselectComponent() throws IOException, TimeoutException {
        if (secondaryAddress != null) {
            componentSelection(true);
            secondaryAddress = null;
        }
    }

    public void selectForReadout(int primaryAddress, List<DataRecord> dataRecords)
            throws IOException, TimeoutException {

        int i = 0;
        for (DataRecord dataRecord : dataRecords) {
            i += dataRecord.encode(dataRecordsAsBytes, i);
        }
        sendLongMessage(primaryAddress, 0x53, 0x51, i, dataRecordsAsBytes);
        MBusMessage mBusMessage = receiveMessage();

        if (mBusMessage.getMessageType() != MessageType.SINGLE_CHARACTER) {
            throw new IOException("unable to select component");
        }
    }

    public void resetReadout(int primaryAddress) throws IOException, TimeoutException {
        sendLongMessage(primaryAddress, 0x53, 0x50, 0, new byte[] {});
        MBusMessage mBusMessage = receiveMessage();

        if (mBusMessage.getMessageType() != MessageType.SINGLE_CHARACTER) {
            throw new IOException("unable to select component");
        }
    }

    /**
     * Sends a SND_NKE message to reset the FCB (frame counter bit).
     * 
     * @param primaryAddress
     *            the primary address of the meter to reset.
     * @throws IOException
     *             if an error occurs during the reset process.
     * @throws TimeoutException
     *             if the slave does not answer with an 0xe5 message within the configured timeout span.
     */
    public void linkReset(int primaryAddress) throws IOException, TimeoutException {
        sendShortMessage(primaryAddress, 0x40);
        MBusMessage mBusMessage = receiveMessage();

        if (mBusMessage.getMessageType() != MessageType.SINGLE_CHARACTER) {
            throw new IOException("unable to reset link");
        }

        frameCountBits[primaryAddress] = true;
    }

    private void componentSelection(boolean deselect) throws IOException, TimeoutException {
        ByteBuffer bf = ByteBuffer.allocate(8);
        byte[] ba = new byte[8];

        bf.order(ByteOrder.LITTLE_ENDIAN);

        bf.put(secondaryAddress.asByteArray());

        bf.position(0);
        bf.get(ba, 0, 8);

        // send select/deselect
        if (deselect) {
            sendLongMessage(0xfd, 0x53, 0x56, 8, ba);
        }
        else {
            sendLongMessage(0xfd, 0x53, 0x52, 8, ba);
        }

        MBusMessage mBusMessage = receiveMessage();

        if (mBusMessage.getMessageType() != MessageType.SINGLE_CHARACTER) {
            throw new IOException("unable to select component");
        }
    }

    private void sendShortMessage(int slaveAddr, int cmd) throws IOException {
        outputBuffer[0] = 0x10;
        outputBuffer[1] = (byte) (cmd);
        outputBuffer[2] = (byte) (slaveAddr);
        outputBuffer[3] = (byte) (cmd + slaveAddr);
        outputBuffer[4] = 0x16;
        os.write(outputBuffer, 0, 5);
    }

    private boolean sendLongMessage(int slaveAddr, int controlField, int ci, int length, byte[] data) {
        int i, j;
        int checksum = 0;

        outputBuffer[0] = 0x68;
        outputBuffer[1] = (byte) (length + 3);
        outputBuffer[2] = (byte) (length + 3);
        outputBuffer[3] = 0x68;
        outputBuffer[4] = (byte) controlField;
        outputBuffer[5] = (byte) slaveAddr;
        outputBuffer[6] = (byte) ci;

        for (i = 0; i < length; i++) {
            outputBuffer[7 + i] = data[i];
        }

        for (j = 4; j < (i + 7); j++) {
            checksum += outputBuffer[j];
        }

        outputBuffer[i + 7] = (byte) (checksum & 0xff);

        outputBuffer[i + 8] = 0x16;

        try {
            os.write(outputBuffer, 0, i + 9);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    private MBusMessage receiveMessage() throws IOException, TimeoutException {

        int timePassedTotal = 0;
        int numBytesReadTotal = 0;
        int messageLength = -1;

        byte[] inputBuffer = new byte[MAX_MESSAGE_SIZE];

        while (true) {
            if (is.available() > 0) {

                int numBytesRead = is.read(inputBuffer, numBytesReadTotal, MAX_MESSAGE_SIZE - numBytesReadTotal);

                numBytesReadTotal += numBytesRead;

                if (messageLength == -1) {

                    if ((inputBuffer[0] & 0xff) == 0xe5) {
                        messageLength = 1;
                    }
                    else if ((inputBuffer[0] & 0xff) == 0x68 && numBytesReadTotal > 1) {
                        messageLength = (inputBuffer[1] & 0xff) + 6;
                    }
                }

                if (numBytesReadTotal == messageLength) {
                    break;
                }
            }

            if (timePassedTotal > timeout) {
                if (numBytesReadTotal == 0) {
                    throw new TimeoutException("No Bytes received. Try to increase timeout.");
                }
                if (numBytesReadTotal != messageLength) {
                    throw new TimeoutException("Incomplete response message received. Try to increase timeout.");
                }
            }
            else {

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }

                timePassedTotal += 100;
            }

        }

        MBusMessage mBusMessage;
        try {
            mBusMessage = new MBusMessage(inputBuffer, messageLength);
        } catch (DecodingException e) {
            throw new IOException("Error decoding incoming M-Bus message.");
        }

        return mBusMessage;

    }

}
