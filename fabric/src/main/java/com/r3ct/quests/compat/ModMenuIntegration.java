package com.r3ct.quests.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import com.r3ct.quests.config.R3CTQuestsConfig;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.translatable("r3ct.config.title"));

            ConfigCategory general = builder.getOrCreateCategory(Component.translatable("r3ct.config.category.hud"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("r3ct.config.entry.enable_hud"), R3CTQuestsConfig.getInstance().enableHud)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> R3CTQuestsConfig.getInstance().enableHud = newValue)
                    .build());

            general.addEntry(entryBuilder.startIntField(Component.translatable("r3ct.config.entry.hud_x"), R3CTQuestsConfig.getInstance().hudXOffset)
                    .setDefaultValue(10)
                    .setSaveConsumer(newValue -> R3CTQuestsConfig.getInstance().hudXOffset = newValue)
                    .build());

            general.addEntry(entryBuilder.startIntField(Component.translatable("r3ct.config.entry.hud_y"), R3CTQuestsConfig.getInstance().hudYOffset)
                    .setDefaultValue(70)
                    .setSaveConsumer(newValue -> R3CTQuestsConfig.getInstance().hudYOffset = newValue)
                    .build());

            builder.setSavingRunnable(() -> {
                R3CTQuestsConfig.save();
            });

            return builder.build();
        };
    }
}