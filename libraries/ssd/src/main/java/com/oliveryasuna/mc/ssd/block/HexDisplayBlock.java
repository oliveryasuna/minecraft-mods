package com.oliveryasuna.mc.ssd.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A seven-segment display that shows the hex letters {@code A b C d E F}
 * instead of digits.
 * <p>
 * Redstone mapping: signal 0 blanks; 1-6 show A-F; 7-15 clamp to F.
 */
public final class HexDisplayBlock extends SSDBlock {

    //==================================================
    // Static fields
    //==================================================

    private static final MapCodec<HexDisplayBlock> CODEC = simpleCodec(HexDisplayBlock::new);

    /**
     * {@link SSDBlock#VALUE} index of the first letter ('A').
     */
    private static final int FIRST_LETTER = 10;

    /**
     * Number of letters (A-F).
     */
    private static final int LETTER_COUNT = 6;

    //==================================================
    // Constructors
    //==================================================

    public HexDisplayBlock(final Properties properties) {
        super(properties);
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockState displayFor(
            final BlockState state,
            final int signal
    ) {
        if(signal <= 0) {
            return state.setValue(LIT, false);
        }

        final int letter = FIRST_LETTER + Math.min(signal - 1, LETTER_COUNT - 1);

        return state
                .setValue(LIT, true)
                .setValue(VALUE, letter);
    }

}
