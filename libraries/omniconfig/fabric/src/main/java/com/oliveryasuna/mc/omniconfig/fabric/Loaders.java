package com.oliveryasuna.mc.omniconfig.fabric;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.omniconfig.mojang.codec.MojangCodecBridge;
import com.oliveryasuna.mc.omniconfig.platform.PermissionGate;
import com.oliveryasuna.mc.omniconfig.platform.Platform;
import com.oliveryasuna.mc.omniconfig.sync.NetworkTransport;
import com.oliveryasuna.mc.omniconfig.value.CodecRegistry;
import com.oliveryasuna.mc.omniconfig.value.codec.ScalarCodec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Factory for loader-specific SPI implementations. Consumer code calls these
 * factories instead of {@code new FabricPlatform()} directly so the
 * Fabric/NeoForge variants can be swapped at the source-tree boundary — the
 * {@code neoforge} module ships a parallel {@code Loaders} that returns the
 * NeoForge implementations.
 */
public final class Loaders {

    //==================================================
    // Static methods
    //==================================================

    public static Platform platform() {
        return new FabricPlatform();
    }

    public static PermissionGate permissionGate() {
        return new FabricPermissionGate();
    }

    public static NetworkTransport serverTransport() {
        return FabricNetworkTransport.server();
    }

    public static NetworkTransport clientTransport() {
        return FabricNetworkTransport.client();
    }

    /**
     * Registers MC-type codecs into {@code codecs} so consumer configs can
     * declare {@link ResourceLocation} and {@link UUID} fields out of the
     * box. Idempotent; later {@code registerCustom} calls win.
     * <p>
     * Today: {@link ResourceLocation} via a hand-written
     * {@link ScalarCodec}; {@link UUID} via
     * {@link MojangCodecBridge#from(com.mojang.serialization.Codec)} wrapping
     * {@link UUIDUtil#STRING_CODEC} — the latter doubles as the canonical
     * example of how to expose any Mojang codec as an OmniConfig codec.
     * Tag references ({@code TagKey<T>}) are registry-bound and need a
     * separate registration call per registry type; deferred to a
     * follow-up.
     *
     * @param codecs Registry to extend.
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
