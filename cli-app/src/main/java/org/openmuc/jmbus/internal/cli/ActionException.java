/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus.internal.cli;

public final class ActionException extends Exception {

    private static final long serialVersionUID = 4806947065917148946L;

    public ActionException() {
        super();
    }

    public ActionException(String s) {
        super(s);
    }

    public ActionException(Throwable cause) {
        super(cause);
    }

    public ActionException(String s, Throwable cause) {
        super(s, cause);
    }

}
