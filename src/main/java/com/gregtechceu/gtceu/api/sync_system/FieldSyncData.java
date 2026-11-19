package com.gregtechceu.gtceu.api.sync_system;

import com.gregtechceu.gtceu.api.sync_system.annotations.RerenderOnChanged;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.data_transformers.ValueTransformer;

import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Information about the sync behaviour of fields with sync annotations in ISyncManaged classes
 */
@ApiStatus.Internal
public final class FieldSyncData {

    public final String fieldName, nbtSaveKey;
    public final VarHandle handle;
    public final boolean triggerClientRerender;
    @Setter
    public @Nullable ValueTransformer<?> transformer;
    public final List<MethodHandle> changeListenerHandles;
    public final TypeDeclaration type;
    /** Whether a change of this field's value can be seen by comparing it: it is a simple immutable value. */
    public final boolean autoDetectable;

    private static boolean isSimpleValue(Class<?> type) {
        return type.isPrimitive() || type.isEnum() || Number.class.isAssignableFrom(type) ||
                type == Boolean.class || type == Character.class || type == String.class ||
                type == java.util.UUID.class || type == net.minecraft.resources.ResourceLocation.class ||
                type == net.minecraft.core.BlockPos.class ||
                type == net.minecraft.world.level.block.state.BlockState.class ||
                net.minecraft.network.chat.Component.class.isAssignableFrom(type);
    }

    public FieldSyncData(Field field, VarHandle handle, @Nullable ValueTransformer<?> transformer,
                         List<MethodHandle> changeListenerHandles) {
        fieldName = field.getName();
        SaveField saveField = field.getAnnotation(SaveField.class);
        this.nbtSaveKey = (saveField != null && !saveField.nbtKey().isBlank()) ? saveField.nbtKey() : fieldName;
        this.handle = handle;
        this.triggerClientRerender = field.isAnnotationPresent(RerenderOnChanged.class);
        this.changeListenerHandles = changeListenerHandles;
        this.transformer = transformer;
        this.type = new TypeDeclaration(field.getGenericType());
        this.autoDetectable = isSimpleValue(field.getType());
    }
}
