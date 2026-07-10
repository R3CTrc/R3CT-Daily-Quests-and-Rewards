package com.r3ct.daily.mixin;

import com.r3ct.daily.logic.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class BredAnimalsMixin {

    @Inject(method = "spawnChildFromBreeding", at = @At("RETURN"))
    private void r3ct_daily$onBreed(ServerLevel level, Animal mate, CallbackInfo ci) {
        Animal animal = (Animal) (Object) this;

        ServerPlayer player = animal.getLoveCause();

        if (player == null) {
            player = mate.getLoveCause();
        }

        if (player != null) {
            String animalId1 = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()).toString();
            String animalId2 = BuiltInRegistries.ENTITY_TYPE.getKey(mate.getType()).toString();

            String targetId = animalId1;

            if ((animalId1.contains("horse") && animalId2.contains("donkey")) || (animalId1.contains("donkey") && animalId2.contains("horse"))) {
                targetId = "minecraft:mule";
            }

            QuestManager.handleAction(player, "BREED_ANIMAL", targetId, 1);
        }
    }
}