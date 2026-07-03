package com.oliveryasuna.mc.rubric.sync.protocol;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.rubric.value.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Binary encoder/decoder for {@link ValueTree}.
 * <p>
 * Wire layout (mirrors Minecraft's {@code PacketByteBuf} conventions: unsigned
 * varints for lengths, zig-zag varlongs for signed numbers, length-prefixed
 * UTF-8 for strings):
 * <pre>
 *   ValueTree     = Section
 *   Section       = TAG_SECTION u8, varint childCount, (utf8 key, Node)*
 *   ListNode      = TAG_LIST    u8, varint length,     Node*
 *   Scalar(null)  = TAG_NULL    u8
 *   Scalar(bool)  = TAG_BOOL    u8, u8 (0|1)
 *   Scalar(long)  = TAG_LONG    u8, zig-zag varlong
 *   Scalar(double)= TAG_DOUBLE  u8, 8 bytes IEEE 754
 *   Scalar(str)   = TAG_STRING  u8, utf8 (varint length + bytes)
 * </pre>
 * Numeric scalars are canonicalised to {@code Long} / {@code Double} before
 * encoding, mirroring the file-format adapters.
 */
public final class ValueTreeWireCodec {

    //==================================================
    // Static fields
    //==================================================

    static final byte TAG_NULL = 0;
    static final byte TAG_BOOL = 1;
    static final byte TAG_LONG = 2;
    static final byte TAG_DOUBLE = 3;
    static final byte TAG_STRING = 4;
    static final byte TAG_LIST = 5;
    static final byte TAG_SECTION = 6;

    //==================================================
    // Static methods
    //==================================================

    public static byte[] encode(final ValueTree tree) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(final DataOutputStream sink = new DataOutputStream(out)) {
            writeNode(sink, tree.root());
        } catch(final IOException impossible) {
            throw new AssertionError("ByteArrayOutputStream does not throw", impossible);
        }
        return out.toByteArray();
    }

    public static ValueTree decode(final byte[] bytes) throws WireFormatException {
        try(final DataInputStream source = new DataInputStream(new ByteArrayInputStream(bytes))) {
            final ValueNode node = readNode(source);
            if(!(node instanceof final Section root)) {
                throw new WireFormatException("expected Section at tree root, got " + node.getClass().getSimpleName());
            }
            if(source.available() > 0) {
                throw new WireFormatException("trailing bytes after tree (" + source.available() + ")");
            }
            return new ValueTree(root);
        } catch(final EOFException truncated) {
            throw new WireFormatException("truncated tree", truncated);
        } catch(final IOException impossible) {
            throw new AssertionError("ByteArrayInputStream does not throw", impossible);
        }
    }

    static void writeNode(
            final DataOutput sink,
            final ValueNode node
    ) throws IOException {
        switch(node) {
            case final Section section -> {
                sink.writeByte(TAG_SECTION);
                final Map<String, ValueNode> children = section.asMap();
                writeVarint(sink, children.size());
                for(final Map.Entry<String, ValueNode> entry : children.entrySet()) {
                    writeString(sink, entry.getKey());
                    writeNode(sink, entry.getValue());
                }
            }
            case ListNode(final List<ValueNode> items) -> {
                sink.writeByte(TAG_LIST);
                writeVarint(sink, items.size());
                for(final ValueNode item : items) {
                    writeNode(sink, item);
                }
            }
            case final Scalar scalar -> writeScalar(sink, scalar);
            case null, default -> throw new IllegalStateException("Unknown ValueNode subtype: " + node);
        }
    }

    static ValueNode readNode(final DataInput source) throws IOException, WireFormatException {
        final byte tag = source.readByte();
        return switch(tag) {
            case TAG_NULL -> new Scalar(null);
            case TAG_BOOL -> new Scalar(source.readByte() != 0);
            case TAG_LONG -> new Scalar(readVarlongZigZag(source));
            case TAG_DOUBLE -> new Scalar(source.readDouble());
            case TAG_STRING -> new Scalar(readString(source));
            case TAG_LIST -> readList(source);
            case TAG_SECTION -> readSection(source);
            default -> throw new WireFormatException("unknown ValueNode tag: " + tag);
        };
    }

    // Section / List
    //--------------------------------------------------

    private static Section readSection(final DataInput source) throws IOException, WireFormatException {
        final int count = readVarint(source);
        final Section section = new Section();
        for(int i = 0; i < count; i++) {
            final String key = readString(source);
            section.put(key, readNode(source));
        }
        return section;
    }

    private static ListNode readList(final DataInput source) throws IOException, WireFormatException {
        final int length = readVarint(source);
        final List<ValueNode> items = new ArrayList<>(length);
        for(int i = 0; i < length; i++) {
            items.add(readNode(source));
        }
        return new ListNode(items);
    }

    // Scalars
    //--------------------------------------------------

    private static void writeScalar(
            final DataOutput sink,
            final Scalar scalar
    ) throws IOException {
        final Object value = scalar.value();
        switch(value) {
            case null -> sink.writeByte(TAG_NULL);
            case final Boolean b -> {
                sink.writeByte(TAG_BOOL);
                sink.writeByte(b ? 1 : 0);
            }
            case final Number n -> {
                final Object canon = canonicalise(n);
                if(canon instanceof final Long l) {
                    sink.writeByte(TAG_LONG);
                    writeVarlongZigZag(sink, l);
                } else {
                    sink.writeByte(TAG_DOUBLE);
                    sink.writeDouble(((Number)canon).doubleValue());
                }
            }
            case final String s -> {
                sink.writeByte(TAG_STRING);
                writeString(sink, s);
            }
            default -> {
                sink.writeByte(TAG_STRING);
                writeString(sink, value.toString());
            }
        }
    }

    private static Object canonicalise(final Number n) {
        if(n instanceof Byte || n instanceof Short || n instanceof Integer || n instanceof Long) {
            return n.longValue();
        }
        if(n instanceof final BigInteger bi) {
            return bi.longValueExact();
        }
        if(n instanceof Float || n instanceof Double) {
            return n.doubleValue();
        }
        if(n instanceof final BigDecimal bd) {
            return bd.doubleValue();
        }
        return n.doubleValue();
    }

    // Varint / varlong / string
    //--------------------------------------------------

    static void writeVarint(
            final DataOutput sink,
            final int value
    ) throws IOException {
        if(value < 0) {
            throw new IOException("varint must be non-negative: " + value);
        }
        int v = value;
        while((v & ~0x7F) != 0) {
            sink.writeByte((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        sink.writeByte(v);
    }

    static int readVarint(final DataInput source) throws IOException, WireFormatException {
        int result = 0;
        int shift = 0;
        for(int i = 0; i < 5; i++) {
            final byte b = source.readByte();
            result |= (b & 0x7F) << shift;
            if((b & 0x80) == 0) {
                if(result < 0) {
                    throw new WireFormatException("varint exceeds positive int range");
                }
                return result;
            }
            shift += 7;
        }
        throw new WireFormatException("varint too long");
    }

    static void writeVarlongZigZag(
            final DataOutput sink,
            final long value
    ) throws IOException {
        long v = (value << 1) ^ (value >> 63);
        while((v & ~0x7FL) != 0L) {
            sink.writeByte(((int)(v & 0x7FL)) | 0x80);
            v >>>= 7;
        }
        sink.writeByte((int)v);
    }

    static long readVarlongZigZag(final DataInput source) throws IOException, WireFormatException {
        long result = 0L;
        int shift = 0;
        for(int i = 0; i < 10; i++) {
            final byte b = source.readByte();
            result |= ((long)(b & 0x7F)) << shift;
            if((b & 0x80) == 0) {
                final long unsigned = result;
                return (unsigned >>> 1) ^ -(unsigned & 1L);
            }
            shift += 7;
        }
        throw new WireFormatException("varlong too long");
    }

    static void writeString(
            final DataOutput sink,
            final String s
    ) throws IOException {
        final byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarint(sink, bytes.length);
        sink.write(bytes);
    }

    static String readString(final DataInput source) throws IOException, WireFormatException {
        final int length = readVarint(source);
        final byte[] bytes = new byte[length];
        source.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    //==================================================
    // Constructors
    //==================================================

    private ValueTreeWireCodec() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
