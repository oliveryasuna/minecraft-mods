package com.oliveryasuna.mc.omniconfig.api;

import com.oliveryasuna.mc.omniconfig.api.annotation.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class ApiContractTest {

    //==================================================
    // Static fields
    //==================================================

    private static final List<Class<? extends Annotation>> ALL_ANNOTATIONS = List.of(
            Category.class,
            Comment.class,
            Config.class,
            Hidden.class,
            Key.class,
            Length.class,
            NotNull.class,
            OneOf.class,
            Pattern.class,
            Range.class,
            Reload.class,
            RequiresRestart.class,
            Sync.class,
            Widget.class
    );

    //==================================================
    // Constructors
    //==================================================

    ApiContractTest() {
        super();
    }

    //==================================================
    // Nested
    //==================================================

    @Nested
    @DisplayName("Meta-annotation contract")
    final class MetaContract {

        //==================================================
        // Static methods
        //==================================================

        private void assertTargets(
                final Class<? extends Annotation> a,
                final Set<ElementType> expected
        ) {
            final Target target = a.getAnnotation(Target.class);
            assertNotNull(target, a.getSimpleName() + " missing @Target");
            assertEquals(expected, Set.of(target.value()), a.getSimpleName() + " @Target mismatch");
        }

        //==================================================
        // Constructors
        //==================================================

        MetaContract() {
            super();
        }

        //==================================================
        // Tests
        //==================================================

        @Test
        void everyAnnotationIsRuntimeRetained() {
            for(final Class<? extends Annotation> a : ALL_ANNOTATIONS) {
                final Retention retention = a.getAnnotation(Retention.class);
                assertNotNull(retention, a.getSimpleName() + " missing @Retention");
                assertEquals(
                        RetentionPolicy.RUNTIME,
                        retention.value(),
                        a.getSimpleName() + " must be RUNTIME-retained (schema reader uses reflection)");
            }
        }

        @Test
        void everyAnnotationIsDocumented() {
            for(final Class<? extends Annotation> a : ALL_ANNOTATIONS) {
                assertNotNull(
                        a.getAnnotation(Documented.class),
                        a.getSimpleName() + " missing @Documented (annotation appears in user-facing Javadoc)");
            }
        }

        @Test
        void targetsArePinned() {
            assertTargets(Config.class, Set.of(ElementType.TYPE));
            assertTargets(Category.class, Set.of(ElementType.FIELD, ElementType.TYPE));
            assertTargets(Comment.class, Set.of(ElementType.FIELD, ElementType.TYPE));
            assertTargets(Hidden.class, Set.of(ElementType.FIELD, ElementType.TYPE));
            assertTargets(Reload.class, Set.of(ElementType.FIELD, ElementType.TYPE));
            assertTargets(Sync.class, Set.of(ElementType.FIELD, ElementType.TYPE));
            assertTargets(Key.class, Set.of(ElementType.FIELD));
            assertTargets(Length.class, Set.of(ElementType.FIELD));
            assertTargets(NotNull.class, Set.of(ElementType.FIELD));
            assertTargets(OneOf.class, Set.of(ElementType.FIELD));
            assertTargets(Pattern.class, Set.of(ElementType.FIELD));
            assertTargets(Range.class, Set.of(ElementType.FIELD));
            assertTargets(RequiresRestart.class, Set.of(ElementType.FIELD));
            assertTargets(Widget.class, Set.of(ElementType.FIELD));
        }

    }

    @Nested
    @DisplayName("Default values are pinned (public contract)")
    final class Defaults {

        //==================================================
        // Static methods
        //==================================================

        private Object defaultOf(
                final Class<? extends Annotation> a,
                final String member
        ) throws NoSuchMethodException {
            final Method m = a.getDeclaredMethod(member);
            final Object def = m.getDefaultValue();
            assertNotNull(def, a.getSimpleName() + "#" + member + " expected a default value");

            return def;
        }

        private void assertNoDefault(
                final Class<? extends Annotation> a,
                final String member
        ) throws NoSuchMethodException {
            final Method m = a.getDeclaredMethod(member);
            assertNull(m.getDefaultValue(), a.getSimpleName() + "#" + member + " must remain required (no default)");
        }

        //==================================================
        // Constructors
        //==================================================

        Defaults() {
            super();
        }

        //==================================================
        // Tests
        //==================================================

        @Test
        void configDefaults() throws NoSuchMethodException {
            assertEquals("config", defaultOf(Config.class, "name"));
            assertEquals(Format.TOML, defaultOf(Config.class, "format"));
            assertEquals(1, defaultOf(Config.class, "version"));
        }

        @Test
        void rangeDefaultsCoverFullDoubleDomain() throws NoSuchMethodException {
            assertEquals(Double.NEGATIVE_INFINITY, defaultOf(Range.class, "min"));
            assertEquals(Double.POSITIVE_INFINITY, defaultOf(Range.class, "max"));
        }

        @Test
        void lengthDefaultsCoverFullIntDomain() throws NoSuchMethodException {
            assertEquals(0, defaultOf(Length.class, "min"));
            assertEquals(Integer.MAX_VALUE, defaultOf(Length.class, "max"));
        }

        @Test
        void reloadDefaultIsWorld() throws NoSuchMethodException {
            assertEquals(Reload.Tier.WORLD, defaultOf(Reload.class, "value"));
        }

        @Test
        void syncDefaultIsClient() throws NoSuchMethodException {
            assertEquals(Sync.Scope.CLIENT, defaultOf(Sync.class, "value"));
        }

        @Test
        void widgetDefaults() throws NoSuchMethodException {
            assertEquals(Widget.Type.AUTO, defaultOf(Widget.class, "value"));
            assertEquals(false, defaultOf(Widget.class, "allowInvalid"));
        }

        @Test
        void configIdIsRequired() throws NoSuchMethodException {
            assertNoDefault(Config.class, "id");
        }

        @Test
        void requiredValueMembers() throws NoSuchMethodException {
            assertNoDefault(Category.class, "value");
            assertNoDefault(Comment.class, "value");
            assertNoDefault(Key.class, "value");
            assertNoDefault(OneOf.class, "value");
            assertNoDefault(Pattern.class, "value");
        }

    }

    @Nested
    @DisplayName("Enum constant identity (wire / on-disk contract)")
    final class EnumIdentity {

        //==================================================
        // Static methods
        //==================================================

        private <E extends Enum<E>> void assertEnumNames(
                final Class<E> enumType,
                final List<String> expected
        ) {
            final E[] values = enumType.getEnumConstants();
            assertEquals(expected.size(), values.length, enumType.getSimpleName() + " constant count drifted");
            for(int i = 0; i < expected.size(); i++) {
                assertEquals(expected.get(i), values[i].name(), enumType.getSimpleName() + " constant at index " + i + " changed name or order");
            }
        }

        //==================================================
        // Constructors
        //==================================================

        EnumIdentity() {
            super();
        }

        //==================================================
        // Tests
        //==================================================

        @Test
        void formatConstants() {
            assertEnumNames(Format.class, List.of("TOML", "JSON", "JSON5"));
        }

        @Test
        void reloadTierConstants() {
            assertEnumNames(Reload.Tier.class, List.of("LIVE", "WORLD", "RESTART"));
        }

        @Test
        void syncScopeConstants() {
            assertEnumNames(Sync.Scope.class, List.of("CLIENT", "SERVER", "COMMON"));
        }

        @Test
        void widgetTypeConstants() {
            assertEnumNames(Widget.Type.class, List.of("AUTO", "TOGGLE", "SLIDER", "NUMBER_FIELD", "TEXT_FIELD", "DROPDOWN", "COLOR"));
        }

    }

    @Nested
    @DisplayName("Dependency hygiene — api has no outside types in its surface")
    final class DependencyHygiene {

        //==================================================
        // Static methods
        //==================================================

        private Class<?> unwrapArray(final Class<?> c) {
            Class<?> cur = c;
            while(cur.isArray()) {
                cur = cur.getComponentType();
            }

            return cur;
        }

        //==================================================
        // Constructors
        //==================================================

        DependencyHygiene() {
            super();
        }

        //==================================================
        // Tests
        //==================================================

        @Test
        void annotationMembersReturnOnlyJdkOrApiTypes() {
            for(final Class<? extends Annotation> a : ALL_ANNOTATIONS) {
                for(final Method m : a.getDeclaredMethods()) {
                    final Class<?> ret = unwrapArray(m.getReturnType());
                    final String pkg = ret.getPackageName();
                    final boolean ok = ret.isPrimitive()
                                       || pkg.startsWith("java.")
                                       || pkg.startsWith("javax.")
                                       || pkg.startsWith("com.oliveryasuna.mc.omniconfig.api");
                    assertTrue(ok, a.getSimpleName() + "#" + m.getName() + " returns " + ret.getName() + " — api must not leak non-JDK/non-api types");
                }
            }
        }

        @Test
        void widgetAndSyncAndReloadEnumsAreNestedInOwningAnnotation() {
            assertEquals(Widget.class, Widget.Type.class.getEnclosingClass());
            assertEquals(Sync.class, Sync.Scope.class.getEnclosingClass());
            assertEquals(Reload.class, Reload.Tier.class.getEnclosingClass());
        }

    }

    @Nested
    @DisplayName("Marker annotations carry no members")
    final class Markers {

        //==================================================
        // Static methods
        //==================================================

        private static void assertNoMembers(final Class<? extends Annotation> a) {
            assertEquals(0, a.getDeclaredMethods().length, a.getSimpleName() + " should be a pure marker (no members)");
            assertFalse(a.isAnnotation() && a.getDeclaredFields().length > 0, a.getSimpleName() + " should declare no fields");
        }

        //==================================================
        // Constructors
        //==================================================

        Markers() {
            super();
        }

        //==================================================
        // Tests
        //==================================================

        @Test
        void markersAreEmpty() {
            assertNoMembers(Hidden.class);
            assertNoMembers(NotNull.class);
            assertNoMembers(RequiresRestart.class);
        }

    }

}
