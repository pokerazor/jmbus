/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus.wireless;

import static javax.xml.bind.DatatypeConverter.parseHexBinary;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.SecondaryAddress;

public class BCDNegativeTempTest {

    @Test
    public void testDecodeNegativeTemperature6BCD() throws Exception {
        byte[] message = parseHexBinary(
                "2C44A7320613996707047A821000202F2F0C06000000000C14000000000C22224101000B5A4102000B5E4000F05E");

        Map<SecondaryAddress, byte[]> keyMap = Collections.emptyMap();
        WMBusMessage wmBusDataMessage = WMBusMessage.decode(message, 0, keyMap);
        wmBusDataMessage.getVariableDataResponse().decode();

        List<DataRecord> dataRecords = wmBusDataMessage.getVariableDataResponse().getDataRecords();
        int size = dataRecords.size();

        Double lastValue = dataRecords.get(size - 1).getScaledDataValue();

        assertEquals(-4.1, lastValue, 0.1);
    }

}
