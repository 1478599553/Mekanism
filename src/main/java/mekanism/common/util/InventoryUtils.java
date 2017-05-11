package mekanism.common.util;

import mekanism.api.EnumColor;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.content.transporter.Finder;
import mekanism.common.content.transporter.InvStack;
import mekanism.common.content.transporter.TransporterManager;
import mekanism.common.tile.TileEntityBin;
import mekanism.common.tile.TileEntityLogisticalSorter;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public final class InventoryUtils
{
	public static final int[] EMPTY = new int[] {};

	public static int[] getIntRange(int start, int end)
	{
		int[] ret = new int[1 + end - start];

		for(int i = start; i <= end; i++)
		{
			ret[i - start] = i;
		}

		return ret;
	}
	
	public static IInventory checkChestInv(IInventory inv)
	{
		if(inv instanceof TileEntityChest)
		{
			TileEntityChest main = (TileEntityChest)inv;
			TileEntityChest adj = null;

			if(main.adjacentChestXNeg != null)
			{
				adj = main.adjacentChestXNeg;
			}
			else if(main.adjacentChestXPos != null)
			{
				adj = main.adjacentChestXPos;
			}
			else if(main.adjacentChestZNeg != null)
			{
				adj = main.adjacentChestZNeg;
			}
			else if(main.adjacentChestZPos != null)
			{
				adj = main.adjacentChestZPos;
			}

			if(adj != null)
			{
				return new InventoryLargeChest("", main, adj);
			}
		}

		return inv;
	}

	public static ItemStack putStackInInventory(TileEntity tile, ItemStack itemStack, EnumFacing side, boolean force)
	{
		if(force && tile instanceof TileEntityLogisticalSorter)
		{
			return ((TileEntityLogisticalSorter)tile).sendHome(itemStack.copy());
		}

		ItemStack toInsert = itemStack.copy();

		//prioritize other implementations first to allow item forcing
		if(tile instanceof ISidedInventory)
		{
			ISidedInventory sidedInventory = (ISidedInventory)tile;
			int[] slots = sidedInventory.getSlotsForFace(side.getOpposite());

			if(slots != null && slots.length != 0)
			{
				if(force && sidedInventory instanceof TileEntityBin && side == EnumFacing.UP)
				{
					slots = sidedInventory.getSlotsForFace(EnumFacing.UP);
				}

				for(int get = 0; get <= slots.length - 1; get++)
				{
					int slotID = slots[get];

					if(!force)
					{
						if(!sidedInventory.isItemValidForSlot(slotID, toInsert) || !sidedInventory.canInsertItem(slotID, toInsert, side.getOpposite()))
						{
							continue;
						}
					}

					ItemStack inSlot = sidedInventory.getStackInSlot(slotID);

					if(inSlot == null)
					{
						if(toInsert.stackSize <= sidedInventory.getInventoryStackLimit())
						{
							sidedInventory.setInventorySlotContents(slotID, toInsert);
							sidedInventory.markDirty();
							
							return null;
						}
						else {
							int rejects = toInsert.stackSize - sidedInventory.getInventoryStackLimit();
							
							ItemStack toSet = toInsert.copy();
							toSet.stackSize = sidedInventory.getInventoryStackLimit();

							ItemStack remains = toInsert.copy();
							remains.stackSize = rejects;

							sidedInventory.setInventorySlotContents(slotID, toSet);
							sidedInventory.markDirty();

							toInsert = remains;
						}
					}
					else if(areItemsStackable(toInsert, inSlot) && inSlot.stackSize < Math.min(inSlot.getMaxStackSize(), sidedInventory.getInventoryStackLimit()))
					{
						int max = Math.min(inSlot.getMaxStackSize(), sidedInventory.getInventoryStackLimit());
						
						if(inSlot.stackSize + toInsert.stackSize <= max)
						{
							ItemStack toSet = toInsert.copy();
							toSet.stackSize += inSlot.stackSize;

							sidedInventory.setInventorySlotContents(slotID, toSet);
							sidedInventory.markDirty();
							
							return null;
						}
						else {
							int rejects = (inSlot.stackSize + toInsert.stackSize) - max;

							ItemStack toSet = toInsert.copy();
							toSet.stackSize = max;

							ItemStack remains = toInsert.copy();
							remains.stackSize = rejects;

							sidedInventory.setInventorySlotContents(slotID, toSet);
							sidedInventory.markDirty();

							toInsert = remains;
						}
					}
				}
			}
		}
		else if(tile instanceof IInventory)
		{
			IInventory inventory = checkChestInv((IInventory)tile);
			
			for(int i = 0; i <= inventory.getSizeInventory() - 1; i++)
			{
				if(!force)
				{
					if(!inventory.isItemValidForSlot(i, toInsert))
					{
						continue;
					}
				}

				ItemStack inSlot = inventory.getStackInSlot(i);

				if(inSlot == null)
				{
					if(toInsert.stackSize <= inventory.getInventoryStackLimit())
					{
						inventory.setInventorySlotContents(i, toInsert);
						inventory.markDirty();
						
						return null;
					}
					else {
						int rejects = toInsert.stackSize - inventory.getInventoryStackLimit();
						
						ItemStack toSet = toInsert.copy();
						toSet.stackSize = inventory.getInventoryStackLimit();

						ItemStack remains = toInsert.copy();
						remains.stackSize = rejects;

						inventory.setInventorySlotContents(i, toSet);
						inventory.markDirty();

						toInsert = remains;
					}
				}
				else if(areItemsStackable(toInsert, inSlot) && inSlot.stackSize < Math.min(inSlot.getMaxStackSize(), inventory.getInventoryStackLimit()))
				{
					int max = Math.min(inSlot.getMaxStackSize(), inventory.getInventoryStackLimit());
					
					if(inSlot.stackSize + toInsert.stackSize <= max)
					{
						ItemStack toSet = toInsert.copy();
						toSet.stackSize += inSlot.stackSize;

						inventory.setInventorySlotContents(i, toSet);
						inventory.markDirty();
						
						return null;
					}
					else {
						int rejects = (inSlot.stackSize + toInsert.stackSize) - max;

						ItemStack toSet = toInsert.copy();
						toSet.stackSize = max;

						ItemStack remains = toInsert.copy();
						remains.stackSize = rejects;

						inventory.setInventorySlotContents(i, toSet);
						inventory.markDirty();

						toInsert = remains;
					}
				}
			}
		}
		else if(isItemHandler(tile, side.getOpposite()))
		{
			IItemHandler inventory = getItemHandler(tile, side.getOpposite());
			
			for(int i = 0; i < inventory.getSlots(); i++)
			{
				toInsert = inventory.insertItem(i, toInsert, false);
				
				if(toInsert == null)
				{
					return null;
				}
			}
		}

		return toInsert;
	}

	public static boolean areItemsStackable(ItemStack toInsert, ItemStack inSlot) 
	{
		if(toInsert == null || inSlot == null)
		{
			return true;
		}
		
    	return inSlot.isItemEqual(toInsert) && ItemStack.areItemStackTagsEqual(inSlot, toInsert);
  	}

  	public static InvStack takeTopItem(TileEntity tile, EnumFacing side, int amount)
	{
  		if(isItemHandler(tile, side.getOpposite()))
  		{
  			IItemHandler inventory = getItemHandler(tile, side.getOpposite());
  			
  			for(int i = inventory.getSlots() - 1; i >= 0; i--)
			{
				ItemStack stack = inventory.extractItem(i, amount, true);
				
				if(stack != null)
				{
					return new InvStack(tile, i, stack, side.getOpposite());
				}
			}
  		}
		if(tile instanceof ISidedInventory)
		{
			ISidedInventory sidedInventory = (ISidedInventory)tile;
			int[] slots = sidedInventory.getSlotsForFace(side.getOpposite());

			if(slots != null)
			{
				for(int get = slots.length - 1; get >= 0; get--)
				{
					int slotID = slots[get];

					if(sidedInventory.getStackInSlot(slotID) != null && sidedInventory.getStackInSlot(slotID).stackSize > 0)
					{
						ItemStack toSend = sidedInventory.getStackInSlot(slotID).copy();
						toSend.stackSize = Math.min(amount, toSend.stackSize);

						if(sidedInventory.canExtractItem(slotID, toSend, side.getOpposite()))
						{
							return new InvStack(tile, slotID, toSend, side.getOpposite());
						}
					}
				}
			}
		}
		else if(tile instanceof IInventory)
		{
			IInventory inventory = checkChestInv((IInventory)tile);
			
			for(int i = inventory.getSizeInventory() - 1; i >= 0; i--)
			{
				if(inventory.getStackInSlot(i) != null && inventory.getStackInSlot(i).getCount() > 0)
				{
					ItemStack toSend = inventory.getStackInSlot(i).copy();
					toSend.stackSize = Math.min(amount, toSend.stackSize);

					return new InvStack(tile, i, toSend, side.getOpposite());
				}
			}
		}

		return null;
	}

	public static InvStack takeDefinedItem(TileEntity tile, EnumFacing side, ItemStack type, int min, int max)
	{
		InvStack ret = new InvStack(tile, side.getOpposite());

		if(isItemHandler(tile, side.getOpposite()))
		{
			IItemHandler inventory = getItemHandler(tile, side.getOpposite());
			
			for(int i = inventory.getSlots() - 1; i >= 0; i--)
			{
				ItemStack stack = inventory.extractItem(i, max, true);
				
				if(stack != null && StackUtils.equalsWildcard(stack, type))
				{
					int current = ret.getStack() != null ? ret.getStack().stackSize : 0;

					if(current+stack.stackSize <= max)
					{
						ret.appendStack(i, stack.copy());
					}
					else {
						ItemStack copy = stack.copy();
						copy.stackSize = max-current;
						ret.appendStack(i, copy);
					}

					if(ret.getStack() != null && ret.getStack().stackSize == max)
					{
						return ret;
					}
				}
			}
		}
		else if(tile instanceof ISidedInventory)
		{
			ISidedInventory sidedInventory = (ISidedInventory)tile;
			int[] slots = sidedInventory.getSlotsForFace(side.getOpposite());

			if(slots != null && slots.length != 0)
			{
				for(int get = slots.length - 1; get >= 0; get--)
				{
					int slotID = slots[get];

					if(sidedInventory.getStackInSlot(slotID) != null && StackUtils.equalsWildcard(sidedInventory.getStackInSlot(slotID), type))
					{
						ItemStack stack = sidedInventory.getStackInSlot(slotID);
						int current = ret.getStack() != null ? ret.getStack().stackSize : 0;

						if(current+stack.stackSize <= max)
						{
							ItemStack copy = stack.copy();

							if(sidedInventory.canExtractItem(slotID, copy, side.getOpposite()))
							{
								ret.appendStack(slotID, copy);
							}
						}
						else {
							ItemStack copy = stack.copy();

							if(sidedInventory.canExtractItem(slotID, copy, side.getOpposite()))
							{
								copy.stackSize = max-current;
								ret.appendStack(slotID, copy);
							}
						}

						if(ret.getStack() != null && ret.getStack().stackSize == max)
						{
							return ret;
						}
					}
				}
			}
		}
		else if(tile instanceof IInventory)
		{
			IInventory inventory = checkChestInv((IInventory)tile);
			
			for(int i = inventory.getSizeInventory() - 1; i >= 0; i--)
			{
				if(inventory.getStackInSlot(i) != null && StackUtils.equalsWildcard(inventory.getStackInSlot(i), type))
				{
					ItemStack stack = inventory.getStackInSlot(i);
					int current = ret.getStack() != null ? ret.getStack().stackSize : 0;

					if(current+stack.getCount() <= max)
					{
						ret.appendStack(i, stack.copy());
					}
					else {
						ItemStack copy = stack.copy();
						copy.stackSize = max-current;
						ret.appendStack(i, copy);
					}

					if(ret.getStack() != null && ret.getStack().stackSize == max)
					{
						return ret;
					}
				}
			}
		}

		if(ret != null && ret.getStack() != null && ret.getStack().stackSize >= min)
		{
			return ret;
		}

		return null;
	}

	public static InvStack takeTopStack(TileEntity tile, EnumFacing side, Finder id)
	{
		if(isItemHandler(tile, side.getOpposite()))
		{
			IItemHandler inventory = getItemHandler(tile, side.getOpposite());
			
			for(int i = inventory.getSlots() - 1; i >= 0; i--)
			{
				ItemStack stack = inventory.extractItem(i, 64, true);
				
				if(stack != null && id.modifies(stack))
				{
					return new InvStack(tile, i, stack, side.getOpposite());
				}
			}
		}
		else if(tile instanceof ISidedInventory)
		{
			ISidedInventory sidedInventory = (ISidedInventory)tile;
			int[] slots = sidedInventory.getSlotsForFace(side.getOpposite());

			if(slots != null && slots.length != 0)
			{
				for(int get = slots.length - 1; get >= 0; get--)
				{
					int slotID = slots[get];

					if(sidedInventory.getStackInSlot(slotID) != null && id.modifies(sidedInventory.getStackInSlot(slotID)))
					{
						ItemStack toSend = sidedInventory.getStackInSlot(slotID);

						if(sidedInventory.canExtractItem(slotID, toSend, side.getOpposite()))
						{
							return new InvStack(tile, slotID, toSend, side.getOpposite());
						}
					}
				}
			}
		}
		else if(tile instanceof IInventory)
		{
			IInventory inventory = checkChestInv((IInventory)tile);
			
			for(int i = inventory.getSizeInventory() - 1; i >= 0; i--)
			{
				if(!inventory.getStackInSlot(i).isEmpty() && id.modifies(inventory.getStackInSlot(i)))
				{
					ItemStack toSend = inventory.getStackInSlot(i).copy();
					return new InvStack(tile, i, toSend, side.getOpposite());
				}
			}
		}

		return null;
	}

	public static boolean canInsert(TileEntity tileEntity, EnumColor color, ItemStack itemStack, EnumFacing side, boolean force)
	{
		if(force && tileEntity instanceof TileEntityLogisticalSorter)
		{
			return ((TileEntityLogisticalSorter)tileEntity).canSendHome(itemStack);
		}

		if(!force && tileEntity instanceof ISideConfiguration)
		{
			ISideConfiguration config = (ISideConfiguration)tileEntity;
			EnumFacing tileSide = config.getOrientation();
			EnumColor configColor = config.getEjector().getInputColor(MekanismUtils.getBaseOrientation(side, tileSide).getOpposite());

			if(config.getEjector().hasStrictInput() && configColor != null && configColor != color)
			{
				return false;
			}
		}

		if(isItemHandler(tileEntity, side.getOpposite()))
		{
			IItemHandler inventory = getItemHandler(tileEntity, side.getOpposite());
			
			for(int i = 0; i < inventory.getSlots(); i++)
			{
				ItemStack rejects = inventory.insertItem(i, itemStack, true);
				
				if(TransporterManager.didEmit(itemStack, rejects))
				{
					return true;
				}
			}
		}
		else if(tileEntity instanceof ISidedInventory)
		{
			ISidedInventory sidedInventory = (ISidedInventory)tileEntity;
			int[] slots = sidedInventory.getSlotsForFace(side.getOpposite());

			if(slots != null && slots.length != 0)
			{
				if(force && sidedInventory instanceof TileEntityBin && side == EnumFacing.UP)
				{
					slots = sidedInventory.getSlotsForFace(EnumFacing.UP);
				}

				for(int get = 0; get <= slots.length - 1; get++)
				{
					int slotID = slots[get];

					if(!force)
					{
						if(!sidedInventory.isItemValidForSlot(slotID, itemStack) || !sidedInventory.canInsertItem(slotID, itemStack, side.getOpposite()))
						{
							continue;
						}
					}

					ItemStack inSlot = sidedInventory.getStackInSlot(slotID);

					if(inSlot == null)
					{
						if(itemStack.stackSize <= sidedInventory.getInventoryStackLimit())
						{
							return true;
						}
						else {
							int rejects = itemStack.stackSize - sidedInventory.getInventoryStackLimit();
							
							if(rejects < itemStack.stackSize)
							{
								return true;
							}
						}
					}
					else if(areItemsStackable(itemStack, inSlot) && inSlot.stackSize < Math.min(inSlot.getMaxStackSize(), sidedInventory.getInventoryStackLimit()))
					{
						int max = Math.min(inSlot.getMaxStackSize(), sidedInventory.getInventoryStackLimit());
						
						if(inSlot.stackSize + itemStack.stackSize <= max)
						{
							return true;
						}
						else {
							int rejects = (inSlot.stackSize + itemStack.stackSize) - max;

							if(rejects < itemStack.stackSize)
							{
								return true;
							}
						}
					}
				}
			}
		}
		else if(tileEntity instanceof IInventory)
		{
			IInventory inventory = checkChestInv((IInventory)tileEntity);
			
			for(int i = 0; i <= inventory.getSizeInventory() - 1; i++)
			{
				if(!force)
				{
					if(!inventory.isItemValidForSlot(i, itemStack))
					{
						continue;
					}
				}

				ItemStack inSlot = inventory.getStackInSlot(i);

				if(inSlot == null)
				{
					if(itemStack.stackSize <= inventory.getInventoryStackLimit())
					{
						return true;
					}
					else {
						int rejects = itemStack.stackSize - inventory.getInventoryStackLimit();
						
						if(rejects < itemStack.stackSize)
						{
							return true;
						}
					}
				}
				else if(areItemsStackable(itemStack, inSlot) && inSlot.getCount() < Math.min(inSlot.getMaxStackSize(), inventory.getInventoryStackLimit()))
				{
					int max = Math.min(inSlot.getMaxStackSize(), inventory.getInventoryStackLimit());
					
					if(inSlot.stackSize + itemStack.stackSize <= max)
					{
						return true;
					}
					else {
						int rejects = (inSlot.stackSize + itemStack.stackSize) - max;

						if(rejects < itemStack.stackSize)
						{
							return true;
						}
					}
				}
			}
		}

		return false;
	}
	
	public static ItemStack loadFromNBT(NBTTagCompound nbtTags)
	{
		ItemStack ret = new ItemStack(nbtTags);
		return ret;
	}
	
	public static boolean isItemHandler(TileEntity tile, EnumFacing side)
	{
		return CapabilityUtils.hasCapability(tile, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
	}
	
	public static IItemHandler getItemHandler(TileEntity tile, EnumFacing side)
	{
		return CapabilityUtils.getCapability(tile, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
	}

	/*TODO From CCLib -- go back to that version when we're using dependencies again*/
	public static boolean canStack(ItemStack stack1, ItemStack stack2) {
		return stack1 == null || stack2 == null ||
				(stack1.getItem() == stack2.getItem() &&
						(!stack2.getHasSubtypes() || stack2.getItemDamage() == stack1.getItemDamage()) &&
						ItemStack.areItemStackTagsEqual(stack2, stack1)) &&
						stack1.isStackable();
	}
}
