package com.oliveryasuna.mc.rubric.neoforge;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.rubric.mojang.codec.MojangCodecBridge;
import com.oliveryasuna.mc.rubric.platform.PermissionGate;
import com.oliveryasuna.mc.rubric.platform.Platform;
import com.oliveryasuna.mc.rubric.sync.NetworkTransport;
import com.oliveryasuna.mc.rubric.value.CodecRegistry;
import com.oliveryasuna.mc.rubric.value.codec.ScalarCodec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Factory for loader-specific SPI implementations. Consumer code calls these
 * factories instead of {@code new NeoForgePlatform()} directly so the
 * Fabric/NeoForge variants can be swapped at the source-tree boundary — the
 * {@code fabric} module ships a parallel {@code Loaders} that returns the
 * Fabric implementations.
 */
public final class Loaders {

    //==================================================
    // Static methods
    //==================================================

    public static Platform platform() {
        return new NeoForgePlatform();
    }

    public static PermissionGate permissionGate() {
        return new NeoForgePermissionGate();
    }

    public static NetworkTransport serverTransport() {
        return NeoForgeNetworkTransport.server();
    }

    public static NetworkTransport clientTransport() {
        return NeoForgeNetworkTransport.client();
    }

    /**
     * Registers MC-type codecs into {@code codecs} so consumer configs can
     * declare {@link ResourceLocation} and {@link UUID} fields out of the
     * box. Idempotent; later {@code registerCustom} calls win.
     */
    public static void registerMcCodecs(final CodecRegistry codecs) {
        codecs.registerLeaf(ResourceLocation.class, new ScalarCodec<>(
                ResourceLocation::toString,
                value -> ResourceLocation.parse(String.valueOf(value))
        ));
        codecs.registerLeaf(UUID.class, MojangCodecBridge.from(UUIDUtil.STRING_CODEC));
    }

    //==================================================
    // Constructors
    //==================================================

    private Loaders() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
