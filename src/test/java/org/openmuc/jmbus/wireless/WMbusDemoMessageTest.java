/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus.wireless;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Test;
import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.DeviceType;
import org.openmuc.jmbus.SecondaryAddress;

public class WMbusDemoMessageTest {

    @Test
    public void testMessage1() throws DecodingException {
        // device info as bytes:6532821851582c06
        byte[] testMessage1 = new byte[] { (byte) 0x2c, (byte) 0x44, (byte) 0x65, (byte) 0x32, (byte) 0x82, (byte) 0x18,
                (byte) 0x51, (byte) 0x58, (byte) 0x2c, (byte) 0x06, (byte) 0x7a, (byte) 0xe1, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x04, (byte) 0x6d, (byte) 0x19, (byte) 0x06, (byte) 0xd9, (byte) 0x18, (byte) 0x0c,
                (byte) 0x13, (byte) 0x34, (byte) 0x12, (byte) 0x00, (byte) 0x00, (byte) 0x42, (byte) 0x6c, (byte) 0xbf,
                (byte) 0x1c, (byte) 0x4c, (byte) 0x13, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x32,
                (byte) 0x6c, (byte) 0xff, (byte) 0xff, (byte) 0x01, (byte) 0xfd, (byte) 0x73, (byte) 0x00 };

        Map<SecondaryAddress, byte[]> keyMap = emptyMap();
        WMBusMessage message1 = WMBusMessage.decode(testMessage1, 100, keyMap);

        message1.getVariableDataResponse().decode();

        assertNotNull(message1.getVariableDataResponse());

        SecondaryAddress secondaryAddress = message1.getSecondaryAddress();

        assertEquals("LSE", secondaryAddress.getManufacturerId());
        assertEquals(58511882, secondaryAddress.getDeviceId().intValue());
        assertEquals(44, secondaryAddress.getVersion());
        assertEquals(DeviceType.WARM_WATER_METER, secondaryAddress.getDeviceType());

    }

}
