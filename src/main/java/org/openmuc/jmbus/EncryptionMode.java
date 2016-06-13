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
import java.util.Map;

public enum EncryptionMode {
    NONE(0),
    RESERVED_01(1),
    DES_CBC(2),
    DES_CBC_IV(3),
    RESERVED_04(4),
    AES_CBC_IV(5),
    RESERVED_06(6),
    RESERVED_07(7),
    RESERVED_08(8),
    RESERVED_09(9),
    RESERVED_10(10),
    RESERVED_11(11),
    RESERVED_12(12),
    RESERVED_13(13),
    RESERVED_14(14),
    RESERVED_15(15);

    private final int id;

    private static final Map<Integer, EncryptionMode> idMap = new HashMap<Integer, EncryptionMode>();

    static {
        for (EncryptionMode enumInstance : EncryptionMode.values()) {
            if (idMap.put(enumInstance.getId(), enumInstance) != null) {
                throw new IllegalArgumentException("duplicate ID: " + enumInstance.getId());
            }
        }
    }

    private EncryptionMode(int id) {
        this.id = id;
    }

    /**
     * Returns the ID of this EncryptionMode.
     * 
     * @return the ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the EncryptionMode that corresponds to the given ID. Throws an IllegalArgumentException if no
     * EncryptionMode with the given ID exists.
     * 
     * @param id
     *            the ID
     * @return the EncryptionMode that corresponds to the given ID
     */
    public static EncryptionMode getInstance(int id) {
        EncryptionMode enumInstance = idMap.get(id);
        if (enumInstance == null) {
            throw new IllegalArgumentException("invalid ID: " + id);
        }
        return enumInstance;
    }

}
