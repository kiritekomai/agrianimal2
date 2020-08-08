package com.kiritekomai.agrianimal.entities;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import com.kiritekomai.agrianimal.Reference;
import net.minecraft.block.CropsBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockNamedItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public abstract class AgrianimalEntity extends TameableEntity{
	private static final Set<Item> FARMABLE_ITEMS = ImmutableSet.of(Items.POTATO, Items.CARROT, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS);
	private static final DataParameter<Boolean> HAEVESTING = EntityDataManager.createKey(AgrianimalEntity.class,DataSerializers.BOOLEAN);
	private final Inventory myInventory  = new Inventory(8);

	public AgrianimalEntity(EntityType<? extends TameableEntity> type, World worldIn) {
		super(type, worldIn);
		this.setCanPickUpLoot(true);
		this.setTamed(false);
	}

	protected void registerData() {
		super.registerData();
		this.dataManager.register(HAEVESTING, false);
	}

	public void writeAdditional(CompoundNBT compound) {
		super.writeAdditional(compound);

		compound.putBoolean("Harvesting", this.isHarvesting());

		ListNBT listnbt = new ListNBT();

		for(int i = 0; i < this.myInventory .getSizeInventory(); ++i) {
			ItemStack itemstack = this.myInventory .getStackInSlot(i);
			if (!itemstack.isEmpty()) {
				listnbt.add(itemstack.write(new CompoundNBT()));
			}
		}

		compound.put("Inventory", listnbt);
	}

	/**
	 * (abstract) Protected helper method to read subclass entity data from NBT.
	 */
	public void readAdditional(CompoundNBT compound) {
		super.readAdditional(compound);

		this.setHarvesting(compound.getBoolean("Harvesting"));

		ListNBT listnbt = compound.getList("Inventory", 10);

		for(int i = 0; i < listnbt.size(); ++i) {
			ItemStack itemstack = ItemStack.read(listnbt.getCompound(i));
			if (!itemstack.isEmpty()) {
				this.myInventory .addItem(itemstack);
			}
		}

	}

	public Inventory getInventory () {
		return this.myInventory ;
	}

	public void dropInventoryItems() {

		float f1 = this.rotationYawHead;
		float f2 = this.rotationPitch;
		double mx = (double) (-MathHelper.sin(f1 * ((float) Math.PI / 180F))
				* MathHelper.cos(f2 * ((float) Math.PI / 180F)) * 0.3F);
		double my = (double) (MathHelper.cos(f1 * ((float) Math.PI / 180F))
				* MathHelper.cos(f2 * ((float) Math.PI / 180F)) * 0.3F);
		double mz = (double) (-MathHelper.sin(f2 * ((float) Math.PI / 180F)) * 0.3F + 0.1F);

		for (int i = 0; i < this.myInventory.getSizeInventory(); ++i) {
			ItemStack itemstack = this.myInventory.getStackInSlot(i);
			if (!itemstack.isEmpty()) {
				ItemEntity entityitem = new ItemEntity(this.world, this.getPosX(), this.getPosY() + 0.5F, this.getPosZ(),
						itemstack.copy());
				entityitem.setMotion(mx,my,mz);
				entityitem.setDefaultPickupDelay();
				if (this.world.addEntity(entityitem)) {
					itemstack.setCount(0);
				}
			}
			this.myInventory.setInventorySlotContents(i, itemstack);
			this.myInventory.markDirty();
		}

	}

	public boolean isFarmItemInInventory() {
		for (int i = 0; i < this.myInventory.getSizeInventory(); ++i) {
			Item item = this.myInventory.getStackInSlot(i).getItem();
			if (this.isFarmableItem(item)) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected void updateEquipmentIfNeeded(ItemEntity itemEntity) {
		ItemStack itemstack = itemEntity.getItem();
		Item item = itemstack.getItem();
		if (this.isFarmableItem(item) || this.isPickupItem(item)) {
			ItemStack itemstack1 = this.myInventory.addItem(itemstack);
			if (itemstack1.isEmpty()) {
				itemEntity.remove();
			} else {
				itemstack.setCount(itemstack1.getCount());
			}
		}

	}

	public boolean isFarmableItem(Item itemIn) {
		return itemIn instanceof BlockNamedItem && ((BlockNamedItem)itemIn).getBlock() instanceof CropsBlock;
//				FARMABLE_ITEMS.contains(itemIn);
	}

	public boolean isPickupItem(Item itemIn) {
		return true;
	}

	public void setHarvesting(boolean harvesting) {
		this.dataManager.set(HAEVESTING, harvesting);
	}

	public boolean isHarvesting() {
		return this.dataManager.get(HAEVESTING);
	}

	public boolean attackEntityFrom(DamageSource source, float amount) {
		if ((source == DamageSource.IN_WALL) && this.isHarvesting()) {
			return false;
		}
		return super.attackEntityFrom(source, amount);
	}

	public void onDeath(DamageSource cause) {
		this.dropInventoryItems();
		super.onDeath(cause);
	}

	public void livingTick() {
		this.world.getProfiler().startSection("looting");
		if (!this.world.isRemote && this.canPickUpLoot() && this.isAlive() && !this.dead && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.world, this)) {
			for(ItemEntity itementity : this.world.getEntitiesWithinAABB(ItemEntity.class, this.getBoundingBox().grow(1.5D, 1.5D, 1.5D))) {
				if (!itementity.removed && !itementity.getItem().isEmpty() && !itementity.cannotPickup()) {
					this.updateEquipmentIfNeeded(itementity);
				}
			}
		}
		this.world.getProfiler().endSection();
		super.livingTick();
	}

	@SubscribeEvent
	public static void onLivingAttack(LivingAttackEvent event) {
		if (!event.getEntity().getEntityWorld().isRemote
				&& event.getEntityLiving() instanceof AgrianimalEntity
				&& ((AgrianimalEntity)event.getEntityLiving()).isHarvesting()
				&&  ( event.getSource() == DamageSource.SWEET_BERRY_BUSH || event.getSource() == DamageSource.IN_WALL)) {
			event.setCanceled(true);
		}
	}
}
