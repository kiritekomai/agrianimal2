package com.kiritekomai.agrianimal.entities.ai.goal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import com.kiritekomai.agrianimal.entities.AgrianimalEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventoryProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.world.World;

public class MyPutItemGoal  extends Goal{
	protected int runDelay;
	protected int timeoutCounter;
	private int maxStayTicks;
	/** Villager that is harvesting */
	private final AgrianimalEntity agriAnimal;
	private BlockPos destinationBlock;
	private BlockPos targetInventoryBlockPos;
	public double movementSpeed;
	/** 0 => harvest, 1 => replant, -1 => none */
	private int currentTask;

	static final int maxSearchDist = 16;

	public MyPutItemGoal(AgrianimalEntity farmerIn, double speedIn) {
		this.agriAnimal = farmerIn;
		movementSpeed = speedIn;
		//this.setMutexBits(5);
	}

	/**
	 * Returns whether the EntityAIBase should begin execution.
	 */
	public boolean shouldExecute() {
		if (this.runDelay > 0) {
			--this.runDelay;
			return false;
		}
		this.runDelay = 120 + this.agriAnimal.getRNG().nextInt(180);

		if (!this.agriAnimal.isHarvesting()
				|| !isInventoryFull((IInventory) this.agriAnimal.getInventory())) {
			return false;
		}

		if (!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.agriAnimal.world,
				this.agriAnimal)) {
			return false;
		}
		ArrayList<BlockPos> list = this.getNearNotFullInventoryBlockPos();

		if (canGoNearInventoryBlockPos(list)) {
			this.currentTask = -1;
			return true;

		}
		return false;
	}

	private ArrayList<BlockPos> getNearNotFullInventoryBlockPos() {
		ArrayList<BlockPos> list = new ArrayList<BlockPos>();
		BlockPos blockpos = new BlockPos(this.agriAnimal.getPosX(), this.agriAnimal.getPosY() + 0.2D, this.agriAnimal.getPosZ());//農地はY軸で微妙に下がっていて切り捨てだと1ブロック下判定なのでハーフブロックで誤検知しない程度に適当にかさまし
		BlockPos.Mutable blockpos$mutableblockpos = new BlockPos.Mutable();

		//Search all iinventory within search distance
		for (int x = -maxSearchDist; x <= maxSearchDist; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -maxSearchDist; z <= maxSearchDist; z++) {
					blockpos$mutableblockpos.setPos(blockpos).move(x, y, z);

					IInventory iinventory = getInventoryAtPosition(this.agriAnimal.world, blockpos$mutableblockpos);
					if (iinventory != null && !this.isInventoryFull(iinventory)) {
						list.add(new BlockPos(blockpos$mutableblockpos));
					}
				}
			}
		}
		//sort list order by entity's position
		Collections.sort(list, new Comparator<BlockPos>() {
			@Override
			public int compare(BlockPos obj1, BlockPos obj2) {
				double d1 = obj1.distanceSq((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ(),true);
				double d2 = obj2.distanceSq((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ(),true);
				if (d1 < d2) {
					return -1;
				} else if (d1 > d2) {
					return 1;
				}
				return 0;
			}
		});
		return list;
	}

	private boolean canGoNearInventoryBlockPos(ArrayList<BlockPos> iinventoryBlockPosList) {

		//PathFinder path_finder = new PathFinder(new WalkNodeProcessor());

		for (BlockPos chkPos : iinventoryBlockPosList) {
			ArrayList<BlockPos> list = new ArrayList<BlockPos>();
			BlockPos.Mutable blockpos$mutableblockpos = new BlockPos.Mutable();

			for (int yoffset = 0; yoffset <= 1; yoffset = yoffset > 0 ? -yoffset : 1 - yoffset) {
				for (int xoffset = 0; xoffset <= 1; xoffset = xoffset > 0 ? -xoffset : 1 - xoffset) {
					for (int zoffset = 0; zoffset <= 1; zoffset = zoffset > 0 ? -zoffset : 1 - zoffset) {
						blockpos$mutableblockpos.setPos(chkPos).move(xoffset, yoffset, zoffset);
						BlockState iblockstate = agriAnimal.world.getBlockState(blockpos$mutableblockpos);
						Block block = iblockstate.getBlock();
						// don't moves to the block which has a VoxelShape
						if (block.getShape(iblockstate, agriAnimal.world, blockpos$mutableblockpos, ISelectionContext.forEntity(this.agriAnimal)).isEmpty()) {
							blockpos$mutableblockpos.setPos(chkPos).move(xoffset, yoffset - 1, zoffset);
							iblockstate = agriAnimal.world.getBlockState(blockpos$mutableblockpos);
							block = iblockstate.getBlock();
							if (!block.getShape(iblockstate, agriAnimal.world, blockpos$mutableblockpos, ISelectionContext.forEntity(this.agriAnimal)).isEmpty()) {
								if (this.agriAnimal.getNavigator().getPathToPos(
										(double) ((float) blockpos$mutableblockpos.getX()) + 0.5D,
										(double) ((float) blockpos$mutableblockpos.getY()) + 1.0D,
										(double) ((float) blockpos$mutableblockpos.getZ()) + 0.5D,
										1) != null) {
									/*if (path_finder.findPath(this.agriAnimal.world, this.agriAnimal, blockpos$mutableblockpos,
																	maxSearchDist) != null) {*/
									//exists path to the destination block
									list.add(new BlockPos(blockpos$mutableblockpos));
								}
							}
						}
					}
				}
			}
			if (!list.isEmpty()) {
				//select a nearest position
				BlockPos blockpos = new BlockPos(this.agriAnimal.getPositionVec());
				Collections.sort(list, new Comparator<BlockPos>() {
					@Override
					public int compare(BlockPos obj1, BlockPos obj2) {
						double d1 = obj1.distanceSq((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ(),true);
						double d2 = obj2.distanceSq((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ(), true);
						if (d1 < d2) {
							return -1;
						} else if (d1 > d2) {
							return 1;
						}

						return 0;
					}
				});
				this.destinationBlock = list.get(0);
				this.targetInventoryBlockPos = chkPos;
				return true;
			}

		}
		return false;
	}

	public void startExecuting() {
		this.agriAnimal.getNavigator().tryMoveToXYZ((double) ((float) this.destinationBlock.getX()) + 0.5D,
				(double) (this.destinationBlock.getY() + 1), (double) ((float) this.destinationBlock.getZ()) + 0.5D,
				this.movementSpeed);
		this.timeoutCounter = 0;
		this.maxStayTicks = this.agriAnimal.getRNG().nextInt(this.agriAnimal.getRNG().nextInt(1200) + 1200) + 1200;

		this.currentTask = 1;
	}

	public boolean shouldContinueExecuting() {
		return this.currentTask >= 0 && this.timeoutCounter >= -this.maxStayTicks && this.timeoutCounter <= 1200;
	}

	/**
	 * Keep ticking a continuous task that has already been started
	 */
	public void tick() {
		this.agriAnimal.getLookController().setLookPosition((double) this.destinationBlock.getX() + 0.5D,
				(double) (this.destinationBlock.getY() + 0.5D), (double) this.destinationBlock.getZ() + 0.5D, 10.0F,
				(float) this.agriAnimal.getVerticalFaceSpeed());

		/*if (this.agriAnimal.getNavigator().noPath()) {
			//lost path
			this.currentTask = -1;
			this.runDelay = 10;
			return;
		} else */
		double dx = this.agriAnimal.getPosX() - ((double) destinationBlock.getX() + 0.5D);
		double dy = this.agriAnimal.getPosY() + 0.5D - ((double) destinationBlock.getY() + 0.5D);
		double dz = this.agriAnimal.getPosZ() - ((double) destinationBlock.getZ() + 0.5D);
		double dist_2 = dx * dx + dy * dy + dz * dz;
		if (dist_2 > 1.5D) {
			//on going
			++this.timeoutCounter;
			this.agriAnimal.getNavigator().tryMoveToXYZ((double) ((float) this.destinationBlock.getX()) + 0.5D,
					(double) (this.destinationBlock.getY()) + 1.0D,
					(double) ((float) this.destinationBlock.getZ()) + 0.5D, this.movementSpeed);
			if (this.agriAnimal.getNavigator().noPath()) {
				//lost path
				this.currentTask = -1;
				this.runDelay = 10;
			}
		} else {
			//arrive
			--this.timeoutCounter;

			//put items
			IInventory targetInventory = getInventoryAtPosition(this.agriAnimal.world, targetInventoryBlockPos);
			if (targetInventory != null) {
				this.transferItems((IInventory) this.agriAnimal.getInventory(), targetInventory);
			}
			this.agriAnimal.getNavigator().clearPath();
			this.currentTask = -1;
			this.runDelay = 10;
		}
	}

	@Nullable
	public static IInventory getInventoryAtPosition(World worldIn, BlockPos pos) {

		return getInventoryAtPosition(worldIn,pos.getX(),pos.getY(),pos.getZ());
	}


   @Nullable
   public static IInventory getInventoryAtPosition(World worldIn, double x, double y, double z) {
      IInventory iinventory = null;
      BlockPos blockpos = new BlockPos(x, y, z);
      BlockState blockstate = worldIn.getBlockState(blockpos);
      Block block = blockstate.getBlock();
      if (block instanceof ISidedInventoryProvider) {
         iinventory = ((ISidedInventoryProvider)block).createInventory(blockstate, worldIn, blockpos);
      } else if (blockstate.hasTileEntity()) {
         TileEntity tileentity = worldIn.getTileEntity(blockpos);
         if (tileentity instanceof IInventory) {
            iinventory = (IInventory)tileentity;
            if (iinventory instanceof ChestTileEntity && block instanceof ChestBlock) {
               iinventory = ChestBlock.getChestInventory((ChestBlock)block, blockstate, worldIn, blockpos, true);
            }
         }
      }

      if (iinventory == null) {
         List<Entity> list = worldIn.getEntitiesInAABBexcluding((Entity)null, new AxisAlignedBB(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D), EntityPredicates.HAS_INVENTORY);
         if (!list.isEmpty()) {
            iinventory = (IInventory)list.get(worldIn.rand.nextInt(list.size()));
         }
      }

      return iinventory;
   }

	/**
	 * Returns false if the inventory has any room to place items in
	 */
	private boolean isInventoryFull(IInventory inventoryIn) {
		int i = inventoryIn.getSizeInventory();

		for (int j = 0; j < i; ++j) {
			ItemStack itemstack = inventoryIn.getStackInSlot(j);
			if (itemstack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	private void transferItems(IInventory iinventoryFrom, IInventory iinventoryTo) {
		//if (net.minecraftforge.items.VanillaInventoryCodeHooks.insertHook(this)) return;
		if (iinventoryFrom == null || iinventoryTo == null) {
			return;
		}
		if (this.isInventoryFull(iinventoryTo)) {
			return;
		}
		for (int i = 0; i < iinventoryFrom.getSizeInventory(); ++i) {
			if (!iinventoryFrom.getStackInSlot(i).isEmpty()) {
				ItemStack itemstack = iinventoryFrom.getStackInSlot(i).copy();
				Item item = itemstack.getItem();
				boolean needPut = false;
				if (!this.agriAnimal.isFarmableItem(item)) {
					needPut = true;
				} else {
					for (int j = 0; j < i; j++) {
						if (item == iinventoryFrom.getStackInSlot(j).getItem()) {
							needPut = true;
							break;
						}
					}
				}
				if (needPut) {
					ItemStack itemstack1 = putStackInInventoryAllSlots(iinventoryFrom, iinventoryTo, itemstack);
					iinventoryFrom.setInventorySlotContents(i, itemstack1);
					if (itemstack1.isEmpty()) {
						iinventoryFrom.markDirty();
					}
				}
			}
		}

	}

	/**
	 * Attempts to place the passed stack in the inventory, using as many slots as required. Returns leftover items
	 */
	public static ItemStack putStackInInventoryAllSlots(@Nullable IInventory source, IInventory destination,
			ItemStack stack) {
		int i = destination.getSizeInventory();

		for (int j = 0; j < i && !stack.isEmpty(); ++j) {
			stack = insertStack(source, destination, stack, j);
		}

		return stack;
	}

	/**
	 * Insert the specified stack to the specified inventory and return any leftover items
	 */
	private static ItemStack insertStack(@Nullable IInventory source, IInventory destination, ItemStack stack,
			int index) {
		ItemStack itemstack = destination.getStackInSlot(index);
		if (canInsertItemInSlot(destination, stack, index)) {
			boolean flag = false;
			if (itemstack.isEmpty()) {
				destination.setInventorySlotContents(index, stack);
				stack = ItemStack.EMPTY;
				flag = true;
			} else if (canCombine(itemstack, stack)) {
				int i = stack.getMaxStackSize() - itemstack.getCount();
				int j = Math.min(stack.getCount(), i);
				stack.shrink(j);
				itemstack.grow(j);
				flag = j > 0;
			}

			if (flag) {
				destination.markDirty();
			}
		}

		return stack;
	}

	/**
	 * Can this hopper insert the specified item from the specified slot on the specified side?
	 */
	private static boolean canInsertItemInSlot(IInventory inventoryIn, ItemStack stack, int index) {
		if (!inventoryIn.isItemValidForSlot(index, stack)) {
			return false;
		}
		return true;
	}

	private static boolean canCombine(ItemStack stack1, ItemStack stack2) {
		if (stack1.getItem() != stack2.getItem()) {
			return false;
		} else if (stack1.getDamage() != stack2.getDamage()) {
			return false;
		} else if (stack1.getCount() > stack1.getMaxStackSize()) {
			return false;
		} else {
			return ItemStack.areItemStackTagsEqual(stack1, stack2);
		}
	}
}
