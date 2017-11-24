/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.SerialPortBuilder;
import org.openmuc.jrxtx.StopBits;

/**
 * Was tested with the IMST iM871A-USB Wireless M-Bus stick.<br>
 */
public class WMBusSapImst extends AbstractWMBusSap {

    private MessageReceiver receiver;
    private final String serialPortName;

    private class MessageReceiver extends Thread {

        private static final byte MBUS_BL_CONTROL = 0x44;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
            HciMessage hciMessage = null;
            while (true) {
                hciMessage = new HciMessage(is);

                if (hciMessage.payload().length > 1) {
                    if (hciMessage.payload()[1] == MBUS_BL_CONTROL) {
                        break;
                    }
                    else {
                        discard(hciMessage);
                    }
                }
                else {
                    // No payload, most a response
                }
            }

            final byte[] payload = hciMessage.payload();
            final int signalStrengthInDBm = hciMessage.rSSI();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.newMessage(new WMBusMessage(payload, signalStrengthInDBm, keyMap));
                }
            });
        }

        private void discard(HciMessage hciMessage) {
            final byte[] messageData = hciMessage.payload;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.discardedBytes(messageData);
                }
            });
        }
    }

    public WMBusSapImst(String serialPortName, WMBusMode mode, WMBusListener listener) {
        super(mode, listener);
        this.serialPortName = serialPortName;

    }

    @Override
    public void open() throws IOException {
        if (!closed) {
            return;
        }
        this.serialPort = SerialPortBuilder.newBuilder(this.serialPortName)
                .setBaudRate(57600)
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
        byte[] payload = new byte[6];
        payload[0] = 0x00; // NVM Flag: change configuration only temporary
        payload[1] = 0x03; // IIFlag 1: Bit 0 Device Mode and Bit 1 Radio Mode
        payload[2] = 0x00; // Device Mode: Meter

        payload[4] = 16; // IIFlag 2: Bit 4 : Auto RSSI Attachment
        payload[5] = 0x01; // Rx-Timestamp attached for each received Radio message
        switch (mode) {
        case S:
            payload[3] = 0x01; // Link/Radio Mode: S1-m
            writeCommand((byte) 0, Const.DEVMGMT_ID, Const.DEVMGMT_MSG_SET_CONFIG_REQ, payload);
            break;
        case T:
            payload[3] = 0x04; // Link/Radio Mode: T2
            writeCommand((byte) 0, Const.DEVMGMT_ID, Const.DEVMGMT_MSG_SET_CONFIG_REQ, payload);
            break;
        default:
            throw new IOException("wMBUS Mode '" + mode.toString() + "' is not supported");
        }
    }

    private boolean writeCommand(byte controlField, byte endpointId, byte msgId, byte[] payload) {
        int lengthPayloadAll = payload.length & 0xFF;
        double numOfPackagesTemp = lengthPayloadAll / Const.MAX_SINGLE_PAYLOAD_SIZE;

        int numOfPackages = (int) numOfPackagesTemp;
        if (numOfPackages == 0 || numOfPackagesTemp % 1 != 0) {
            ++numOfPackages;
        }

        if (numOfPackages <= Const.MAX_PACKAGES) {

            for (int i = 0; i < numOfPackages; ++i) {
                int payloadSendLength = Const.MAX_SINGLE_PAYLOAD_SIZE;
                if (numOfPackages - i == 1) {
                    payloadSendLength = lengthPayloadAll - (numOfPackages - 1) * Const.MAX_SINGLE_PAYLOAD_SIZE;
                }
                byte[] payloadSend = new byte[payloadSendLength];
                System.arraycopy(payload, i * payloadSendLength, payloadSend, 0, payloadSendLength);

                byte controlField_EndpointField = (byte) ((controlField << 4) | endpointId);
                byte[] hciHeader = { Const.START_OF_FRAME, controlField_EndpointField, msgId,
                        (byte) payloadSendLength };

                byte[] hciMessage = new byte[Const.HCI_HEADER_LENGTH + payloadSendLength];
                System.arraycopy(hciHeader, 0, hciMessage, 0, hciHeader.length);
                System.arraycopy(payloadSend, 0, hciMessage, hciHeader.length, payloadSend.length);

                try {
                    os.write(hciMessage);
                } catch (IOException e) {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Writes a reset command to the IMST module
     */
    public void reset() {
        writeCommand((byte) 0, Const.DEVMGMT_MSG_FACTORY_RESET_REQ, Const.DEVMGMT_ID, new byte[] {});
    }

    /**
     * IMST constants packages
     */
    class Const {
        public static final byte START_OF_FRAME = (byte) 0xA5;
        // A5 01 03
        public static final int MAX_PACKAGES = 255;
        public static final int MAX_SINGLE_PAYLOAD_SIZE = 255;
        public static final int HCI_HEADER_LENGTH = 4;

        // ControlField
        public final static byte RESERVED = 0x00; // 0b0000
        public final static byte TIMESTAMP_ATTACHED = 0x02; // 0b0010
        public final static byte RSSI_ATTACHED = 0x04; // 0b0100
        public final static byte CRC16_ATTACHED = 0x08; // 0b1000 (FCS)

        // List of Endpoint Identifier
        public final static byte DEVMGMT_ID = 0x01;
        public final static byte RADIOLINK_ID = 0x02;
        public final static byte RADIOLINKTEST_ID = 0x03;
        public final static byte HWTEST_ID = 0x04;

        // Device Management MessageIdentifier
        public final static byte DEVMGMT_MSG_PING_REQ = 0x01;
        public final static byte DEVMGMT_MSG_PING_RSP = 0x02;
        public final static byte DEVMGMT_MSG_SET_CONFIG_REQ = 0x03;
        public final static byte DEVMGMT_MSG_SET_CONFIG_RSP = 0x04;
        public final static byte DEVMGMT_MSG_GET_CONFIG_REQ = 0x05;
        public final static byte DEVMGMT_MSG_GET_CONFIG_RSP = 0x06;
        public final static byte DEVMGMT_MSG_RESET_REQ = 0x07;
        public final static byte DEVMGMT_MSG_RESET_RSP = 0x08;
        public final static byte DEVMGMT_MSG_FACTORY_RESET_REQ = 0x09;
        public final static byte DEVMGMT_MSG_FACTORY_RESET_RSP = 0x0A;
        public final static byte DEVMGMT_MSG_GET_OPMODE_REQ = 0x0B;
        public final static byte DEVMGMT_MSG_GET_OPMODE_RSP = 0x0C;
        public final static byte DEVMGMT_MSG_SET_OPMODE_REQ = 0x0D;
        public final static byte DEVMGMT_MSG_SET_OPMODE_RSP = 0x0E;
        public final static byte DEVMGMT_MSG_GET_DEVICEINFO_REQ = 0x0F;
        public final static byte DEVMGMT_MSG_GET_DEVICEINFO_RSP = 0x10;
        public final static byte DEVMGMT_MSG_GET_SYSSTATUS_REQ = 0x11;
        public final static byte DEVMGMT_MSG_GET_SYSSTATUS_RSP = 0x12;
        public final static byte DEVMGMT_MSG_GET_FWINFO_REQ = 0x13;
        public final static byte DEVMGMT_MSG_GET_FWINFO_RSP = 0x14;
        public final static byte DEVMGMT_MSG_GET_RTC_REQ = 0x19;
        public final static byte DEVMGMT_MSG_GET_RTC_RSP = 0x1A;
        public final static byte DEVMGMT_MSG_SET_RTC_REQ = 0x1B;
        public final static byte DEVMGMT_MSG_SET_RTC_RSP = 0x1C;
        public final static byte DEVMGMT_MSG_ENTER_LPM_REQ = 0x1D;
        public final static byte DEVMGMT_MSG_ENTER_LPM_RSP = 0x1E;
        public final static byte DEVMGMT_MSG_SET_AES_ENCKEY_REQ = 0x21;
        public final static byte DEVMGMT_MSG_SET_AES_ENCKEY_RSP = 0x22;
        public final static byte DEVMGMT_MSG_ENABLE_AES_ENCKEY_REQ = 0x23;
        public final static byte DEVMGMT_MSG_ENABLE_AES_ENCKEY_RSP = 0x24;
        public final static byte DEVMGMT_MSG_SET_AES_DECKEY_RSP_0X25 = 0x25;
        public final static byte DEVMGMT_MSG_SET_AES_DECKEY_RSP_0X26 = 0x26;
        public final static byte DEVMGMT_MSG_AES_DEC_ERROR_IND = 0x27;

        // Radio Link Message Identifier
        public final static byte RADIOLINK_MSG_WMBUSMSG_REQ = 0x01;
        public final static byte RADIOLINK_MSG_WMBUSMSG_RSP = 0x02;
        public final static byte RADIOLINK_MSG_WMBUSMSG_IND = 0x03;
        public final static byte RADIOLINK_MSG_DATA_REQ = 0x04;
        public final static byte RADIOLINK_MSG_DATA_RSP = 0x05;

    }

    /**
     * <br>
     * <li><tt>HCI Message</tt>
     * <ul>
     * <li>StartOfFrame 8 Bit: 0xA5
     * <li>MsgHeader 24 Bit:
     * <ul>
     * <li>ControlField 4 Bit:
     * <ul>
     * <li>0000b Reserved
     * <li>0010b Time Stamp Field attached
     * <li>0100b RSSI Field attached
     * <li>1000b CRC16 Field attached
     * </ul>
     * <li>EndPoint ID 4 Bit: Identifies a logical message endpoint which groups several messages.
     * <li>Msg ID Field 8 Bit: Identifies the message type.
     * <li>LengthFiled 8 Bit: Number of bytes in the payload. If null no payload.
     * </ul>
     * <li>PayloadField n * 8 Bit: wMBus Message
     * <li>Time Stamp (optional): 32 Bit Timestamp of the RTC
     * <li>RSSI (optional) 8 Bit: Receive Signal Strength Indicator
     * <li>FCS (optional) 16 Bit: CRC from Control Field up to last byte of Payload, Time Stamp or RSSI Field.</li>
     * </ul>
     *
     */
    private class HciMessage {

        private byte controlField = 0;;
        private byte endpointID = 0;
        private byte msgId = 0;
        private int length = 0;

        private byte[] payload = {};
        private int timeStamp = 0;
        private int rSSI = 0;
        private int fCS = 0;

        public HciMessage(DataInputStream is) throws IOException {

            byte b0, b1;
            b0 = is.readByte();
            if (b0 != Const.START_OF_FRAME) {
                return;
            }
            b1 = is.readByte();
            controlField = (byte) ((b1 >> 4) & (byte) 0x0F);
            endpointID = (byte) ((b1 & (byte) 0x0F));

            msgId = is.readByte();
            length = is.readByte();
            payload = new byte[length + 1];
            is.read(payload, 1, length);
            payload[0] = (byte) length;

            if ((controlField & Const.TIMESTAMP_ATTACHED) == Const.TIMESTAMP_ATTACHED) {
                timeStamp = is.readInt();
            }
            if ((controlField & Const.RSSI_ATTACHED) == Const.RSSI_ATTACHED) {
                rSSI = is.readByte() & 0xFF;
            }
            if ((controlField & Const.CRC16_ATTACHED) == Const.CRC16_ATTACHED) {
                fCS = is.readShort() & 0xFFFF;
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Control Field: ")
                    .append(HexConverter.toHexString(controlField))
                    .append("\nEndpointID:    ")
                    .append(HexConverter.toHexString(endpointID))
                    .append("\nMsg ID:        ")
                    .append(HexConverter.toHexString(msgId))
                    .append("\nLength:        ")
                    .append(length)
                    .append("\nTimestamp:     ")
                    .append(timeStamp)
                    .append("\nRSSI:          ")
                    .append(rSSI)
                    .append("\nFCS:           ")
                    .append(fCS)
                    .append("\nPayload:\n")
                    .append(HexConverter.toHexString(payload));
            return sb.toString();
        }

        public byte[] payload() {
            return payload;
        }

        public int rSSI() {
            return rSSI;
        }

    }

}
