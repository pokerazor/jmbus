/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import org.junit.Assert;
import org.junit.Test;
import org.openmuc.jmbus.DataRecord.Description;

public class DataRecordParserTest {

    @Test
    public void testINT64() throws Exception {

        decode1();

        decode2();

    }

    private void decode2() throws DecodingException {
        DataRecord dataRecord = new DataRecord();
        byte[] bytes = new byte[] { (byte) 0x07, (byte) 0x04, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

        dataRecord.decode(bytes, 0, bytes.length);

        Object obj = dataRecord.getDataValue();

        Assert.assertEquals(obj instanceof Long, true);

        Long val = (Long) obj;

        Assert.assertEquals(Long.valueOf(-1l), val);
    }

    private void decode1() throws DecodingException {
        DataRecord dataRecord = new DataRecord();

        byte[] bytes = new byte[] { (byte) 0x07, (byte) 0x04, (byte) 0x12, (byte) 0x23, (byte) 0x34, (byte) 0x45,
                (byte) 0x56, (byte) 0x67, (byte) 0x78, (byte) 0x12 };

        dataRecord.decode(bytes, 0, bytes.length);

        Object obj = dataRecord.getDataValue();

        Assert.assertEquals(obj instanceof Long, true);

        Long val = (Long) obj;

        Assert.assertEquals(Long.valueOf(1330927310113874706l), val);
    }

    @Test
    public void testINT32() {

        DataRecord dataRecord = new DataRecord();

        byte[] bytes = new byte[] { (byte) 0x04, (byte) 0x03, (byte) 0xe4, (byte) 0x05, (byte) 0x00, (byte) 0x00 };

        try {
            dataRecord.decode(bytes, 0, bytes.length);

            Object obj = dataRecord.getDataValue();

            Assert.assertEquals(true, obj instanceof Long);

            Long integer = (Long) obj;

            Assert.assertEquals(Long.valueOf(1508), integer);

        } catch (DecodingException e) {
            Assert.fail("Failed to parse!");
        }

        dataRecord = new DataRecord();

        bytes = new byte[] { (byte) 0x04, (byte) 0x03, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };

        try {
            dataRecord.decode(bytes, 0, bytes.length);

            Object obj = dataRecord.getDataValue();

            Assert.assertEquals(true, obj instanceof Long);

            Long integer = (Long) obj;

            Assert.assertEquals(Long.valueOf(-1), integer);

        } catch (DecodingException e) {
            Assert.fail("Failed to parse!");
        }

    }

    private void assertParsingResults(byte[] bytes, Description desc, DlmsUnit unit, byte scaler, Object data)
            throws DecodingException {

        DataRecord dataRecord = new DataRecord();

        dataRecord.decode(bytes, 0, bytes.length);

        Assert.assertEquals(desc, dataRecord.getDescription());
        Assert.assertEquals(unit, dataRecord.getUnit());
        Assert.assertEquals(scaler, dataRecord.getMultiplierExponent());
        Assert.assertEquals(data, dataRecord.getDataValue());
    }

    @Test
    public void testDataRecords() throws DecodingException {
        /* e0000nnn Energy Wh */

        byte[] bytes = new byte[] { (byte) 0x04, (byte) 0x07, (byte) 0xc8, (byte) 0x1e, (byte) 0x00, (byte) 0x00 };

        assertParsingResults(bytes, Description.ENERGY, DlmsUnit.WATT_HOUR, (byte) 4, Long.valueOf(7880));

        /* e0001nnn Energy J */

        /* e0010nnn Volume m^3 */
        bytes = new byte[] { (byte) 0x04, (byte) 0x15, (byte) 0xfe, (byte) 0xbf, (byte) 0x00, (byte) 0x00 };

        assertParsingResults(bytes, Description.VOLUME, DlmsUnit.CUBIC_METRE, (byte) -1, Long.valueOf(49150));

        bytes = new byte[] { (byte) 0x84, (byte) 0x40, (byte) 0x15, (byte) 0xf8, (byte) 0xbf, (byte) 0x00,
                (byte) 0x00 };

        assertParsingResults(bytes, Description.VOLUME, DlmsUnit.CUBIC_METRE, (byte) -1, Long.valueOf(49144));

        /* e0011nnn Mass kg */

        /* e01000nn On Time seconds/minutes/hours/days */
        bytes = new byte[] { (byte) 0x04, (byte) 0x22, (byte) 0x38, (byte) 0x09, (byte) 0x00, (byte) 0x00 };

        assertParsingResults(bytes, Description.ON_TIME, DlmsUnit.HOUR, (byte) 0, Long.valueOf(2360));

        /* e01001nn Operating Time seconds/minutes/hours/days */
        bytes = new byte[] { (byte) 0x04, (byte) 0x26, (byte) 0x3d, (byte) 0x07, (byte) 0x00, (byte) 0x00 };

        assertParsingResults(bytes, Description.OPERATING_TIME, DlmsUnit.HOUR, (byte) 0, Long.valueOf(1853));

        /* e10110nn Flow Temperature °C */
        bytes = new byte[] { (byte) 0x02, (byte) 0x5a, (byte) 0x79, (byte) 0x02 };

        assertParsingResults(bytes, Description.FLOW_TEMPERATURE, DlmsUnit.DEGREE_CELSIUS, (byte) -1,
                Long.valueOf((short) 633));

        /* e10111nn Return Temperature °C */
        bytes = new byte[] { (byte) 0x02, (byte) 0x5e, (byte) 0xa6, (byte) 0x01 };

        assertParsingResults(bytes, Description.RETURN_TEMPERATURE, DlmsUnit.DEGREE_CELSIUS, (byte) -1,
                Long.valueOf((short) 422));

        /* e11000nn Temperature Difference K */
        bytes = new byte[] { (byte) 0x02, (byte) 0x62, (byte) 0xd3, (byte) 0x00 };

        assertParsingResults(bytes, Description.TEMPERATURE_DIFFERENCE, DlmsUnit.KELVIN, (byte) -1,
                Long.valueOf((short) 211));

        /* e1101101 Date and time - type F */
        bytes = new byte[] { (byte) 0x04, (byte) 0x6d, (byte) 0x2b, (byte) 0x11, (byte) 0x78, (byte) 0x11 };

        DataRecord dataRecord = new DataRecord();

        dataRecord.decode(bytes, 0, bytes.length);
    }

}
