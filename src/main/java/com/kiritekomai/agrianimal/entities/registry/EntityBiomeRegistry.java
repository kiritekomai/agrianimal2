package com.kiritekomai.agrianimal.entities.registry;

import com.kiritekomai.agrianimal.Reference;
import net.minecraft.entity.EntityClassification;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class EntityBiomeRegistry {

    @SubscribeEvent
    public static void onBiomeLoading(BiomeLoadingEvent event) {

        String currentBiome = event.getName().toString();

        String[] spawnbiome = {
                Biomes.TAIGA.getLocation().toString(),
                Biomes.TAIGA_HILLS.getLocation().toString(),
                Biomes.TAIGA_MOUNTAINS.getLocation().toString(),
                Biomes.GIANT_TREE_TAIGA.getLocation().toString(),
                Biomes.GIANT_SPRUCE_TAIGA.getLocation().toString(),
                Biomes.GIANT_TREE_TAIGA_HILLS.getLocation().toString(),
                Biomes.GIANT_SPRUCE_TAIGA_HILLS.getLocation().toString(),
                Biomes.SNOWY_TAIGA.getLocation().toString(),
                Biomes.SNOWY_TAIGA_HILLS.getLocation().toString(),
                Biomes.SNOWY_TAIGA_MOUNTAINS.getLocation().toString(),
                //Biomes.PLAINS.getLocation().toString(),   //for debug
                //Biomes.DESERT.getLocation().toString()    //for debug
        };
        if (Arrays.asList(spawnbiome).contains(currentBiome)) {

            event.getSpawns().getSpawner(EntityClassification.CREATURE)
                    .add(new MobSpawnInfo.Spawners(EntityRegistry.AGRI_FOX, 2, 4, 8));
        }
    }
}
