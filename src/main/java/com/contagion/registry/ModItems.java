package com.contagion.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import com.contagion.ContagionMod;
import com.contagion.text.Msg;

/**
 * All items are plain {@link Item}s; their behavior lives in {@link com.contagion.lab.LabInteractions}.
 */
public final class ModItems {
	public static final ResourceKey<EquipmentAsset> FACE_MASK_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, ContagionMod.id("face_mask"));

	public static final Item SYRINGE = register("syringe", p -> p.stacksTo(16).component(DataComponents.LORE, lore("syringe", 2)));
	public static final Item BLOOD_SAMPLE = register("blood_sample", p -> p.stacksTo(1));
	public static final Item PETRI_DISH = register("petri_dish", p -> p.stacksTo(16).component(DataComponents.LORE, lore("petri_dish", 2)));
	public static final Item CULTURE = register("culture", p -> p.stacksTo(1));
	public static final Item MORTAR = register("mortar", p -> p.stacksTo(1).component(DataComponents.LORE, lore("mortar", 2)));
	public static final Item PREPARED_REAGENT = register("prepared_reagent", p -> p.stacksTo(16));
	public static final Item EMPTY_VIAL = register("empty_vial", p -> p.stacksTo(16).component(DataComponents.LORE, lore("empty_vial", 2)));
	public static final Item SERUM = register("serum", p -> p.stacksTo(1));
	public static final Item THERMOMETER = register("thermometer", p -> p.stacksTo(1).component(DataComponents.LORE, lore("thermometer", 2)));
	public static final Item RESEARCH_JOURNAL = register("research_journal", p -> p.stacksTo(1).component(DataComponents.LORE, lore("research_journal", 2)));
	public static final Item FACE_MASK = register("face_mask", p -> p.stacksTo(1)
			.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setAsset(FACE_MASK_ASSET).build())
			.component(DataComponents.LORE, lore("face_mask", 2)));
	public static final Item MICROSCOPE = registerBlockItem(ModBlocks.MICROSCOPE_KEY, ModBlocks.MICROSCOPE, "microscope");
	public static final Item BEAKER = registerBlockItem(ModBlocks.BEAKER_KEY, ModBlocks.BEAKER, "beaker");

	private ModItems() {
	}

	/** Tooltip lines contagion.lore.&lt;name&gt;.1 .. .n from the language files. */
	private static ItemLore lore(String name, int lines) {
		List<Component> list = new ArrayList<>();

		for (int i = 1; i <= lines; i++) {
			list.add(Msg.of("contagion.lore." + name + "." + i).styled(ChatFormatting.GRAY));
		}

		return new ItemLore(list);
	}

	private static Item register(String name, UnaryOperator<Item.Properties> properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ContagionMod.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(properties.apply(new Item.Properties()).setId(key)));
	}

	private static Item registerBlockItem(ResourceKey<Block> blockKey, Block block, String name) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, blockKey.identifier());
		return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, new Item.Properties().setId(key)
				.component(DataComponents.LORE, lore(name, 2))));
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(SYRINGE);
			output.accept(PETRI_DISH);
			output.accept(MORTAR);
			output.accept(EMPTY_VIAL);
			output.accept(THERMOMETER);
			output.accept(RESEARCH_JOURNAL);
			output.accept(FACE_MASK);
			output.accept(MICROSCOPE);
			output.accept(BEAKER);
		});
	}
}
