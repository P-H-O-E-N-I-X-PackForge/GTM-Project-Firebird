package com.gregtechceu.gtceu.api.sync_system.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Instructs the sync system to sync any changes to this field with clients.
 * <p>
 * Changes to fields of simple immutable types, numbers, booleans, strings, enums, resource locations, block positions,
 * block states and text, are detected automatically each server tick. A field of any other type, a list or an object
 * that is changed in place, needs {@code getSyncDataHolder().markClientSyncFieldDirty(FIELD_NAME)} to be called to
 * include it in the next sync update. Marking a simple field as well does no harm.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SyncToClient {}
