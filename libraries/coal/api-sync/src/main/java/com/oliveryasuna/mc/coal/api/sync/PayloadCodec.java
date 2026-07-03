package com.oliveryasuna.mc.coal.api.sync;

public interface PayloadCodec {

    //==================================================
    // Methods
    //==================================================

    byte[] encode(SyncPayload payload);

    SyncPayload decode(byte[] bytes) throws WireFormatException;

}
