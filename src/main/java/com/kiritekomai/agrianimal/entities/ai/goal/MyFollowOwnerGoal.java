package com.kiritekomai.agrianimal.entities.ai.goal;

import com.kiritekomai.agrianimal.entities.AgrianimalEntity;

import net.minecraft.entity.ai.goal.FollowOwnerGoal;

public class MyFollowOwnerGoal extends FollowOwnerGoal{
	private final AgrianimalEntity agriAnimal;

	   public MyFollowOwnerGoal(AgrianimalEntity farmerIn, double p_i225711_2_, float p_i225711_4_, float p_i225711_5_, boolean p_i225711_6_) {
		   super( farmerIn,  p_i225711_2_,  p_i225711_4_,  p_i225711_5_,  p_i225711_6_);
			this.agriAnimal = farmerIn;
	   }

	   public boolean shouldExecute() {
		   if( this.agriAnimal.isHarvesting()) {
			   return false;
		   }
		   return super.shouldExecute();
	   }
	   public boolean shouldContinueExecuting() {
		   if( this.agriAnimal.isHarvesting()) {
			   return false;
		   }
		   return super.shouldContinueExecuting();
	   }
}
