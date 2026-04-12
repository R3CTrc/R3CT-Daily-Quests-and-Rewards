package com.r3ct.quests.mixin;

import com.r3ct.quests.logic.QuestManager;
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

    @Inject(method = "spawnChildFromBreeding", at = @At("TAIL"))
    private void onBreed(ServerLevel level, Animal otherParent, CallbackInfo ci) {
        Animal thisAnimal = (Animal)(Object)this;
        ServerPlayer player = thisAnimal.getLoveCause();
        if (player == null) player = otherParent.getLoveCause();
        if (player != null) {
            String thisId = BuiltInRegistries.ENTITY_TYPE.getKey(thisAnimal.getType()).toString();
            String otherId = BuiltInRegistries.ENTITY_TYPE.getKey(otherParent.getType()).toString();
            if ((thisId.contains("horse") && otherId.contains("donkey")) || (thisId.contains("donkey") && otherId.contains("horse"))) {
                QuestManager.handleAction(player, "BREED_ANIMAL", "minecraft:mule", 1);
            } else {
                QuestManager.handleAction(player, "BREED_ANIMAL", thisId, 1);
            }
            QuestManager.handleAction(player, "BREED_ANIMAL", "any", 1);
        }
    }
}