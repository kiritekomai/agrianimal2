package com.kiritekomai.agrianimal.entities.ai.goal;

import com.kiritekomai.agrianimal.entities.AgrianimalEntity;

import net.minecraft.block.AttachedStemBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropsBlock;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.StemGrownBlock;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public class MyHarvestFarmland extends MoveToBlockGoal {
	/** Villager that is harvesting */
	private final AgrianimalEntity agriAnimal;
	private boolean hasFarmItem;
	/** 0 => harvest, 1 => replant, -1 => none */
	private int currentTask;

	static final int findPathMaxLength = 16;

	public MyHarvestFarmland(AgrianimalEntity farmerIn, double speedIn) {
		super(farmerIn, speedIn, findPathMaxLength);
		this.agriAnimal = farmerIn;
	}

	/**
	 * Returns whether the EntityAIBase should begin execution.
	 */
	public boolean shouldExecute() {
		if (!this.agriAnimal.isHarvesting()) {
			return false;
		}
		if (this.runDelay <= 0) {
			this.currentTask = -1;
			this.hasFarmItem = this.agriAnimal.isFarmItemInInventory();
		}

		return super.shouldExecute();
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	public boolean shouldContinueExecuting() {
		return this.currentTask >= 0 && super.shouldContinueExecuting();
	}

	/**
	 * Keep ticking a continuous task that has already been started
	 */
	public void tick() {
		super.tick();
		this.agriAnimal.getLookController().setLookPosition((double) this.destinationBlock.getX() + 0.5D,
				(double) (this.destinationBlock.getY() + 1), (double) this.destinationBlock.getZ() + 0.5D, 10.0F,
				(float) this.agriAnimal.getVerticalFaceSpeed());
		if (this.getIsAboveDestination() || this.destinationBlock.up().withinDistance(this.creature.getPositionVec(), 2.0D)) {
			World iworld = this.agriAnimal.world;
			BlockPos blockpos = this.destinationBlock.up();
			BlockState iblockstate = iworld.getBlockState(blockpos);
			Block block = iblockstate.getBlock();
			if (this.currentTask == 0 && block instanceof CropsBlock && ((CropsBlock) block).isMaxAge(iblockstate)) {
				iworld.destroyBlock(blockpos, true);
			} else if (this.currentTask == 0 && getStemGlownBlockPos(iworld, blockpos) != null) {
				iworld.destroyBlock(getStemGlownBlockPos(iworld, blockpos), true);
			} else if (this.currentTask == 0 && getReedBlockPos(iworld, blockpos) != null) {
				iworld.destroyBlock(getReedBlockPos(iworld, blockpos), true);
			} else if (this.currentTask == 1 && iblockstate.isAir()) {
				Inventory myinventory = this.agriAnimal.getInventory();

				for (int i = 0; i < myinventory.getSizeInventory(); ++i) {
					ItemStack itemstack = myinventory.getStackInSlot(i);
					boolean flag = false;
					if (!itemstack.isEmpty()) {
						if (itemstack.getItem() == Items.WHEAT_SEEDS) {
							iworld.setBlockState(blockpos, Blocks.WHEAT.getDefaultState(), 3);
							flag = true;
						} else if (itemstack.getItem() == Items.POTATO) {
							iworld.setBlockState(blockpos, Blocks.POTATOES.getDefaultState(), 3);
							flag = true;
						} else if (itemstack.getItem() == Items.CARROT) {
							iworld.setBlockState(blockpos, Blocks.CARROTS.getDefaultState(), 3);
							flag = true;
						} else if (itemstack.getItem() == Items.BEETROOT_SEEDS) {
							iworld.setBlockState(blockpos, Blocks.BEETROOTS.getDefaultState(), 3);
							flag = true;
						} else if (itemstack.getItem() instanceof net.minecraftforge.common.IPlantable) {
							if (((net.minecraftforge.common.IPlantable) itemstack.getItem()).getPlantType(iworld,
									blockpos) == net.minecraftforge.common.PlantType.Crop) {
								iworld.setBlockState(blockpos,
										((net.minecraftforge.common.IPlantable) itemstack.getItem()).getPlant(iworld,
												blockpos),
										3);
								flag = true;
							}
						}
					}

					if (flag) {
						itemstack.shrink(1);
						if (itemstack.isEmpty()) {
							myinventory.setInventorySlotContents(i, ItemStack.EMPTY);
						}
						break;
					}
				}
			}
			this.agriAnimal.getNavigator().clearPath();

			this.currentTask = -1;
			this.runDelay = 10;
		}

	}
	/**
	 * Return true to set given position as destination
	 */
	protected boolean shouldMoveTo(IWorldReader worldIn, BlockPos pos) {
		Block block = worldIn.getBlockState(pos).getBlock();
		pos = pos.up();

		int task = -1;
		boolean shouldGo = false;

		if (block == Blocks.FARMLAND) {
			BlockState iblockstate = worldIn.getBlockState(pos);
			block = iblockstate.getBlock();
			if (block instanceof CropsBlock && ((CropsBlock) block).isMaxAge(iblockstate)
					&& (this.currentTask == 0 || this.currentTask < 0)) {
				task = 0;
				shouldGo = true;
			}

			if (!shouldGo && iblockstate.isAir() && this.hasFarmItem
					&& (this.currentTask == 1 || this.currentTask < 0)) {
				task = 1;
				shouldGo = true;
			}
		}
		if (!shouldGo && (getStemGlownBlockPos(worldIn, pos) != null)
				&& (this.currentTask == 0 || this.currentTask < 0)) {
			task = 0;
			shouldGo = true;

		}
		if (!shouldGo && (getReedBlockPos(worldIn, pos) != null)
				&& (this.currentTask == 0 || this.currentTask < 0)) {
			task = 0;
			shouldGo = true;

		}
		GroundPathNavigator path_finder = new GroundPathNavigator(this.agriAnimal,this.agriAnimal.world);

		if (shouldGo) {
			if (path_finder.getPathToPos( pos, findPathMaxLength) != null) {
				//exits path to the destination block
				this.currentTask = task;
				return true;
			}
		}

		return false;
	}

	protected BlockPos getStemGlownBlockPos(IWorldReader worldIn, BlockPos pos) {
		Block block = worldIn.getBlockState(pos).getBlock();
		BlockState iblockstate = worldIn.getBlockState(pos);

		if (block instanceof AttachedStemBlock) {
			BlockPos facing_pos = pos.offset(iblockstate.get(HorizontalBlock.HORIZONTAL_FACING));
			Block facing_block = worldIn.getBlockState(facing_pos).getBlock();
			if (facing_block instanceof StemGrownBlock) {
				return facing_pos;
			}
		}
		return null;
	}

	protected BlockPos getReedBlockPos(IWorldReader worldIn, BlockPos pos) {
		Block block = worldIn.getBlockState(pos).getBlock();

		if (block instanceof SugarCaneBlock) {
			BlockPos up_pos = pos.up();
			Block up_block = worldIn.getBlockState(up_pos).getBlock();
			if (up_block instanceof SugarCaneBlock) {
				return up_pos;
			}
		}
		return null;
	}
}
