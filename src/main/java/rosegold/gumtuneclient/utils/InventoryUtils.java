package rosegold.gumtuneclient.utils;

import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import rosegold.gumtuneclient.GumTuneClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

public class InventoryUtils {

    public static boolean holdItem(String item) {
        int slot = getSlotIdOfItemInHotbar(item);
        if (slot == -1) return false;
        GumTuneClient.mc.thePlayer.inventory.currentItem = slot;
        return true;
    }
    public static boolean holdItemSb(String id) {
        int slot = findItemInHotbarSkyblockId(id);
        if (slot == -1) return false;
        GumTuneClient.mc.thePlayer.inventory.currentItem = slot;
        return true;
    }

    public static int getSlotIdOfItemInContainer(String item) {
        return getSlotIdOfItemInContainer(item, false);
    }

    public static int getSlotIdOfItemInContainer(String item, boolean equals) {
        for (Slot slot : GumTuneClient.mc.thePlayer.openContainer.inventorySlots) {
            if (!slot.getHasStack()) continue;
            String itemName = StringUtils.removeFormatting(slot.getStack().getDisplayName());
            if (equals) {
                if (itemName.equalsIgnoreCase(item)) {
                    return slot.slotNumber;
                }
            } else {
                if (itemName.contains(item)) {
                    return slot.slotNumber;
                }
            }
        }
        return -1;
    }

    public static Slot getSlotOfItemInContainer(String item) {
        return getSlotOfItemInContainer(item, false);
    }

    public static Slot getSlotOfItemInContainer(String item, boolean equals) {
        for (Slot slot : GumTuneClient.mc.thePlayer.openContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.removeFormatting(slot.getStack().getDisplayName());
                if (equals) {
                    if (itemName.equalsIgnoreCase(item)) {
                        return slot;
                    }
                } else {
                    if (itemName.contains(item)) {
                        return slot;
                    }
                }
            }
        }
        return null;
    }

    public static int getSlotIdOfItemInHotbar(String... items) {
        for (int i = 0; i < 9; i++) {
            ItemStack slot = GumTuneClient.mc.thePlayer.inventory.getStackInSlot(i);
            if (slot != null && slot.getItem() != null) {
                String itemName = StringUtils.removeFormatting(slot.getDisplayName());
                if (Arrays.stream(items).anyMatch(itemName::contains)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static Slot getSlotOfItemInHotbar(String item) {
        for (int i = 0; i < 9; i++) {
            ItemStack slot = GumTuneClient.mc.thePlayer.inventory.getStackInSlot(i);
            if (slot != null && slot.getItem() != null) {
                String itemName = StringUtils.removeFormatting(slot.getDisplayName());
                if (itemName.contains(item)) {
                    return GumTuneClient.mc.thePlayer.inventoryContainer.getSlot(i);
                }
            }
        }
        return null;
    }

    public static int getSlotIdOfItemInInventory(String item) {
        for (Slot slot : GumTuneClient.mc.thePlayer.inventoryContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.removeFormatting(slot.getStack().getDisplayName());
                if (itemName.contains(item)) {
                    return slot.slotNumber;
                }
            }
        }
        return -1;
    }

    public static Slot getSlotOfItemInInventory(String item) {
        for (Slot slot : GumTuneClient.mc.thePlayer.inventoryContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.removeFormatting(slot.getStack().getDisplayName());
                if (itemName.contains(item)) {
                    return slot;
                }
            }
        }
        return null;
    }

    public static String getInventoryName() {
        try {
            if (GumTuneClient.mc.currentScreen instanceof GuiChest) {
                final ContainerChest chest = (ContainerChest) GumTuneClient.mc.thePlayer.openContainer;
                if (chest == null) return null;
                final IInventory inv = chest.getLowerChestInventory();
                return inv.hasCustomName() ? inv.getName() : null;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean hasItemInInventory(String item) {
        for (Slot slot : GumTuneClient.mc.thePlayer.inventoryContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.removeFormatting(slot.getStack().getDisplayName());
                if (itemName.contains(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasItemInHotbar(String... item) {
        // return getSlotIdOfItemInHotbar(item) != -1;
        for (int i = 0; i < 9; i++) {
            ItemStack slot = GumTuneClient.mc.thePlayer.inventory.getStackInSlot(i);
            if (slot != null && slot.getItem() != null) {
                String itemName = StringUtils.removeFormatting(slot.getDisplayName());
                if (Arrays.stream(item).anyMatch(itemName::contains)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ArrayList<Slot> getIndexesOfItemsFromInventory(Predicate<Slot> predicate) {
        ArrayList<Slot> indexes = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            Slot slot = GumTuneClient.mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot != null && slot.getHasStack()) {
                if (predicate.test(slot)) {
                    indexes.add(slot);
                }
            }
        }
        return indexes;
    }

    public static ArrayList<Slot> getIndexesOfItemsFromcontainer(Predicate<Slot> predicate) {
        ArrayList<Slot> indexes = new ArrayList<>();
        for (int i = 0; i < GumTuneClient.mc.thePlayer.openContainer.inventorySlots.size(); i++) {
            Slot slot = GumTuneClient.mc.thePlayer.openContainer.getSlot(i);
            if (slot != null && slot.getHasStack()) {
                if (predicate.test(slot)) {
                    indexes.add(slot);
                }
            }
        }
        return indexes;
    }

    public static void clickSlotWithId(int id, ClickType mouseButton, ClickMode mode, int windowId) {
        GumTuneClient.mc.playerController.windowClick(windowId, id, mouseButton.ordinal(), mode.ordinal(), GumTuneClient.mc.thePlayer);
    }

    public static void clickContainerSlot(int slot, ClickType mouseButton, ClickMode mode) {
        GumTuneClient.mc.playerController.windowClick(GumTuneClient.mc.thePlayer.openContainer.windowId, slot, mouseButton.ordinal(), mode.ordinal(), GumTuneClient.mc.thePlayer);
    }

    public static void clickSlot(int slot, ClickType mouseButton, ClickMode mode) {
        GumTuneClient.mc.playerController.windowClick(GumTuneClient.mc.thePlayer.inventoryContainer.windowId, slot, mouseButton.ordinal(), mode.ordinal(), GumTuneClient.mc.thePlayer);
    }

    public static void swapSlots(int slot, int hotbarSlot) {
        GumTuneClient.mc.playerController.windowClick(GumTuneClient.mc.thePlayer.inventoryContainer.windowId, slot, hotbarSlot, 2, GumTuneClient.mc.thePlayer);
    }

    public static void openInventory() {
        KeyBinding.onTick(GumTuneClient.mc.gameSettings.keyBindInventory.getKeyCode());
    }

    public static Slot getSlotOfId(int id) {
        for (Slot slot : GumTuneClient.mc.thePlayer.inventoryContainer.inventorySlots) {
            if (slot.slotNumber == id) {
                return slot;
            }
        }
        return null;
    }

    public static Slot getSlotOfIdInContainer(int id) {
        for (Slot slot : GumTuneClient.mc.thePlayer.openContainer.inventorySlots) {
            if (slot.slotNumber == id) {
                return slot;
            }
        }
        return null;
    }

    public static String getLoreOfItemInContainer(int slot) {
        if (slot == -1) return null;
        ItemStack itemStack = GumTuneClient.mc.thePlayer.openContainer.getSlot(slot).getStack();
        if (itemStack == null) return null;
        return getItemLore(itemStack);
    }

    public static String getLoreOfItemInContainer(int slot, int index) {
        if (slot == -1) return null;
        ItemStack itemStack = GumTuneClient.mc.thePlayer.openContainer.getSlot(slot).getStack();
        if (itemStack == null) return null;
        return getItemLore(itemStack, index);
    }

    public static int getAmountOfItemInInventory(String item) {
        int amount = 0;
        for (Slot slot : GumTuneClient.mc.thePlayer.inventoryContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.removeFormatting(slot.getStack().getDisplayName());
                if (itemName.equals(item)) {
                    amount += slot.getStack().stackSize;
                }
            }
        }
        return amount;
    }

    public static int findItemInHotbar(String name) {
        InventoryPlayer inv = GumTuneClient.mc.thePlayer.inventory;
        for (int i = 0; i < 9; i++) {
            ItemStack curStack = inv.getStackInSlot(i);
            if (curStack != null) {
                if (curStack.getDisplayName().contains(name)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int findItemInHotbarSkyblockId(String id) {
        InventoryPlayer inv = GumTuneClient.mc.thePlayer.inventory;
        for (int i = 0; i < 9; i++) {
            ItemStack curStack = inv.getStackInSlot(i);
            if (curStack != null) {
                String skyblockId = getSkyBlockItemId(curStack);
                if (skyblockId != null && skyblockId.equals(id)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static String getItemLore(ItemStack itemStack) {
        if (itemStack.hasTagCompound()) {
            return itemStack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8).toString();
        }
        return null;
    }

    public static String getItemLore(ItemStack itemStack, int index) {
        if (itemStack.hasTagCompound()) {
            return itemStack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8).getStringTagAt(index);
        }
        return null;
    }

    public static String getSkyBlockItemId(ItemStack itemStack) {
        NBTTagCompound extraAttributes = getExtraAttributes(itemStack);
        if (extraAttributes != null && extraAttributes.hasKey("id", 8)) {
            return extraAttributes.getString("id");
        }
        return null;
    }

    public static boolean isInventoryLoaded() {
        if (GumTuneClient.mc.thePlayer == null || GumTuneClient.mc.thePlayer.openContainer == null) return false;
        if (!(GumTuneClient.mc.currentScreen instanceof GuiChest)) return false;
        ContainerChest chest = (ContainerChest) GumTuneClient.mc.thePlayer.openContainer;
        int lowerChestSize = chest.getLowerChestInventory().getSizeInventory();
        ItemStack lastSlot = chest.getLowerChestInventory().getStackInSlot(lowerChestSize - 1);
        return lastSlot != null && lastSlot.getItem() != null;
    }

    public static NBTTagCompound getExtraAttributes(ItemStack itemStack) {
        if (itemStack.hasTagCompound()) {
            return itemStack.getTagCompound().getCompoundTag("ExtraAttributes");
        }
        return null;
    }

    public static int getFilledSlotCount() {
        int count = 0;
        for (ItemStack itemStack : GumTuneClient.mc.thePlayer.inventory.mainInventory) {
            if (itemStack != null) {
                count++;
            }
        }
        return count;
    }

    public static enum ClickType {
        LEFT,
        RIGHT
    }

    public static enum ClickMode {
        PICKUP,
        QUICK_MOVE,
        SWAP
    }
}
