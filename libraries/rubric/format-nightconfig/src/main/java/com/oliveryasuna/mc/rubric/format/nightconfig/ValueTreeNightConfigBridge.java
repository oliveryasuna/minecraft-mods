package com.oliveryasuna.mc.rubric.format.nightconfig;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.rubric.value.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts between core's {@link ValueTree} and NightConfig's
 * {@link CommentedConfig}.
 * <p>
 * <strong>Mapping (read: NightConfig -&gt; ValueTree):</strong>
 * <ul>
 *     <li>
 *         {@code Config} / {@code CommentedConfig} -&gt; {@link Section},
 *         preserving entry order.
 *     </li>
 *     <li>
 *         {@code List<?>} -&gt; {@link ListNode}, elements mapped recursively.
 *     </li>
 *     <li>
 *         {@code String} / {@code Boolean} / {@code Number} -&gt;
 *         {@link Scalar}.
 *     </li>
 *     <li>
 *         {@code null} -&gt; {@code Scalar(null)} (core treats absent + null
 *         identically).
 *     </li>
 *     <li>
 *         Anything else -&gt; {@code Scalar(value.toString())} (defensive
 *         fallback).
 *     </li>
 * </ul>
 * <p>
 * <strong>Mapping (write: ValueTree -&gt; NightConfig):</strong>
 * <ul>
 *     <li>
 *         {@link Section} -&gt; nested {@link CommentedConfig#inMemory()}.
 *     </li>
 *     <li>
 *         {@link ListNode} -&gt; {@link ArrayList}, elements mapped
 *         recursively.
 *     </li>
 *     <li>
 *         {@code Scalar(Boolean|String)} -&gt; raw value as-is.
 *     </li>
 *     <li>
 *         Integer-valued {@link Number} (Byte/Short/Integer/Long/BigInteger)
 *         -&gt; {@code Long}.
 *     </li>
 *     <li>
 *         Floating {@link Number} (Float/Double/BigDecimal) -&gt;
 *         {@code Double}.
 *     </li>
 *     <li>
 *         {@code Scalar(null)} as a section child -&gt; key is omitted
 *         (preserves the "absent == default" contract documented at
 *         {@code core/value/ValueTreeMapper.java}). Inside a {@link ListNode},
 *         null elements are emitted as {@code null} since TOML arrays have no
 *         notion of omission; the codec layer is responsible for rejecting
 *         nulls in structured element types.
 *     </li>
 * </ul>
 * <p>
 * Iteration order on read follows NightConfig's underlying map (insertion
 * order for {@code inMemory()}). On write, the tree is already schema-ordered
 * by {@code ValueTreeMapper.toTree}, so we simply walk it.
 */
public final class ValueTreeNightConfigBridge {

    //==================================================
    // Static methods
    //==================================================

    public static ValueTree fromCommentedConfig(final CommentedConfig source) {
        return new ValueTree(readSection(source));
    }

    public static CommentedConfig toCommentedConfig(final ValueTree tree) {
        final CommentedConfig destination = CommentedConfig.inMemory();
        writeSection(tree.root(), destination);
        return destination;
    }

    // Read (NightConfig -> ValueTree)
    //--------------------------------------------------

    private static Section readSection(final UnmodifiableConfig source) {
        final Section section = new Section();
        for(final UnmodifiableConfig.Entry entry : source.entrySet()) {
            section.put(entry.getKey(), readNode(entry.getRawValue()));
        }
        return section;
    }

    private static ValueNode readNode(final Object value) {
        switch(value) {
            case null -> {
                return new Scalar(null);
            }
            case final UnmodifiableConfig nested -> {
                return readSection(nested);
            }
            case final List<?> list -> {
                final List<ValueNode> items = new ArrayList<>(list.size());
                for(final Object item : list) {
                    items.add(readNode(item));
                }
                return new ListNode(items);
            }
            case final Number number -> {
                return new Scalar(coerce(number));
            }
            default -> {
            }
        }
        if(value instanceof String || value instanceof Boolean) {
            return new Scalar(value);
        }
        return new Scalar(value.toString());
    }

    // Write (ValueTree -> NightConfig)
    //--------------------------------------------------

    private static void writeSection(
            final Section source,
            final Config destination
    ) {
        for(final Map.Entry<String, ValueNode> entry : source.asMap().entrySet()) {
            final ValueNode node = entry.getValue();
            if(node instanceof Scalar(final Object value) && value == null) {
                continue;
            }
            destination.set(List.of(entry.getKey()), toRaw(node));
        }
    }

    private static Object toRaw(final ValueNode node) {
        if(node instanceof Scalar(final Object value)) {
            return coerce(value);
        }
        if(node instanceof ListNode(final List<ValueNode> items)) {
            final List<Object> out = new ArrayList<>(items.size());
            for(final ValueNode item : items) {
                out.add(item instanceof Scalar(final Object value) && value == null ? null : toRaw(item));
            }
            return out;
        }
        if(node instanceof final Section section) {
            final CommentedConfig nested = CommentedConfig.inMemory();
            writeSection(section, nested);
            return nested;
        }
        throw new IllegalStateException("Unknown ValueNode subtype: " + node);
    }

    private static Object coerce(final Object value) {
        if(value == null) {
            return null;
        }
        if(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return ((Number)value).longValue();
        }
        if(value instanceof final BigInteger bi) {
            return bi.longValueExact();
        }
        if(value instanceof Float || value instanceof Double) {
            return ((Number)value).doubleValue();
        }
        if(value instanceof final BigDecimal bd) {
            return bd.doubleValue();
        }
        return value;
    }

    //==================================================
    // Constructors
    //==================================================

    private ValueTreeNightConfigBridge() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
