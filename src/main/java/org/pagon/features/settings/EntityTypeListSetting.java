package org.pagon.features.settings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EntityTypeListSetting extends Setting<List<EntityType<?>>> {
    public EntityTypeListSetting(String name) {
        super(name, new ArrayList<>());
    }

    @Override
    public String getType() {
        return "EntityTypeList";
    }

    @Override
    public String getValueAsString() {
        return getValue().stream()
                .map(BuiltInRegistries.ENTITY_TYPE::getKey)
                .map(Identifier::toString)
                .collect(Collectors.joining(","));
    }
}
