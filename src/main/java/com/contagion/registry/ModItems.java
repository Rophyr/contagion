package com.contagion.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import com.contagion.ContagionMod;
import com.contagion.disease.TaintedFoodItem;
import com.contagion.lab.ThermometerItem;
import com.contagion.text.Msg;

/**
 * Most items are plain {@link Item}s whose behavior lives in {@link com.contagion.lab.LabInteractions}; the
 * thermometer and the tainted meats have their own classes because they hook into using/eating.
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
	public static final Item THERMOMETER = register("thermometer", ThermometerItem::new, p -> p.stacksTo(1).component(DataComponents.LORE, lore("thermometer", 2)));
	public static final Item RESEARCH_JOURNAL = register("research_journal", p -> p.stacksTo(1).component(DataComponents.LORE, lore("research_journal", 2)));
	public static final Item FACE_MASK = register("face_mask", p -> p.stacksTo(1)
			.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setAsset(FACE_MASK_ASSET).build())
			.component(DataComponents.LORE, lore("face_mask", 2)));
	public static final Item MICROSCOPE = registerBlockItem(ModBlocks.MICROSCOPE_KEY, ModBlocks.MICROSCOPE, "microscope");
	public static final Item BEAKER = registerBlockItem(ModBlocks.BEAKER_KEY, ModBlocks.BEAKER, "beaker");

	/** Meat from an infected animal, raw vanilla item id to the tainted look-alike (raw and cooked). */
	public static final Map<String, Item> TAINTED = new LinkedHashMap<>();

	static {
		for (String[] meat : new String[][] {{"beef", "cooked_beef"}, {"porkchop", "cooked_porkchop"}, {"mutton", "cooked_mutton"},
				{"chicken", "cooked_chicken"}, {"rabbit", "cooked_rabbit"}}) {
			for (String name : meat) {
				TAINTED.put(name, registerTainted(name));
			}
		}
	}

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
		return register(name, Item::new, properties);
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, UnaryOperator<Item.Properties> properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ContagionMod.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.apply(new Item.Properties()).setId(key)));
	}

	/** Looks and tastes like the vanilla item (same name, model and food values). */
	private static Item registerTainted(String vanillaName) {
		Item vanilla = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(vanillaName));
		FoodProperties food = switch (vanillaName) {
			case "beef" -> Foods.BEEF;
			case "cooked_beef" -> Foods.COOKED_BEEF;
			case "porkchop" -> Foods.PORKCHOP;
			case "cooked_porkchop" -> Foods.COOKED_PORKCHOP;
			case "mutton" -> Foods.MUTTON;
			case "cooked_mutton" -> Foods.COOKED_MUTTON;
			case "chicken" -> Foods.CHICKEN;
			case "cooked_chicken" -> Foods.COOKED_CHICKEN;
			case "rabbit" -> Foods.RABBIT;
			default -> Foods.COOKED_RABBIT;
		};
		// Raw chicken keeps its chance of food poisoning.
		Consumable consumable = vanillaName.equals("chicken") ? Consumables.CHICKEN : Consumables.DEFAULT_FOOD;
		return register("tainted_" + vanillaName, TaintedFoodItem::new, p -> p.overrideDescription(vanilla.getDescriptionId()).food(food, consumable));
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
