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

import java.util.HashMap;

import org.junit.Test;

public class WMbusDemoMessageTest {

    @Test
    public void testMessage1() throws DecodingException {
        // manufacturer ID:LSE, device ID:58511882, device version:44, device type:WARM_WATER_METER, as
        // bytes:6532821851582c06
        byte[] testMessage1 = new byte[] { (byte) 0x2c, (byte) 0x44, (byte) 0x65, (byte) 0x32, (byte) 0x82, (byte) 0x18,
                (byte) 0x51, (byte) 0x58, (byte) 0x2c, (byte) 0x06, (byte) 0x7a, (byte) 0xe1, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x04, (byte) 0x6d, (byte) 0x19, (byte) 0x06, (byte) 0xd9, (byte) 0x18, (byte) 0x0c,
                (byte) 0x13, (byte) 0x34, (byte) 0x12, (byte) 0x00, (byte) 0x00, (byte) 0x42, (byte) 0x6c, (byte) 0xbf,
                (byte) 0x1c, (byte) 0x4c, (byte) 0x13, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x32,
                (byte) 0x6c, (byte) 0xff, (byte) 0xff, (byte) 0x01, (byte) 0xfd, (byte) 0x73, (byte) 0x00 };

        byte[] testMessage2 = new byte[] {};
        byte[] key = new byte[] {};

        HashMap<String, byte[]> keyMap = new HashMap<String, byte[]>();
        WMBusMessage message2 = null;

        if (testMessage2.length > 0) {
            SecondaryAddress secondaryAddress2 = SecondaryAddress.getFromWMBusLinkLayerHeader(testMessage2, 2);
            keyMap.put(HexConverter.toShortHexString(secondaryAddress2.asByteArray()), key);
            message2 = new WMBusMessage(testMessage2, 100, keyMap);
            message2.decodeDeep();
        }

        WMBusMessage message1 = new WMBusMessage(testMessage1, 100, keyMap);

        message1.decodeDeep();

        System.out.println();
        System.out.println(message1.toString());

        if (testMessage2.length > 0) {
            System.out.println();
            System.out.println(message2.toString());
        }
    }

}
