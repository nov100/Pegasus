package com.lody.virtual.client.hook.providers;

import android.content.AttributionSource;
import android.os.Build;

import com.lody.virtual.client.core.VirtualCore;

import java.lang.reflect.Method;

/**
 * @author Lody
 */
public class ExternalProviderHook extends ProviderHook {

    public ExternalProviderHook(Object base) {
        super(base);
    }

    @Override
    protected void processArgs(Method method, Object... args) {
        if (args == null || args.length == 0) {
            return;
        }

        // Older provider API: calling package is passed directly.
        if (args[0] instanceof String) {
            String pkg = (String) args[0];

            if (VirtualCore.get().isAppInstalled(pkg)) {
                args[0] = VirtualCore.get().getHostPkg();
            }
            return;
        }

        // Android 12+ provider APIs use AttributionSource.
        // External system providers see the real Binder caller (the VA host),
        // so do not pass the virtual/guest UID and package to the system.
        if (Build.VERSION.SDK_INT >= 31
                && args[0] instanceof AttributionSource) {

            AttributionSource source = (AttributionSource) args[0];

            AttributionSource.Builder builder =
                    new AttributionSource.Builder(VirtualCore.get().myUid())
                            .setPackageName(VirtualCore.get().getHostPkg());

            String attributionTag = source.getAttributionTag();
            if (attributionTag != null) {
                builder.setAttributionTag(attributionTag);
            }

            args[0] = builder.build();
        }
    }
}
