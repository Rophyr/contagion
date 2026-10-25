package com.contagion.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Faces the player who placed it. {@link #STATUS} drives the lamp: 0 = idle, 1 = working (warm light),
 * 2 = result ready (green). Interaction is handled in {@link com.contagion.lab.Microscope}.
 */
public class MicroscopeBlock extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty STATUS = IntegerProperty.create("status", 0, 2);
	public static final int IDLE = 0;
	public static final int WORKING = 1;
	public static final int READY = 2;

	private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 1.5, 13.0, 16.0, 14.0);

	public MicroscopeBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(STATUS, IDLE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, STATUS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	public static int lightLevel(BlockState state) {
		return switch (state.getValue(STATUS)) {
			case WORKING -> 9;
			case READY -> 6;
			default -> 0;
		};
	}

	/** Updates the lamp if the block at {@code pos} is still a microscope. */
	public static void setStatus(Level level, BlockPos pos, int status) {
		if (!level.isLoaded(pos)) {
			return;
		}

		BlockState state = level.getBlockState(pos);

		if (state.getBlock() instanceof MicroscopeBlock && state.getValue(STATUS) != status) {
			level.setBlock(pos, state.setValue(STATUS, status), Block.UPDATE_ALL);
		}
	}
}
