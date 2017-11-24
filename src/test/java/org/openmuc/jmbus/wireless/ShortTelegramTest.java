/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus.wireless;

import static javax.xml.bind.DatatypeConverter.parseHexBinary;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;
import org.openmuc.jmbus.SecondaryAddress;

public class ShortTelegramTest {

    @Test
    public void testShortKamstrup() throws Exception {
        byte[] packetLong = parseHexBinary(
                "5C442D2C06357260190C8D207B70032F21271D7802F9FF15011104061765000004EEFF07BFA8000004EEFF08D24F00000414B1FB000002FD170000026CE919426CFF184406F76400004414E8FA0000043B0B0000000259DB11025D1C0B5B");
        byte[] packetShort = parseHexBinary(
                "3F442D2C06357260190C8D207C71032F21255C79DD829283011117650000BFA80000D24F0000B1FB00000000E919FF18F7640000E8FA00000B000000DB111C0B5B");

        HashMap<SecondaryAddress, byte[]> keyMap = new HashMap<>();
        byte[] key = new byte[] {};
        keyMap.put(SecondaryAddress.newFromWMBusLlHeader(packetShort, 0), key);

        WMBusMessage wmBusDataMessage = WMBusMessage.decode(packetShort, 0, keyMap);

        wmBusDataMessage.getVariableDataResponse().decode();

        /* Could not decode becase no long header was present */
        assertEquals(0, wmBusDataMessage.getVariableDataResponse().getDataRecords().size());

        wmBusDataMessage = WMBusMessage.decode(packetLong, 0, new HashMap<SecondaryAddress, byte[]>());
        wmBusDataMessage.getVariableDataResponse().decode();

        /* Can decode long header */
        assertEquals(13, wmBusDataMessage.getVariableDataResponse().getDataRecords().size());

        wmBusDataMessage = WMBusMessage.decode(packetShort, 0, new HashMap<SecondaryAddress, byte[]>());
        wmBusDataMessage.getVariableDataResponse().decode();

        /* Can short header now */
        assertEquals(13, wmBusDataMessage.getVariableDataResponse().getDataRecords().size());
    }

    @Test
    public void testShortKamstrupNew() throws Exception {

        byte[] packetLong = parseHexBinary(
                "40442D2C713785691C0C8D2066445050201E5E780406A60B000004FF074E11000004FF08130700000414C91A000002FD170000043B000000000259B10B025D67095B");
        byte[] packetShort = parseHexBinary(
                "31442D2C713785691C0C8D2067585050202A4479C4D788B0A60B00004E11000013070000C91A0000000000000000B10B67095B");

        WMBusMessage wmBusDataMessage = WMBusMessage.decode(packetShort, 0, new HashMap<SecondaryAddress, byte[]>());
        wmBusDataMessage.getVariableDataResponse().decode();
        /* Could not decode becase no long header was present */
        assertEquals(0, wmBusDataMessage.getVariableDataResponse().getDataRecords().size());

        wmBusDataMessage = WMBusMessage.decode(packetLong, 0, new HashMap<SecondaryAddress, byte[]>());
        wmBusDataMessage.getVariableDataResponse().decode();

        /* Can decode long header */
        assertEquals(8, wmBusDataMessage.getVariableDataResponse().getDataRecords().size());

        wmBusDataMessage = WMBusMessage.decode(packetShort, 0, new HashMap<SecondaryAddress, byte[]>());
        wmBusDataMessage.getVariableDataResponse().decode();

        /* Can short header now */
        assertEquals(8, wmBusDataMessage.getVariableDataResponse().getDataRecords().size());
    }

}
