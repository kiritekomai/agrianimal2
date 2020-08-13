package com.kiritekomai.agrianimal.entities.ai.goal;

import com.kiritekomai.agrianimal.entities.AgriFoxEntity;
import com.kiritekomai.agrianimal.entities.AgrianimalEntity;

import net.minecraft.block.*;
import net.minecraft.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockNamedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public class MyHarvestFarmland extends MoveToBlockGoal {
    /**
     * Villager that is harvesting
     */
    private final AgrianimalEntity agriAnimal;
    private boolean hasFarmItem;
    /**
     * 0 => harvest, 1 => replant(farmland), 2 => replant(soulsand), -1 => none
     */
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
            } else if (isMatureNetherWart(iworld, blockpos)) {
                iworld.destroyBlock(blockpos, true);
            } else if (this.currentTask == 0 && getStemGlownBlockPos(iworld, blockpos) != null) {
                iworld.destroyBlock(getStemGlownBlockPos(iworld, blockpos), true);
            } else if (this.currentTask == 0 && getReedBlockPos(iworld, blockpos) != null) {
                iworld.destroyBlock(getReedBlockPos(iworld, blockpos), true);
            } else if (this.currentTask == 0 && getSweetBerryBushBlockPos(iworld, blockpos) != null) {
                harvestSweetBerry(iworld, blockpos, iblockstate);
            } else if (this.currentTask >= 1 && iblockstate.isAir()) {
                Inventory myinventory = this.agriAnimal.getInventory();

                for (int i = 0; i < myinventory.getSizeInventory(); ++i) {
                    ItemStack itemstack = myinventory.getStackInSlot(i);
                    boolean flag = false;
                    if (!itemstack.isEmpty()) {
                        if (this.currentTask == 1) {
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
                            } else if (this.agriAnimal.isFarmableItem(itemstack.getItem())) {
                                iworld.setBlockState(blockpos,
                                        ((CropsBlock) ((BlockNamedItem) (itemstack.getItem())).getBlock()).getDefaultState(),
                                        3);
                                flag = true;
                            }
                        } else if (this.currentTask == 2) {
                            if (itemstack.getItem() == Items.NETHER_WART) {
                                iworld.setBlockState(blockpos, Blocks.NETHER_WART.getDefaultState(), 3);
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
        if (!shouldGo && block == Blocks.SOUL_SAND) {
            BlockState iblockstate = worldIn.getBlockState(pos);
            if (isMatureNetherWart(worldIn, pos)
                    && (this.currentTask == 0 || this.currentTask < 0)) {
                task = 0;
                shouldGo = true;
            }

            if (!shouldGo && iblockstate.isAir() && this.agriAnimal.hasItem(Items.NETHER_WART)
                    && (this.currentTask == 2 || this.currentTask < 0)) {
                task = 2;
                shouldGo = true;
            }
        }
        if (!shouldGo) {
            if ((getStemGlownBlockPos(worldIn, pos) != null)
                    && (this.currentTask == 0 || this.currentTask < 0)) {
                task = 0;
                shouldGo = true;

            } else if ((getReedBlockPos(worldIn, pos) != null)
                    && (this.currentTask == 0 || this.currentTask < 0)) {
                task = 0;
                shouldGo = true;

            } else if ((getSweetBerryBushBlockPos(worldIn, pos) != null)
                    && (this.currentTask == 0 || this.currentTask < 0)) {
                task = 0;
                shouldGo = true;

            }
        }
        GroundPathNavigator path_finder = new GroundPathNavigator(this.agriAnimal, this.agriAnimal.world);

        if (shouldGo) {
            if (path_finder.getPathToPos(pos, findPathMaxLength) != null) {
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

    protected BlockPos getSweetBerryBushBlockPos(IWorldReader worldIn, BlockPos pos) {
        BlockState blockstate = worldIn.getBlockState(pos);
        if (blockstate.isIn(Blocks.SWEET_BERRY_BUSH) && blockstate.get(SweetBerryBushBlock.AGE) >= 2) {
            return pos;
        }
        return null;
    }

    protected void harvestSweetBerry(World world, BlockPos pos, BlockState blockstate) {
        int i = blockstate.get(SweetBerryBushBlock.AGE);
        blockstate.with(SweetBerryBushBlock.AGE, Integer.valueOf(1));
        int j = 1 + world.rand.nextInt(2) + (i == 3 ? 1 : 0);
        Block.spawnAsEntity(world, pos, new ItemStack(Items.SWEET_BERRIES, j));

        this.agriAnimal.playSound(SoundEvents.ITEM_SWEET_BERRIES_PICK_FROM_BUSH, 1.0F, 1.0F);
        world.setBlockState(pos, blockstate.with(SweetBerryBushBlock.AGE, Integer.valueOf(1)), 2);
    }

    protected boolean isMatureNetherWart(IWorldReader worldIn, BlockPos pos) {
        BlockState blockstate = worldIn.getBlockState(pos);
        if (blockstate.isIn(Blocks.NETHER_WART) && blockstate.get(NetherWartBlock.AGE) >= 3) {
            return true;
        }
        return false;
    }
}
