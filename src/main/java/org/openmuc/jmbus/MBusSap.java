/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import org.openmuc.jmbus.MBusMessage.MessageType;
import org.openmuc.jmbus.VerboseMessage.MessageDirection;
import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.SerialPort;
import org.openmuc.jrxtx.SerialPortBuilder;
import org.openmuc.jrxtx.SerialPortTimeoutException;
import org.openmuc.jrxtx.StopBits;

/**
 * M-Bus Application Layer Service Access Point (SAP).
 * <p>
 * Use this access point to communicate using the M-Bus wired protocol.
 * </p>
 */
public class MBusSap implements AutoCloseable {

    // 261 is the maximum size of a long frame
    private final static int MAX_MESSAGE_SIZE = 261;

    private SerialPort serialPort;

    private final byte[] outputBuffer = new byte[MAX_MESSAGE_SIZE];

    private final byte[] dataRecordsAsBytes = new byte[MAX_MESSAGE_SIZE];

    private final boolean[] frameCountBits;

    private DataOutputStream os = null;
    private DataInputStream is = null;

    private int timeout = 500;
    private SecondaryAddress secondaryAddress = null;

    private VerboseMessageListener verboseMessageListener = null;

    private byte[] sentBytes = {};

    private boolean echoCancellation = false;

    private final String serialPortName;

    private final int baudRate;

    /**
     * Creates an M-Bus Service Access Point that is used to read meters.
     * 
     * @param serialPortName
     *            examples for serial port identifiers are on Linux "/dev/ttyS0" or "/dev/ttyUSB0" and on Windows "COM1"
     * @param baudRate
     *            the baud rate to use.
     * @see MBusSap#open()
     */
    public MBusSap(String serialPortName, int baudRate) {

        this.serialPortName = serialPortName;
        this.baudRate = baudRate;

        // set all frame bits to true
        this.frameCountBits = new boolean[254];
        for (int i = 0; i < frameCountBits.length; i++) {
            frameCountBits[i] = true;
        }

        // dead code
        // if (verboseMessageListener != null) {
        // verboseMessageListener.newVerboseMessage(
        // "Serial port name: " + serialPortName + "\n Baud rate: " + baudRate + "\n Timeout: " + timeout);
        // }
    }

    /**
     * Opens the serial port. The serial port needs to be opened before attempting to read a device.
     * 
     * @throws IOException
     *             if any kind of error occurs opening the serial port.
     */
    public void open() throws IOException {
        this.serialPort = SerialPortBuilder.newBuilder(serialPortName)
                .setBaudRate(baudRate)
                .setDataBits(DataBits.DATABITS_8)
                .setStopBits(StopBits.STOPBITS_1)
                .setParity(Parity.EVEN)
                .build();
        serialPort.setSerialPortTimeout(this.timeout);

        os = new DataOutputStream(serialPort.getOutputStream());
        is = new DataInputStream(serialPort.getInputStream());
    }

    /**
     * Closes the M-Bus SAP. Releases all allocated resources.
     */
    @Override
    public void close() {
        if (serialPort == null) {
            return;
        }
        try {
            serialPort.close();
        } catch (IOException e) {
            // ignore
        }
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
     * Returns the timeout in milliseconds (ms).
     * 
     * @return the timeout in ms.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets the verbose mode on if a implementation of debugMessageListener is setted.
     * 
     * @param verboseMessageListener
     *            Implementation of debugMessageListener
     */
    public void setVerboseMessageListener(VerboseMessageListener verboseMessageListener) {
        this.verboseMessageListener = verboseMessageListener;
    }

    /**
     * Set true for activating echo cancellation.
     * 
     * <p>
     * Note: UNTESTED!
     * </p>
     * 
     * @param echoCancellation
     *            for activating true else false
     */
    public void setEchoCancellation(boolean echoCancellation) {
        this.echoCancellation = echoCancellation;
    }

    /**
     * Reads a meter using primary addressing. Sends a data request (REQ_UD2) to the remote device and returns the
     * variable data structure from the received RSP_UD frame.
     * 
     * @param primaryAddress
     *            the primary address of the meter to read. For secondary address use 0xfd.
     * @return the variable data structure from the received RSP_UD frame
     * @throws InterruptedIOException
     *             if no response at all (not even a single byte) was received from the meter within the timeout span.
     * @throws IOException
     *             if any kind of error (including timeout) occurs while trying to read the remote device. Note that the
     *             connection is not closed when an IOException is thrown.
     * @throws SerialPortTimeoutException
     *             if no response at all (not even a single byte) was received from the meter within the timeout span.
     */
    public VariableDataStructure read(int primaryAddress) throws IOException, SerialPortTimeoutException {

        if (serialPort.isClosed()) {
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
     * @throws SerialPortTimeoutException
     *             if no response at all (not even a single byte) was received from the meter within the timeout span.
     */
    public boolean write(int primaryAddress, byte[] data) throws IOException, SerialPortTimeoutException {
        if (data == null) {
            data = new byte[0];
        }

        boolean ret = sendLongMessage(primaryAddress, 0x73, 0x51, data.length, data);
        MBusMessage mBusMessage = receiveMessage();

        if (mBusMessage.getMessageType() != MessageType.SINGLE_CHARACTER) {
            throw new IOException("uUnable to select component.");
        }
        return ret;
    }

    /**
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

        try {
            if (sendLongMessage(0xfd, 0x53, 0x52, 8, ba)) {

                MBusMessage mBusMessage = receiveMessage();

                if (mBusMessage.getMessageType() == MessageType.SINGLE_CHARACTER) {
                    return true;
                }
            }
        } catch (InterruptedIOException e) {
            // ignore
        } catch (IOException e) {
            return true;
        }
        return false;
    }

    /**
     * Selects the meter with the specified secondary address. After this the meter can be read on primary address 0xfd.
     * 
     * @param secondaryAddress
     *            the secondary address of the meter to select.
     * @throws IOException
     *             if any kind of error (including timeout) occurs while trying to read the remote device. Note that the
     *             connection is not closed when an IOException is thrown.
     * @throws SerialPortTimeoutException
     *             if no response at all (not even a single byte) was received from the meter within the timeout span.
     */
    public void selectComponent(SecondaryAddress secondaryAddress) throws IOException, SerialPortTimeoutException {
        this.secondaryAddress = secondaryAddress;
        componentSelection(false);
    }

    /**
     * Deselects the previously selected meter.
     * 
     * @throws IOException
     *             if any kind of error (including timeout) occurs while trying to read the remote device. Note that the
     *             connection is not closed when an IOException is thrown.
     * @throws SerialPortTimeoutException
     *             if no response at all (not even a single byte) was received from the meter within the timeout span.
     */
    public void deselectComponent() throws IOException, SerialPortTimeoutException {
        if (secondaryAddress != null) {
            componentSelection(true);
            secondaryAddress = null;
        }
    }

    /**
     * Selection of wanted records.
     * 
     * @param primaryAddress
     *            primary address of the slave
     * @param dataRecords
     *            data record to select
     * @throws IOException
     *             if any kind of error (including timeout) occurs while trying to read the remote device. Note that the
     *             connection is not closed when an IOException is thrown.
     * @throws SerialPortTimeoutException
     *             if no response at all (not even a single byte) was received from the meter within the timeout span.
     */
    public void selectForReadout(int primaryAddress, List<DataRecord> dataRecords)
            throws IOException, SerialPortTimeoutException {

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

    /**
     * Sends a application reset to the slave with specified primary address.
     * 
     * @param primaryAddress
     *            primary address of the slave
     * @throws IOException
     *             if any kind of error (including timeout) occurs while trying to read the remote device. Note that the
     *             connection is not closed when an IOException is thrown.
     * @throws SerialPortTimeoutException
     *             if no response at all (not even a single byte) was received from the meter within the timeout span.
     */
    public void resetReadout(int primaryAddress) throws IOException, SerialPortTimeoutException {
        sendLongMessage(primaryAddress, 0x53, 0x50, 0, new byte[] {});
        MBusMessage mBusMessage = receiveMessage();

        if (mBusMessage.getMessageType() != MessageType.SINGLE_CHARACTER) {
            throw new IOException("Unable to reset application.");
        }
    }

    /**
     * Sends a SND_NKE message to reset the FCB (frame counter bit).
     * 
     * @param primaryAddress
     *            the primary address of the meter to reset.
     * @throws InterruptedIOException
     *             if the slave does not answer with an 0xe5 message within the configured timeout span.
     * @throws IOException
     *             if an error occurs during the reset process.
     * @throws SerialPortTimeoutException
     *             - * if the slave does not answer with an 0xe5 message within the configured timeout span.
     */
    public void linkReset(int primaryAddress) throws IOException, SerialPortTimeoutException {
        sendShortMessage(primaryAddress, 0x40);
        MBusMessage mBusMessage = receiveMessage();

        if (mBusMessage.getMessageType() != MessageType.SINGLE_CHARACTER) {
            throw new IOException("Unable to reset link.");
        }

        frameCountBits[primaryAddress] = true;
    }

    private void componentSelection(boolean deselect) throws IOException, SerialPortTimeoutException {
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

        if (echoCancellation) {
            sentBytes = Arrays.copyOfRange(outputBuffer, 0, 5);
        }
        verboseMessage(MessageDirection.SEND, outputBuffer, 0, 5);

        synchronized (os) {
            os.write(outputBuffer, 0, 5);
        }
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

        if (echoCancellation) {
            sentBytes = Arrays.copyOfRange(outputBuffer, 0, i + 9);
        }

        verboseMessage(MessageDirection.SEND, outputBuffer, 0, i + 9);

        try {
            synchronized (os) {
                os.write(outputBuffer, 0, i + 9);
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    private MBusMessage receiveMessage() throws IOException {

        byte[] receivedBytes;

        int b0 = is.read();
        if (b0 == 0xe5) {
            // messageLength = 1;
            receivedBytes = new byte[] { (byte) b0 };
        }
        else if ((b0 & 0xff) == 0x68) {
            int b1 = is.readByte() & 0xff;

            /**
             * The L field gives the quantity of the user data inputs plus 3 (for C,A,CI).
             */
            int messageLength = b1 + 6;

            receivedBytes = new byte[messageLength];
            receivedBytes[0] = (byte) b0;
            receivedBytes[1] = (byte) b1;

            int lenRead = messageLength - 2;

            is.readFully(receivedBytes, 2, lenRead);
        }
        else {
            throw new IOException(String.format("Received unknown message: %02X", b0));
        }

        if (Arrays.equals(sentBytes, receivedBytes)) {
            verboseMessage("received echo");
            return receiveMessage();
        }

        verboseMessage(MessageDirection.RECEIVE, receivedBytes, 0, receivedBytes.length);

        return MBusMessage.decode(receivedBytes, receivedBytes.length);
    }

    private void verboseMessage(MessageDirection direction, byte[] array, int from, int to) {
        if (this.verboseMessageListener == null) {
            return;
        }

        byte[] message = Arrays.copyOfRange(array, from, to);

        VerboseMessage debugMessage = new VerboseMessage(direction, message);
        this.verboseMessageListener.newVerboseMessage(debugMessage);
    }

    private void verboseMessage(String information) {
        if (verboseMessageListener == null) {
            return;
        }

        verboseMessageListener.newVerboseMessage(information);
    }

}
