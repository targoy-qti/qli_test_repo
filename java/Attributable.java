/*
 * Copyright (C) Android Open Source Project
 * SPDX-License-Identifier: GPL-2.0-only
 */


package android.bluetooth;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.AttributionSource;

import java.util.List;

/**
 * Marker interface for a class which can have an {@link AttributionSource}
 * assigned to it; these are typically {@link android.os.Parcelable} classes
 * which need to be updated after crossing Binder transaction boundaries.
 *
 * @hide
 */
public interface Attributable {
    /** @hide */
    void setAttributionSource(@NonNull AttributionSource attributionSource);

    /** @hide */
    static @Nullable <T extends Attributable> T setAttributionSource(
            @Nullable T attributable,
            @NonNull AttributionSource attributionSource) {
        if (attributable != null) {
            attributable.setAttributionSource(attributionSource);
        }
        return attributable;
    }
