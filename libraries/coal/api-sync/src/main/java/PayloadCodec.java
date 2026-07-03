import com.oliveryasuna.mc.coal.api.sync.SyncPayload;
import com.oliveryasuna.mc.coal.api.sync.WireFormatException;

public interface PayloadCodec {

    //==================================================
    // Methods
    //==================================================

    byte[] encode(SyncPayload payload);

    SyncPayload decode(byte[] bytes) throws WireFormatException;

}
