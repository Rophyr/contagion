package com.contagion.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import com.contagion.ContagionMod;
import com.contagion.lab.MicroscopeMenu;

public final class ModMenus {
	public static final MenuType<MicroscopeMenu> MICROSCOPE = Registry.register(BuiltInRegistries.MENU, ContagionMod.id("microscope"),
			new MenuType<>(MicroscopeMenu::new, FeatureFlags.VANILLA_SET));

	private ModMenus() {
	}

	public static void init() {
		// Loads the class, which registers the menu types.
	}
}
