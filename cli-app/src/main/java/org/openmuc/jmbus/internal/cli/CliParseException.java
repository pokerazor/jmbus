/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus.internal.cli;

public final class CliParseException extends Exception {

    private static final long serialVersionUID = -5162894897245715377L;

    public CliParseException() {
        super();
    }

    public CliParseException(String s) {
        super(s);
    }

    public CliParseException(Throwable cause) {
        super(cause);
    }

    public CliParseException(String s, Throwable cause) {
        super(s, cause);
    }

}
