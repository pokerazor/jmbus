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

/**
 * This class represents a binary-coded decimal (BCD) number as defined by the M-Bus standard. The class provides
 * methods to convert the BCD to other types such as <code>double</code>, <code>int</code> or <code>String</code>.
 * 
 * @author Stefan Feuerhahn
 * @author Michael Zillgith
 * 
 */
public class Bcd extends Number {

    private static final long serialVersionUID = 790515601507532939L;
    private final byte[] value;

    /**
     * Constructs a <code>Bcd</code> from the given bytes. The constructed Bcd will use the given byte array for
     * internal storage of its value. It is therefore recommended not to change the byte array after construction.
     * 
     * @param bcdBytes
     *            the byte array to be used for construction of the <code>Bcd</code>
     */
    public Bcd(byte[] bcdBytes) {
        value = bcdBytes;
    }

    public byte[] getBytes() {
        return value;
    }

    @Override
    public String toString() {
        byte[] ba;
        int shift;
        ba = new byte[value.length * 2];
        int c = 0;

        for (int i = value.length - 1; i >= 0; i--) {
            shift = value[i] >> 4;
            ba[c++] = (byte) ((shift & 0x0f) + 48);

            shift = value[i];
            ba[c++] = (byte) ((shift & 0x0f) + 48);
        }

        return new String(ba);
    }

    /**
     * Returns the value of this <code>Bcd</code> as a double.
     */
    @Override
    public double doubleValue() {
        return longValue();
    }

    /**
     * Returns the value of this <code>Bcd</code> as a float.
     */
    @Override
    public float floatValue() {
        return longValue();
    }

    /**
     * Returns the value of this <code>Bcd</code> as an int.
     */
    @Override
    public int intValue() {
        int result = 0;
        int factor = 1;

        for (byte element : value) {
            result += (element & 0x0f) * factor;
            factor = factor * 10;
            result += ((element >> 4) & 0x0f) * factor;
            factor = factor * 10;
        }

        return result;
    }

    /**
     * Returns the value of this <code>Bcd</code> as a long.
     */
    @Override
    public long longValue() {
        long result = 0l;
        long factor = 1l;

        for (byte element : value) {
            result += (element & 0x0f) * factor;
            factor = factor * 10l;
            result += ((element >> 4) & 0x0f) * factor;
            factor = factor * 10l;
        }

        return result;
    }

}
