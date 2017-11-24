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
import org.junit.runner.RunWith;
import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.DataRecord.Description;
import org.openmuc.jmbus.SecondaryAddress;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class GeneralTests {
    public Object testData() {
        Object[] p1 = { "2644333003000000011B72030000003330011B542000002F2F02FD1701002F2F2F2F2F2F2F2F2F80", "LAS", 1D,
                DataRecord.Description.ERROR_FLAGS };

        Object[] p2 = { "2C44A7320613996707047A2A1000202F2F0C06000000000C14000000000C22381701000B5A1702000B5E1702006E",
                "LUG", 0D, DataRecord.Description.ENERGY };
        return new Object[] { p1, p2 };
    }

    @Test
    @Parameters(method = "testData")
    public void test(String lexicalXSDHexBinary, String expectedManId, double expectedDataValue,
            Description expectedDesc) throws Exception {
        byte[] errorPacket = parseHexBinary(lexicalXSDHexBinary);
        WMBusMessage wmBusDataMessage = WMBusMessage.decode(errorPacket, 0, new HashMap<SecondaryAddress, byte[]>());
        wmBusDataMessage.getVariableDataResponse().decode();

        assertEquals(expectedManId, wmBusDataMessage.getSecondaryAddress().getManufacturerId());

        assertEquals(DataRecord.FunctionField.INST_VAL,
                wmBusDataMessage.getVariableDataResponse().getDataRecords().get(0).getFunctionField());

        assertEquals(expectedDataValue,
                wmBusDataMessage.getVariableDataResponse().getDataRecords().get(0).getScaledDataValue(), 0.001);

        assertEquals(expectedDesc, wmBusDataMessage.getVariableDataResponse().getDataRecords().get(0).getDescription());
    }
}
