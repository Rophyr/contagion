package com.contagion.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.contagion.ContagionMod;
import com.contagion.block.BeakerBlock;
import com.contagion.block.MicroscopeBlock;

public final class ModBlocks {
	public static final ResourceKey<Block> MICROSCOPE_KEY = ResourceKey.create(Registries.BLOCK, ContagionMod.id("microscope"));

	// Behavior (analysis, culture tests) is handled in LabInteractions via UseBlockCallback.
	public static final Block MICROSCOPE = Registry.register(BuiltInRegistries.BLOCK, MICROSCOPE_KEY,
			new MicroscopeBlock(BlockBehaviour.Properties.of()
					.strength(1.5F)
					.sound(SoundType.METAL)
					.noOcclusion()
					.lightLevel(MicroscopeBlock::lightLevel)
					.setId(MICROSCOPE_KEY)));

	public static final ResourceKey<Block> BEAKER_KEY = ResourceKey.create(Registries.BLOCK, ContagionMod.id("beaker"));

	// Boils reagents into extracts when it sits on a heat source. Handled in LabInteractions / Beaker.
	public static final Block BEAKER = Registry.register(BuiltInRegistries.BLOCK, BEAKER_KEY,
			new BeakerBlock(BlockBehaviour.Properties.of()
					.strength(0.3F)
					.sound(SoundType.GLASS)
					.noOcclusion()
					.setId(BEAKER_KEY)));

	private ModBlocks() {
	}

	public static void init() {
		// Loads the class, which registers the blocks.
	}
}
