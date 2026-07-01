package com.oliveryasuna.mc.rubric.value;

/**
 * Format-neutral parsed value.
 * <p>
 * The seam between the {@code io}/serialization layer (which converts bytes
 * &lt;-&gt; {@link ValueNode}) and the codec layer (which converts
 * {@link ValueNode} &lt;-&gt; Java values). Carries data only.
 */
public sealed interface ValueNode permits Scalar, ListNode, Section {

}
