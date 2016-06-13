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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class AesCrypt {

    private byte[] result;
    private final byte key[];
    private final byte[] IV;
    private final SecretKeySpec skeySpec;
    private final AlgorithmParameterSpec paramSpec;
    private Cipher cipher;

    public AesCrypt(byte[] key, byte[] IV) throws DecodingException {

        try {
            cipher = Cipher.getInstance("AES/CBC/NoPadding");
        } catch (NoSuchAlgorithmException e) {
            throw new DecodingException(e);
        } catch (NoSuchPaddingException e) {
            throw new DecodingException(e);
        }

        this.key = new byte[16];
        this.IV = new byte[16];
        System.arraycopy(key, 0, this.key, 0, 16);
        System.arraycopy(IV, 0, this.IV, 0, 16);
        skeySpec = new SecretKeySpec(this.key, "AES");
        paramSpec = new IvParameterSpec(this.IV);
    }

    public boolean encrypt(byte[] rawData, int length) throws Exception {
        if (length % 16 != 0) {
            throw new Exception("16 not base of message length!");
        }
        boolean valid = false;
        byte tempData[] = new byte[length];
        System.arraycopy(rawData, 0, tempData, 0, length);

        try {
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, paramSpec);
            result = cipher.doFinal(tempData);
            valid = true;
        } catch (InvalidKeyException e) {
            throw new Exception(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new Exception(e);
        } catch (IllegalBlockSizeException e) {
            throw new Exception(e);
        } catch (BadPaddingException e) {
            throw new Exception(e);
        }

        return valid;
    }

    public byte[] getResult() {
        return result;
    }

    public boolean decrypt(byte[] rawData, int length) throws DecodingException {
        boolean valid = false;
        byte encrypted[] = new byte[length];
        System.arraycopy(rawData, 0, encrypted, 0, length);

        try {
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, paramSpec);
            result = cipher.doFinal(encrypted);
            valid = true;
        } catch (InvalidKeyException e) {
            throw new DecodingException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new DecodingException(e);
        } catch (IllegalBlockSizeException e) {
            throw new DecodingException(e);
        } catch (BadPaddingException e) {
            throw new DecodingException(e);
        }
        return valid;
    }

}
