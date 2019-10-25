package cam72cam.mod.item;

import cam72cam.mod.util.TagCompound;

import javax.annotation.Nonnull;
import java.util.function.BiPredicate;

public class ItemStackHandler implements IInventory {
    public final ExposedItemStackHandler internal;
    protected BiPredicate<Integer, ItemStack> checkSlot = (integer, itemStack) -> true;

    private class ExposedItemStackHandler extends net.minecraftforge.items.ItemStackHandler {
        public ExposedItemStackHandler(int size) {
            super(size);
        }

        @Override
        public int getStackLimit(int slot, net.minecraft.item.ItemStack stack) {
            return super.getStackLimit(slot, stack);
        }
    }

    public ItemStackHandler(int size) {
        this.internal = new ExposedItemStackHandler(size) {
            @Override
            public void setStackInSlot(int slot, @Nonnull net.minecraft.item.ItemStack stack) {
                if (checkSlot.test(slot, new ItemStack(stack))) {
                    super.setStackInSlot(slot, stack);
                }
            }

            @Override
            @Nonnull
            public net.minecraft.item.ItemStack insertItem(int slot, @Nonnull net.minecraft.item.ItemStack stack, boolean simulate) {
                return checkSlot.test(slot, new ItemStack(stack)) ? super.insertItem(slot, stack.copy(), simulate) : stack;
            }

            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                ItemStackHandler.this.onContentsChanged(slot);
            }
        };
    }

    public ItemStackHandler() {
        this(1);
    }

    protected void onContentsChanged(int slot) {
        //NOP
    }

    public void setSize(int inventorySize) {
        internal.setSize(inventorySize);
    }

    @Override
    public int getSlotCount() {
        return internal.getSlots();
    }

    @Override
    public ItemStack get(int slot) {
        return new ItemStack(internal.getStackInSlot(slot));
    }

    @Override
    public void set(int slot, ItemStack stack) {
        if (stack.internal != null) {
            internal.setStackInSlot(slot, stack.internal);
        } else if (internal.getStackInSlot(slot) != null){
            internal.extractItem(slot, internal.getStackInSlot(slot).stackSize, false);
        }
    }

    @Override
    public ItemStack insert(int slot, ItemStack stack, boolean simulate) {
        return new ItemStack(internal.insertItem(slot, stack.internal, simulate));
    }

    @Override
    public ItemStack extract(int slot, int amount, boolean simulate) {
        return new ItemStack(internal.extractItem(slot, amount, simulate));
    }

    @Override
    public int getLimit(int slot) {
        return internal.getStackLimit(slot, internal.getStackInSlot(slot));
    }

    public TagCompound save() {
        return new TagCompound(internal.serializeNBT());
    }

    public void load(TagCompound items) {
        internal.deserializeNBT(items.internal);
    }

}
