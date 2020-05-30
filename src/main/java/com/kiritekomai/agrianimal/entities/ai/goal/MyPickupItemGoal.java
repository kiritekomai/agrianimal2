package com.kiritekomai.agrianimal.entities.ai.goal;

import javax.annotation.Nullable;

import com.kiritekomai.agrianimal.entities.AgrianimalEntity;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public class MyPickupItemGoal extends Goal{
	protected int runDelay;
	protected int timeoutCounter;
	private int maxStayTicks;
	private final AgrianimalEntity agriAnimal;
	public double movementSpeed;
	/** 1 => ongoing, -1 => none */
	private int currentTask;

	static final int maxSearchDist = 16;
	ItemEntity targetItemEntity;

	public MyPickupItemGoal(AgrianimalEntity farmerIn, double speedIn) {
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
				|| isInventoryFull((Inventory) this.agriAnimal.getInventory())) {
			return false;
		}

		if (!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.agriAnimal.world,
				this.agriAnimal)) {
			return false;
		}
		this.targetItemEntity = getPickupTargetItem();
		if (this.targetItemEntity != null) {
			this.currentTask = -1;
			return true;

		}
		return false;
	}

	public void startExecuting() {
		this.agriAnimal.getNavigator().tryMoveToEntityLiving(getPickupTargetItem(), this.movementSpeed);
		this.timeoutCounter = 0;
		this.maxStayTicks = this.agriAnimal.getRNG().nextInt(this.agriAnimal.getRNG().nextInt(1200) + 1200) + 1200;

		this.currentTask = 1;
	}

	public boolean shouldContinueExecuting() {
		return this.currentTask >= 0 && this.timeoutCounter >= -this.maxStayTicks && this.timeoutCounter <= 1200
				&& (!this.targetItemEntity.removed || getPickupTargetItem() != null)
				&& !isInventoryFull((Inventory) this.agriAnimal.getInventory());
	}

	/**
	 * Keep ticking a continuous task that has already been started
	 */
	public void tick() {
		if (this.targetItemEntity.removed) {
			this.agriAnimal.getNavigator().clearPath();
			this.targetItemEntity = getPickupTargetItem();
			if (this.targetItemEntity == null) {
				this.currentTask = -1;
				this.runDelay = 10;
				return;
			}
			this.timeoutCounter = 0;
		}
		this.agriAnimal.getLookController().setLookPosition(this.targetItemEntity.getPosX(),
				this.targetItemEntity.getPosY(), this.targetItemEntity.getPosZ(), 10.0F,
				(float) this.agriAnimal.getVerticalFaceSpeed());

		if (this.agriAnimal.getDistance(this.targetItemEntity) > 1.0D) {
			//on going
			++this.timeoutCounter;
			this.agriAnimal.getNavigator().tryMoveToEntityLiving(this.targetItemEntity, this.movementSpeed);
			if (this.agriAnimal.getNavigator().noPath()) {
				//lost path
				this.currentTask = -1;
				this.runDelay = 10;
			}
		} else {
			//arrive
			--this.timeoutCounter;
			this.agriAnimal.getNavigator().clearPath();
			this.runDelay = 10;
		}
	}

	/**
	 * Returns false if the inventory has any room to place items in
	 */
	private boolean isInventoryFull(Inventory inventoryIn) {
		int i = inventoryIn.getSizeInventory();

		for (int j = 0; j < i; ++j) {
			ItemStack itemstack = inventoryIn.getStackInSlot(j);
			if (itemstack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Nullable
	private ItemEntity getPickupTargetItem() {
		if (!this.agriAnimal.world.isRemote && this.agriAnimal.canPickUpLoot()) {
			for (ItemEntity entityitem : this.agriAnimal.world.getEntitiesWithinAABB(ItemEntity.class,
					this.agriAnimal.getBoundingBox().grow(maxSearchDist, 1.0D, maxSearchDist))) {
				if (!entityitem.removed && !entityitem.getItem().isEmpty() && !entityitem.cannotPickup()) {
					if (this.agriAnimal.isPickupItem(entityitem.getItem().getItem())) {
						if (this.agriAnimal.getNavigator().getPathToEntity(entityitem,1) != null) {
							return entityitem;
						}
					}
				}
			}
		}
		return null;
	}
}
