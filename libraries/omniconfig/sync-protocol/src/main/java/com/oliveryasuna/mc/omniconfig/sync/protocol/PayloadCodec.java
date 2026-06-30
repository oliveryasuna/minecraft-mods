package com.oliveryasuna.mc.omniconfig.sync.protocol;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.omniconfig.value.Section;
import com.oliveryasuna.mc.omniconfig.value.ValueNode;
import com.oliveryasuna.mc.omniconfig.value.ValueTree;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Encodes / decodes a {@link SyncPayload} to and from a byte array.
 * <p>
 * Wire layout (mirrors {@link ValueTreeWireCodec}; see that class for varint /
 * varlong / string conventions):
 * <pre>
 *   Payload   = u8 typeTag, varint protocolVersion, Body
 *   Handshake = varint configIdCount, utf8 configId*
 *   Snapshot  = utf8 configId, ValueTree
 *   Delta     = utf8 configId, varint entryCount, (utf8 path, Node)*
 *   ClientEdit= utf8 configId, varint entryCount, (utf8 path, Node)*
 * </pre>
 * Reading checks {@code protocolVersion == ProtocolVersion.CURRENT} and throws
 * {@link WireFormatException} on mismatch — cross-version translation is not
 * attempted; mismatches fail loudly.
 */
public final class PayloadCodec {

    //==================================================
    // Static fields
    //==================================================

    private static final byte TYPE_HANDSHAKE = 0;
    private static final byte TYPE_SNAPSHOT = 1;
    private static final byte TYPE_DELTA = 2;
    private static final byte TYPE_CLIENT_EDIT = 3;

    //==================================================
    // Static methods
    //==================================================

    public static byte[] encode(final SyncPayload payload) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(final DataOutputStream sink = new DataOutputStream(out)) {
            switch(payload) {
                case SyncPayload.Handshake h -> encodeHandshake(sink, h);
                case SyncPayload.Snapshot s -> encodeSnapshot(sink, s);
                case SyncPayload.Delta d -> encodeDelta(sink, d);
                case SyncPayload.ClientEdit e -> encodeClientEdit(sink, e);
            }
        } catch(final IOException impossible) {
            throw new AssertionError("ByteArrayOutputStream does not throw", impossible);
        }
        return out.toByteArray();
    }

    public static SyncPayload decode(final byte[] bytes) throws WireFormatException {
        try(final DataInputStream source = new DataInputStream(new ByteArrayInputStream(bytes))) {
            final byte type = source.readByte();
            final int version = ValueTreeWireCodec.readVarint(source);
            if(version != ProtocolVersion.CURRENT) {
                throw new WireFormatException(
                        "protocol version mismatch: payload v" + version + ", local v" + ProtocolVersion.CURRENT
                );
            }
            final SyncPayload payload = switch(type) {
                case TYPE_HANDSHAKE -> decodeHandshake(source, version);
                case TYPE_SNAPSHOT -> decodeSnapshot(source);
                case TYPE_DELTA -> decodeDelta(source);
                case TYPE_CLIENT_EDIT -> decodeClientEdit(source);
                default -> throw new WireFormatException("unknown payload type: " + type);
            };
            if(source.available() > 0) {
                throw new WireFormatException("trailing bytes after payload (" + source.available() + ")");
            }
            return payload;
        } catch(final EOFException truncated) {
            throw new WireFormatException("truncated payload", truncated);
        } catch(final IOException impossible) {
            throw new AssertionError("ByteArrayInputStream does not throw", impossible);
        }
    }

    // Encode bodies
    //--------------------------------------------------

    private static void encodeHandshake(
            final DataOutput sink,
            final SyncPayload.Handshake handshake
    ) throws IOException {
        sink.writeByte(TYPE_HANDSHAKE);
        ValueTreeWireCodec.writeVarint(sink, handshake.protocolVersion());
        ValueTreeWireCodec.writeVarint(sink, handshake.configIds().size());
        for(final String id : handshake.configIds()) {
            ValueTreeWireCodec.writeString(sink, id);
        }
    }

    private static void encodeSnapshot(
            final DataOutput sink,
            final SyncPayload.Snapshot snapshot
    ) throws IOException {
        sink.writeByte(TYPE_SNAPSHOT);
        ValueTreeWireCodec.writeVarint(sink, ProtocolVersion.CURRENT);
        ValueTreeWireCodec.writeString(sink, snapshot.configId());
        ValueTreeWireCodec.writeNode(sink, snapshot.tree().root());
    }

    private static void encodeDelta(
            final DataOutput sink,
            final SyncPayload.Delta delta
    ) throws IOException {
        sink.writeByte(TYPE_DELTA);
        ValueTreeWireCodec.writeVarint(sink, ProtocolVersion.CURRENT);
        ValueTreeWireCodec.writeString(sink, delta.configId());
        ValueTreeWireCodec.writeVarint(sink, delta.entries().size());
        for(final SyncPayload.Delta.Entry entry : delta.entries()) {
            ValueTreeWireCodec.writeString(sink, entry.path());
            ValueTreeWireCodec.writeNode(sink, entry.value());
        }
    }

    private static void encodeClientEdit(
            final DataOutput sink,
            final SyncPayload.ClientEdit edit
    ) throws IOException {
        sink.writeByte(TYPE_CLIENT_EDIT);
        ValueTreeWireCodec.writeVarint(sink, ProtocolVersion.CURRENT);
        ValueTreeWireCodec.writeString(sink, edit.configId());
        ValueTreeWireCodec.writeVarint(sink, edit.entries().size());
        for(final SyncPayload.Delta.Entry entry : edit.entries()) {
            ValueTreeWireCodec.writeString(sink, entry.path());
            ValueTreeWireCodec.writeNode(sink, entry.value());
        }
    }

    // Decode bodies
    //--------------------------------------------------

    private static SyncPayload.Handshake decodeHandshake(
            final DataInput source,
            final int version
    ) throws IOException, WireFormatException {
        final int count = ValueTreeWireCodec.readVarint(source);
        final List<String> ids = new ArrayList<>(count);
        for(int i = 0; i < count; i++) {
            ids.add(ValueTreeWireCodec.readString(source));
        }
        return new SyncPayload.Handshake(version, ids);
    }

    private static SyncPayload.Snapshot decodeSnapshot(final DataInput source) throws IOException, WireFormatException {
        final String configId = ValueTreeWireCodec.readString(source);
        final ValueNode root = ValueTreeWireCodec.readNode(source);
        if(!(root instanceof final Section section)) {
            throw new WireFormatException("snapshot root must be Section, got " + root.getClass().getSimpleName());
        }
        return new SyncPayload.Snapshot(configId, new ValueTree(section));
    }

    private static SyncPayload.Delta decodeDelta(final DataInput source) throws IOException, WireFormatException {
        final String configId = ValueTreeWireCodec.readString(source);
        final int count = ValueTreeWireCodec.readVarint(source);
        final List<SyncPayload.Delta.Entry> entries = new ArrayList<>(count);
        for(int i = 0; i < count; i++) {
            final String path = ValueTreeWireCodec.readString(source);
            final ValueNode value = ValueTreeWireCodec.readNode(source);
            entries.add(new SyncPayload.Delta.Entry(path, value));
        }
        return new SyncPayload.Delta(configId, entries);
    }

    private static SyncPayload.ClientEdit decodeClientEdit(final DataInput source) throws IOException, WireFormatException {
        final String configId = ValueTreeWireCodec.readString(source);
        final int count = ValueTreeWireCodec.readVarint(source);
        final List<SyncPayload.Delta.Entry> entries = new ArrayList<>(count);
        for(int i = 0; i < count; i++) {
            final String path = ValueTreeWireCodec.readString(source);
            final ValueNode value = ValueTreeWireCodec.readNode(source);
            entries.add(new SyncPayload.Delta.Entry(path, value));
        }
        return new SyncPayload.ClientEdit(configId, entries);
    }

    //==================================================
    // Constructors
    //==================================================

    private PayloadCodec() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
