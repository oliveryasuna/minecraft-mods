package com.oliveryasuna.mc.omniconfig.sync.protocol;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;

/**
 * Wire-protocol version carried in every {@link SyncPayload}.
 * <p>
 * Versioning policy:
 * <ul>
 *     <li>
 *         A wire-incompatible change (new tag value, reshaped header, removed
 *         field) <strong>must</strong> bump {@link #CURRENT}.
 *     </li>
 *     <li>
 *         Two endpoints with a different {@link #CURRENT} fail the handshake
 *         with a readable message; we do not attempt cross-version translation.
 *     </li>
 *     <li>
 *         {@code com.oliveryasuna.mc.omniconfig.api.Format} evolution (e.g.
 *         adding a new {@code ValueNode} subtype) is treated as a wire change
 *         and bumps this number.
 *     </li>
 * </ul>
 */
public final class ProtocolVersion {

    //==================================================
    // Static fields
    //==================================================

    /**
     * Current wire-protocol version. Bump on any wire-incompatible change.
     */
    public static final int CURRENT = 1;

    //==================================================
    // Constructors
    //==================================================

    private ProtocolVersion() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
