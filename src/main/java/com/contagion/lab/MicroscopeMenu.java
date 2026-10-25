package com.contagion.lab;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.contagion.data.SerumMix;
import com.contagion.registry.ModBlocks;
import com.contagion.registry.ModComponents;
import com.contagion.registry.ModItems;
import com.contagion.registry.ModMenus;

/**
 * The microscope's screen: a slide (blood sample or culture dish) and a sealed serum, like a crafting table
 * the items go back to the player when the screen is closed. Starting a job consumes them.
 */
public final class MicroscopeMenu extends AbstractContainerMenu {
	public static final int SLIDE_SLOT = 0;
	public static final int SERUM_SLOT = 1;
	public static final int SLIDE_X = 21;
	public static final int SLIDE_Y = 38;
	public static final int SERUM_X = 21;
	public static final int SERUM_Y = 76;
	public static final int INVENTORY_Y = 140;

	private final Container container = new SimpleContainer(2) {
		@Override
		public void setChanged() {
			super.setChanged();
			MicroscopeMenu.this.slotsChanged(this);
		}
	};
	private final ContainerLevelAccess access;

	/** Client side. */
	public MicroscopeMenu(int id, Inventory inventory) {
		this(id, inventory, ContainerLevelAccess.NULL);
	}

	public MicroscopeMenu(int id, Inventory inventory, ContainerLevelAccess access) {
		super(ModMenus.MICROSCOPE, id);
		this.access = access;

		addSlot(new Slot(this.container, SLIDE_SLOT, SLIDE_X, SLIDE_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return isSlide(stack);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		addSlot(new Slot(this.container, SERUM_SLOT, SERUM_X, SERUM_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return isSerum(stack);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		addStandardInventorySlots(inventory, 8, INVENTORY_Y);
	}

	public static boolean isSlide(ItemStack stack) {
		return stack.is(ModItems.BLOOD_SAMPLE) || stack.is(ModItems.CULTURE);
	}

	public static boolean isSerum(ItemStack stack) {
		SerumMix mix = stack.is(ModItems.SERUM) ? stack.get(ModComponents.SERUM) : null;
		return mix != null && mix.sealed();
	}

	public ItemStack slide() {
		return this.container.getItem(SLIDE_SLOT);
	}

	public ItemStack serum() {
		return this.container.getItem(SERUM_SLOT);
	}

	public boolean isAt(ServerLevel level, BlockPos pos) {
		return this.access.evaluate((l, p) -> l == level && p.equals(pos), false);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return true;
		}

		this.access.execute((level, pos) -> {
			if (level instanceof ServerLevel serverLevel) {
				if (id == Microscope.BUTTON_START) {
					Microscope.start(serverLevel, serverPlayer, pos, this.container);
				} else if (id == Microscope.BUTTON_EXAMINE) {
					Microscope.examine(serverLevel, serverPlayer, pos);
					Microscope.broadcast(serverLevel, pos);
				}
			}
		});

		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = getSlot(index);

		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		int playerStart = 2;
		int playerEnd = this.slots.size();

		if (index < playerStart) {
			if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
				return ItemStack.EMPTY;
			}
		} else if (isSlide(stack)) {
			if (!moveItemStackTo(stack, SLIDE_SLOT, SLIDE_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (isSerum(stack)) {
			if (!moveItemStackTo(stack, SERUM_SLOT, SERUM_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, ModBlocks.MICROSCOPE);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.access.execute((level, pos) -> clearContainer(player, this.container));
	}
}
