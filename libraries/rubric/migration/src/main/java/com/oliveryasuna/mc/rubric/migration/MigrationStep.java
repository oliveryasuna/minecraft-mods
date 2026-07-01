package com.oliveryasuna.mc.rubric.migration;

import com.oliveryasuna.mc.rubric.value.Section;
import com.oliveryasuna.mc.rubric.value.ValueNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * An ordered set of ops that upgrade a config tree to {@link #getToVersion()}.
 */
public final class MigrationStep {

    //==================================================
    // Static methods
    //==================================================

    public static Builder to(final int toVersion) {
        return new Builder(toVersion);
    }

    //==================================================
    // Fields
    //==================================================

    private final int toVersion;
    private final List<MigrationOp> ops;

    //==================================================
    // Constructors
    //==================================================

    public MigrationStep(
            final int toVersion,
            final List<MigrationOp> ops
    ) {
        super();

        this.toVersion = toVersion;
        this.ops = List.copyOf(ops);
    }

    //==================================================
    // Methods
    //==================================================

    public void apply(final Section root) {
        for(final MigrationOp op : ops) {
            op.apply(root);
        }
    }

    //==================================================
    // Getters/setters
    //==================================================

    public int getToVersion() {
        return toVersion;
    }

    //==================================================
    // Nested
    //==================================================

    public static final class Builder implements org.apache.commons.lang3.builder.Builder<MigrationStep> {

        //==================================================
        // Fields
        //==================================================

        private final int toVersion;
        private final List<MigrationOp> ops;

        //==================================================
        // Constructors
        //==================================================

        private Builder(final int toVersion) {
            super();

            this.toVersion = toVersion;
            this.ops = new ArrayList<>();
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public MigrationStep build() {
            return new MigrationStep(toVersion, ops);
        }

        public Builder op(final MigrationOp op) {
            ops.add(op);
            return this;
        }

        public Builder renameKey(
                final String from,
                final String to
        ) {
            return op(MigrationOps.renameKey(from, to));
        }

        public Builder removeKey(final String path) {
            return op(MigrationOps.removeKey(path));
        }

        public Builder setDefault(
                final String path,
                final ValueNode value
        ) {
            return op(MigrationOps.setDefault(path, value));
        }

        public Builder setValue(
                final String path,
                final ValueNode value
        ) {
            return op(MigrationOps.setValue(path, value));
        }

        public Builder transform(
                final String path,
                final UnaryOperator<ValueNode> fn
        ) {
            return op(MigrationOps.transform(path, fn));
        }


    }

}
