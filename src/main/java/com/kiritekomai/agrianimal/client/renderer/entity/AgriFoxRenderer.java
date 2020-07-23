package com.kiritekomai.agrianimal.client.renderer.entity;

import com.kiritekomai.agrianimal.Reference;
import com.kiritekomai.agrianimal.client.renderer.entity.layers.AgriFoxHeldItemLayer;
import com.kiritekomai.agrianimal.client.renderer.entity.model.AgriFoxModel;
import com.kiritekomai.agrianimal.entities.AgriFoxEntity;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AgriFoxRenderer extends MobRenderer<AgriFoxEntity, AgriFoxModel<AgriFoxEntity>> {
	private static final ResourceLocation FOX = new ResourceLocation(Reference.MOD_ID,"textures/entity/agri_fox/agri_fox.png");
	private static final ResourceLocation WORKING_FOX = new ResourceLocation(Reference.MOD_ID,"textures/entity/agri_fox/agri_fox_work.png");
	private static final ResourceLocation SLEEPING_FOX = new ResourceLocation(Reference.MOD_ID,"textures/entity/agri_fox/agri_fox_sleep.png");
	private static final ResourceLocation SNOW_FOX = new ResourceLocation(Reference.MOD_ID,"textures/entity/agri_fox/snow_agri_fox.png");
	private static final ResourceLocation WORKING_SNOW_FOX = new ResourceLocation(Reference.MOD_ID,"textures/entity/agri_fox/snow_agri_fox_work.png");
	private static final ResourceLocation SLEEPING_SNOW_FOX = new ResourceLocation(Reference.MOD_ID,"textures/entity/agri_fox/snow_agri_fox_sleep.png");

	public AgriFoxRenderer(EntityRendererManager renderManagerIn) {
		super(renderManagerIn, new AgriFoxModel<>(), 0.4F);
		this.addLayer(new AgriFoxHeldItemLayer(this));
	}

	protected void applyRotations(AgriFoxEntity entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
		super.applyRotations(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
		if (entityLiving.func_213480_dY() || entityLiving.isStuck()) {
			float f = -MathHelper.lerp(partialTicks, entityLiving.prevRotationPitch, entityLiving.rotationPitch);
			matrixStackIn.rotate(Vector3f.XP.rotationDegrees(f));
		}

	}

	/**
	 * Returns the location of an entity's texture.
	 */
	public ResourceLocation getEntityTexture(AgriFoxEntity entity) {
		if (entity.getVariantType() == AgriFoxEntity.Type.RED) {
			if( entity.isHarvesting()) {
				return WORKING_FOX;
			}
			else if( entity.isSleeping()) {
				return SLEEPING_FOX;
			}
			else {
				return FOX;
			}
		} else {
			if( entity.isHarvesting()) {
				return WORKING_SNOW_FOX;
			}
			else if( entity.isSleeping()) {
				return SLEEPING_SNOW_FOX;
			}
			else {
				return SNOW_FOX;
			}
		}
	}
}