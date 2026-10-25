package com.contagion.disease;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.contagion.text.Msg;

/**
 * Vanilla items that can be mixed into a serum. Each world's strain secretly picks three of them as the cure
 * (one from each of three different categories) and marks several others as toxic.
 */
public enum Reagent {
	REDSTONE("redstone", Category.MINERAL, false, 0xC81E1E),
	GLOWSTONE_DUST("glowstone_dust", Category.MINERAL, false, 0xF0C850),
	GUNPOWDER("gunpowder", Category.MINERAL, false, 0x707070),
	AMETHYST_SHARD("amethyst_shard", Category.MINERAL, false, 0x9A5CC6),

	SUGAR("sugar", Category.BOTANICAL, false, 0xF2F2F2),
	SWEET_BERRIES("sweet_berries", Category.BOTANICAL, false, 0xB4243C),
	GLOW_BERRIES("glow_berries", Category.BOTANICAL, false, 0xF09A30),
	COCOA_BEANS("cocoa_beans", Category.BOTANICAL, false, 0x6E4026),

	BROWN_MUSHROOM("brown_mushroom", Category.FUNGAL, false, 0x96704E),
	RED_MUSHROOM("red_mushroom", Category.FUNGAL, false, 0xD23C32),
	CRIMSON_FUNGUS("crimson_fungus", Category.FUNGAL, true, 0xA0282E),
	WARPED_FUNGUS("warped_fungus", Category.FUNGAL, true, 0x2A9C8C),

	SPIDER_EYE("spider_eye", Category.ANIMAL, false, 0x8C2236),
	RABBIT_FOOT("rabbit_foot", Category.ANIMAL, true, 0xC8A07A),
	SLIME_BALL("slime_ball", Category.ANIMAL, false, 0x7CC85A),
	HONEYCOMB("honeycomb", Category.ANIMAL, false, 0xE8A428),

	DRIED_KELP("dried_kelp", Category.AQUATIC, false, 0x4A6A2E),
	PUFFERFISH("pufferfish", Category.AQUATIC, false, 0xE6C832),
	INK_SAC("ink_sac", Category.AQUATIC, false, 0x2C2C44),
	PRISMARINE_CRYSTALS("prismarine_crystals", Category.AQUATIC, true, 0x9CD8C8),

	BLAZE_POWDER("blaze_powder", Category.NETHER, true, 0xF08C1E),
	MAGMA_CREAM("magma_cream", Category.NETHER, true, 0xC85A1E),
	NETHER_WART("nether_wart", Category.NETHER, true, 0x8C1E28),
	GHAST_TEAR("ghast_tear", Category.NETHER, true, 0xCFEFEF);

	private final Identifier itemId;
	private final Category category;
	/** Hard to obtain early; a cure contains at most one hard reagent. */
	private final boolean hard;
	/** Colour it gives a serum (purely cosmetic). */
	private final int color;

	Reagent(String path, Category category, boolean hard, int color) {
		this.itemId = Identifier.withDefaultNamespace(path);
		this.category = category;
		this.hard = hard;
		this.color = color;
	}

	public int color() {
		return this.color;
	}

	public Category category() {
		return this.category;
	}

	public boolean hard() {
		return this.hard;
	}

	public Item item() {
		return BuiltInRegistries.ITEM.getValue(this.itemId);
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public Msg msg() {
		return Msg.of("contagion.reagent." + id());
	}

	public static @Nullable Reagent of(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}

		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());

		for (Reagent reagent : values()) {
			if (reagent.itemId.equals(id)) {
				return reagent;
			}
		}

		return null;
	}

	public static @Nullable Reagent byName(String name) {
		try {
			return valueOf(name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public enum Category {
		MINERAL,
		BOTANICAL,
		FUNGAL,
		ANIMAL,
		AQUATIC,
		NETHER;

		public Msg msg() {
			return Msg.of("contagion.category." + name().toLowerCase(Locale.ROOT));
		}
	}
}
