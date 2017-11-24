/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus.wireless;

import static javax.xml.bind.DatatypeConverter.parseHexBinary;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.SecondaryAddress;

public class DecryptionTest {

    private final byte[] testFrameKamstrupEncrypted = parseHexBinary(
            "24442D2C692845631B168D3050209CD621B006B1140AEF4953AE5B86FAFC0B00E70705B84689");

    @Test
    public void testDecryption() throws Exception {
        byte[] goodKey = parseHexBinary("4E5508544202058100DFEFA06B0934A5");

        WMBusMessage wmBusDataMessage = decodewith(goodKey);

        assertEquals(DataRecord.FunctionField.INST_VAL,
                wmBusDataMessage.getVariableDataResponse().getDataRecords().get(1).getFunctionField());
        assertEquals(474.24, wmBusDataMessage.getVariableDataResponse().getDataRecords().get(1).getScaledDataValue(),
                0.001);
    }

    @Test(expected = DecodingException.class)
    public void testDecryptionWrongKey() throws Exception {
        byte[] wrongKey = parseHexBinary("4E5508544202058100DFEFA06B0934AF");

        decodewith(wrongKey);
    }

    private WMBusMessage decodewith(byte[] key) throws DecodingException {
        Map<SecondaryAddress, byte[]> keyMap = new HashMap<>();
        keyMap.put(SecondaryAddress.newFromWMBusLlHeader(testFrameKamstrupEncrypted, 2), key);

        WMBusMessage wmBusDataMessage = WMBusMessage.decode(testFrameKamstrupEncrypted, 0, keyMap);
        wmBusDataMessage.getVariableDataResponse().decode();
        return wmBusDataMessage;
    }
}
