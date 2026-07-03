package com.oliveryasuna.mc.rubric.fabric.gui;

import com.oliveryasuna.mc.rubric.api.ConfigManager;
import com.oliveryasuna.mc.rubric.api.annotation.Widget;
import com.oliveryasuna.mc.rubric.loader.RubricSelf;
import com.oliveryasuna.mc.rubric.loader.gui.ScreenBuildContext;
import com.oliveryasuna.mc.rubric.schema.EntryMetadata;
import com.oliveryasuna.mc.rubric.schema.Schema;
import com.oliveryasuna.mc.rubric.schema.SchemaCategory;
import com.oliveryasuna.mc.rubric.schema.SchemaEntry;
import com.oliveryasuna.mc.rubric.validation.validator.OneOfValidator;
import com.oliveryasuna.mc.rubric.validation.validator.RangeValidator;
import com.oliveryasuna.mc.rubric.value.ValueType;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class YaclScreenProvider implements ScreenProvider {

    //==================================================
    // Static methods
    //==================================================

    private static OptionDescription description(final EntryMetadata meta) {
        final OptionDescription.Builder b = OptionDescription.createBuilder();
        if(!meta.getComment().isEmpty()) {
            b.text(Component.literal(String.join("\n", meta.getComment())));
        }

        return b.build();
    }

    private static Function<String, Object> stringForLeaf(final Class<?> raw) {
        // Placeholder: future codec-round-trip hook. For now the staged value
        // is the raw String the user typed, and manager.set will throw if it
        // doesn't decode — which surfaces as a YACL "save failed" toast.
        return s -> s;
    }

    //==================================================
    // Constructors
    //==================================================

    public YaclScreenProvider() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public String id() {
        return "yacl";
    }

    @Override
    public Screen create(
            final Minecraft client,
            final Screen parent,
            final ConfigManager<?> manager
    ) {
        final Schema schema = manager.getSchema();
        // Staged values live until the user clicks Save & Exit. Each entry's
        // setter writes here; cancel = discard, save = flush via manager.set
        // (which validates) then manager.save (which persists).
        final ScreenBuildContext ctx = new ScreenBuildContext(
                manager,
                RubricSelf.config().gui.showMetadataSuffixes,
                RubricSelf.config().gui.defaultSliderTicks
        );

        final YetAnotherConfigLib.Builder ycl = YetAnotherConfigLib.createBuilder()
                .title(Component.literal(schema.name()));

        final SchemaCategory root = schema.root();
        // Top-level entries belong to no @Category; bucket them under a
        // "General" tab keyed by the schema id so multi-tab nav still works.
        if(!root.entries().isEmpty()) {
            final OptionGroup.Builder rootGroup = OptionGroup.createBuilder()
                    .name(Component.literal("General"))
                    .collapsed(false);
            for(final SchemaEntry entry : root.entries()) {
                addEntryAsOption(rootGroup, entry, entry.getKey(), ctx);
            }
            ycl.category(ConfigCategory.createBuilder()
                    .name(Component.literal("General"))
                    .group(rootGroup.build())
                    .build());
        }
        for(final SchemaCategory child : root.categories()) {
            ycl.category(buildCategory(child, child.getName(), ctx));
        }

        ycl.save(ctx::flushUnchecked);

        return ycl.build().generateScreen(parent);
    }

    private ConfigCategory buildCategory(
            final SchemaCategory cat,
            final String pathPrefix,
            final ScreenBuildContext ctx
    ) {
        final ConfigCategory.Builder b = ConfigCategory.createBuilder()
                .name(Component.literal(cat.getName()));

        if(!cat.getComment().isEmpty()) {
            b.tooltip(Component.literal(String.join("\n", cat.getComment())));
        }

        // Direct entries in this category → one un-collapsed group.
        if(!cat.entries().isEmpty()) {
            final OptionGroup.Builder grp = OptionGroup.createBuilder()
                    .name(Component.literal(cat.getName()))
                    .collapsed(false);
            for(final SchemaEntry entry : cat.entries()) {
                addEntryAsOption(grp, entry, pathPrefix + "." + entry.getKey(), ctx);
            }
            b.group(grp.build());
        }

        // Each nested SchemaCategory becomes a collapsible OptionGroup. Deeper
        // nesting collapses into one flat group per branch — YACL groups don't
        // recurse, so we flatten naming with the dotted prefix.
        for(final SchemaCategory child : cat.categories()) {
            final OptionGroup.Builder grp = OptionGroup.createBuilder()
                    .name(Component.literal(child.getName()))
                    .collapsed(false);
            flattenInto(grp, child, pathPrefix + "." + child.getName(), ctx);
            b.group(grp.build());
        }

        return b.build();
    }

    private void flattenInto(
            final OptionGroup.Builder grp,
            final SchemaCategory cat,
            final String pathPrefix,
            final ScreenBuildContext ctx
    ) {
        for(final SchemaEntry entry : cat.entries()) {
            addEntryAsOption(grp, entry, pathPrefix + "." + entry.getKey(), ctx);
        }
        for(final SchemaCategory child : cat.categories()) {
            flattenInto(grp, child, pathPrefix + "." + child.getName(), ctx);
        }
    }

    private void addEntryAsOption(
            final OptionGroup.Builder grp,
            final SchemaEntry entry,
            final String path,
            final ScreenBuildContext ctx
    ) {
        final EntryMetadata meta = entry.getMetadata();
        if(meta.isHidden()) {
            return;
        }

        final Option<?> option = buildOption(entry, path, ctx);
        if(option != null) {
            grp.option(option);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Option<?> buildOption(
            final SchemaEntry entry,
            final String path,
            final ScreenBuildContext ctx
    ) {
        final ValueType type = entry.getType();
        final Class<?> raw = type.getRawType();

        return switch(type.getKind()) {
            case SCALAR -> buildScalarOption(entry, path, ctx, raw);
            case ENUM -> buildEnumOption(entry, path, ctx, (Class)raw);
            case LIST, MAP, OBJECT -> buildPlaceholderOption(entry, path);
        };
    }

    private Option<?> buildScalarOption(
            final SchemaEntry entry,
            final String path,
            final ScreenBuildContext ctx,
            final Class<?> raw
    ) {
        final EntryMetadata meta = entry.getMetadata();

        if(raw == boolean.class || raw == Boolean.class) {
            return scalarOption(Boolean.class, entry, path, ctx,
                    opt -> BooleanControllerBuilder.create(opt)
                            .yesNoFormatter()
                            .coloured(true));
        } else if(raw == String.class) {
            final Optional<OneOfValidator> oneOf = ScreenProviders.findOneOf(meta);
            if(oneOf.isPresent()) {
                final List<String> allowed = new ArrayList<>(oneOf.get().allowed());

                return scalarOption(String.class, entry, path, ctx,
                        opt -> dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder.create(opt)
                                .values(allowed)
                                .allowAnyValue(false));
            }

            if(meta.getWidget() == Widget.Type.COLOR) {
                return buildColorOption(entry, path, ctx);
            }

            return scalarOption(String.class, entry, path, ctx, StringControllerBuilder::create);
        } else if(raw == int.class || raw == Integer.class) {
            final Optional<RangeValidator> r = ScreenProviders.findRange(meta);
            if(ScreenProviders.useSlider(meta, r)) {
                final RangeValidator rv = r.get();
                return scalarOption(Integer.class, entry, path, ctx,
                        opt -> IntegerSliderControllerBuilder.create(opt)
                                .range((int)Math.round(rv.min()), (int)Math.round(rv.max()))
                                .step(1));
            }

            return scalarOption(Integer.class, entry, path, ctx, IntegerFieldControllerBuilder::create);
        } else if(raw == long.class || raw == Long.class) {
            final Optional<RangeValidator> r = ScreenProviders.findRange(meta);
            if(ScreenProviders.useSlider(meta, r)) {
                final RangeValidator rv = r.get();
                return scalarOption(Long.class, entry, path, ctx,
                        opt -> LongSliderControllerBuilder.create(opt)
                                .range(Math.round(rv.min()), Math.round(rv.max()))
                                .step(1L));
            }

            return scalarOption(Long.class, entry, path, ctx, LongFieldControllerBuilder::create);
        } else if(raw == double.class || raw == Double.class) {
            final Optional<RangeValidator> r = ScreenProviders.findRange(meta);
            if(ScreenProviders.useSlider(meta, r)) {
                final RangeValidator rv = r.get();
                return scalarOption(Double.class, entry, path, ctx,
                        opt -> DoubleSliderControllerBuilder.create(opt)
                                .range(rv.min(), rv.max())
                                .step(ScreenProviders.sliderStep(rv.min(), rv.max(), ctx.getDefaultSliderTicks())));
            }

            return scalarOption(Double.class, entry, path, ctx, DoubleFieldControllerBuilder::create);
        } else if(raw == float.class || raw == Float.class) {
            final Optional<RangeValidator> r = ScreenProviders.findRange(meta);
            if(ScreenProviders.useSlider(meta, r)) {
                final RangeValidator rv = r.get();
                return scalarOption(Float.class, entry, path, ctx,
                        opt -> FloatSliderControllerBuilder.create(opt)
                                .range((float)rv.min(), (float)rv.max())
                                .step((float)ScreenProviders.sliderStep(rv.min(), rv.max(), ctx.getDefaultSliderTicks())));
            }

            return scalarOption(Float.class, entry, path, ctx, FloatFieldControllerBuilder::create);
        }

        // Codec-mediated leaves (UUID/Instant/Duration/ResourceLocation/etc.):
        // render as String via toString round-trip. Real codec round-trip is
        // future work — ScalarCodec.encode/decode through the entry's
        // CodecRegistry would let edits flow through validation cleanly.
        return scalarOption(String.class, entry, path, ctx,
                StringControllerBuilder::create,
                () -> String.valueOf(entry.readFrom(ctx.getManager().get())),
                () -> entry.getDefaultValue() == null ? "" : entry.getDefaultValue().toString(),
                stringForLeaf(raw));
    }

    private <E extends Enum<E>> Option<?> buildEnumOption(
            final SchemaEntry entry,
            final String path,
            final ScreenBuildContext ctx,
            final Class<E> enumClass
    ) {
        return scalarOption(enumClass, entry, path, ctx, opt -> EnumControllerBuilder.create(opt).enumClass(enumClass));
    }

    private Option<?> buildColorOption(
            final SchemaEntry entry,
            final String path,
            final ScreenBuildContext ctx
    ) {
        final EntryMetadata meta = entry.getMetadata();
        // Shared helpers work in packed-int RGB; YACL's ColorController binds
        // java.awt.Color, so wrap on read and unpack on write.
        final int defRgb = ScreenProviders.parseColor((String)entry.getDefaultValue(), 0xFFFFFF);
        final String defString = entry.getDefaultValue() == null ? "" : entry.getDefaultValue().toString();
        final Color def = new Color(defRgb);

        return Option.<Color>createBuilder()
                .name(ScreenProviders.displayName(entry, meta, ctx.isShowMetadataSuffixes()))
                .description(description(meta))
                .binding(
                        def,
                        () -> new Color(ScreenProviders.parseColor(ctx.currentOrDefault(path, entry, defString), defRgb)),
                        v -> ctx.stage(path, ScreenProviders.formatColor(v.getRGB() & 0xFFFFFF))
                )
                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(false))
                .build();
    }

    private Option<?> buildPlaceholderOption(
            final SchemaEntry entry,
            final String path
    ) {
        // Lists / maps / nested objects render as a disabled label until
        // dedicated YACL ListOption / nested-screen wiring lands. Skipping them
        // entirely would hide their existence from users editing the GUI; a
        // visible disabled row keeps discoverability.
        final EntryMetadata meta = entry.getMetadata();
        return Option.<String>createBuilder()
                .name(Component.literal(entry.getKey() + " (edit on disk)")
                        .withStyle(ChatFormatting.GRAY))
                .description(description(meta))
                .binding("(complex)", () -> "(complex)", v -> {
                })
                .controller(StringControllerBuilder::create)
                .available(false)
                .build();
    }

    // Per-type scalar option builder. typeClass is what YACL binds & the staged
    // map stores; manager.set will receive the same.
    private <T> Option<T> scalarOption(
            final Class<T> typeClass,
            final SchemaEntry entry,
            final String path,
            final ScreenBuildContext ctx,
            final Function<Option<T>, ? extends ControllerBuilder<T>> controller
    ) {
        return scalarOption(typeClass, entry, path, ctx, controller, null, null, null);
    }

    private <T> Option<T> scalarOption(
            final Class<T> typeClass,
            final SchemaEntry entry,
            final String path,
            final ScreenBuildContext ctx,
            final Function<Option<T>, ? extends ControllerBuilder<T>> controller,
            final java.util.function.Supplier<T> getterOverride,
            final java.util.function.Supplier<T> defaultOverride,
            final java.util.function.Function<T, Object> setterAdapter
    ) {
        final EntryMetadata meta = entry.getMetadata();
        final T defaultValue = defaultOverride != null
                ? defaultOverride.get()
                : typeClass.cast(ScreenProviders.coerceNullDefault(entry.getDefaultValue(), typeClass));

        return Option.<T>createBuilder()
                .name(ScreenProviders.displayName(entry, meta, ctx.isShowMetadataSuffixes()))
                .description(description(meta))
                .binding(
                        defaultValue,
                        () -> getterOverride != null
                                ? getterOverride.get()
                                : ctx.currentOrDefault(path, entry, defaultValue),
                        v -> ctx.stage(path, setterAdapter != null ? setterAdapter.apply(v) : v))
                .controller(controller::apply)
                .build();
    }

}
