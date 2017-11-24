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
import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.DeviceType;
import org.openmuc.jmbus.SecondaryAddress;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class MagneticSensorTest {

    public Object testMagneticSensorData() {
        /* Same device with no error flag just an HCA counter */
        Object[] p1 = { "2644333015010100021D72150101003330021D880400402F2F0E6E1001000000002F2F2F2F2F2F6E", 1, 110D,
                Description.HCA };

        Object[] p2 = { "2644333015010100021D72150101003330021D790400002F2F02FD971D000004FD08FC0800002F49", 2, 0D,
                Description.ERROR_FLAGS };
        return new Object[] { p1, p2 };
    }

    @Test
    @Parameters(method = "testMagneticSensorData")
    public void testMagneticSensor(String lexicalXSDHexBinary, int expectedNumOfRec, double expectedScaledVal,
            Description expectedDesc) throws DecodingException {
        byte[] sensorPacket = parseHexBinary(lexicalXSDHexBinary);
        WMBusMessage wmBusDataMessage = WMBusMessage.decode(sensorPacket, 0, new HashMap<SecondaryAddress, byte[]>());
        wmBusDataMessage.getVariableDataResponse().decode();

        assertEquals(expectedNumOfRec, wmBusDataMessage.getVariableDataResponse().getDataRecords().size());
        assertEquals("LAS", wmBusDataMessage.getSecondaryAddress().getManufacturerId());
        assertEquals(DeviceType.RESERVED_FOR_SENSOR_0X1D, wmBusDataMessage.getSecondaryAddress().getDeviceType());

        assertEquals(expectedScaledVal,
                wmBusDataMessage.getVariableDataResponse().getDataRecords().get(0).getScaledDataValue(), 0.01);
        assertEquals(DataRecord.FunctionField.INST_VAL,
                wmBusDataMessage.getVariableDataResponse().getDataRecords().get(0).getFunctionField());
        assertEquals(expectedDesc, wmBusDataMessage.getVariableDataResponse().getDataRecords().get(0).getDescription());
    }
}
