package com.contagion.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A glass beaker. {@link #CONTENTS}: 0 = empty, 1 = water, 2 = boiling a reagent, 3 = extract ready.
 * Interaction is handled in {@link com.contagion.lab.Beaker}.
 */
public class BeakerBlock extends Block {
	public static final IntegerProperty CONTENTS = IntegerProperty.create("contents", 0, 3);
	public static final int EMPTY = 0;
	public static final int WATER = 1;
	public static final int BOILING = 2;
	public static final int DONE = 3;

	private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 10.0, 12.0);

	public BeakerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(CONTENTS, EMPTY));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CONTENTS);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	public static void setContents(Level level, BlockPos pos, int contents) {
		if (!level.isLoaded(pos)) {
			return;
		}

		BlockState state = level.getBlockState(pos);

		if (state.getBlock() instanceof BeakerBlock && state.getValue(CONTENTS) != contents) {
			level.setBlock(pos, state.setValue(CONTENTS, contents), Block.UPDATE_ALL);
		}
	}
}
