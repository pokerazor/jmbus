/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

public class DecodingException extends Exception {

    private static final long serialVersionUID = 1735527302166708223L;

    public DecodingException() {
        super();
    }

    public DecodingException(String s) {
        super(s);
    }

    public DecodingException(Throwable cause) {
        super(cause);
    }

    public DecodingException(String s, Throwable cause) {
        super(s, cause);
    }

}
