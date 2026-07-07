package com.oliveryasuna.mc.ssd.display;

import com.mojang.serialization.Codec;
import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.ssd.SSDMod;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of the built-in {@link DisplayType}s and the {@link Codec} that
 * (de)serializes one by id (used by the display block's block codec).
 */
public final class DisplayTypes {

    //==================================================
    // Static fields
    //==================================================

    private static final Map<ResourceLocation, DisplayType> REGISTRY = new LinkedHashMap<>();

    /**
     * Decimal display: 0-9.
     */
    public static final DisplayType DIGIT = register(new DisplayType(id("digit"), DigitMapping.INSTANCE));

    /**
     * Hex-letter display: A-F.
     */
    public static final DisplayType HEX = register(new DisplayType(id("hex"), HexLetterMapping.INSTANCE));

    public static final Codec<DisplayType> CODEC = ResourceLocation.CODEC.xmap(DisplayTypes::byId, DisplayType::id);

    //==================================================
    // Static methods
    //==================================================

    public static DisplayType byId(final ResourceLocation id) {
        final DisplayType type = REGISTRY.get(id);

        if(type == null) {
            throw new IllegalArgumentException("Unknown display type: " + id);
        }

        return type;
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(SSDMod.MOD_ID, path);
    }

    private static DisplayType register(final DisplayType type) {
        REGISTRY.put(type.id(), type);

        return type;
    }

    //==================================================
    // Constructors
    //==================================================

    private DisplayTypes() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
