package com.kiritekomai.agrianimal.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.world.World;

public abstract class AgrianimalEntity extends TameableEntity{
	public AgrianimalEntity(EntityType<? extends TameableEntity> type, World worldIn) {
        super(type, worldIn);
       // this.setTamed(false);
    }
}
