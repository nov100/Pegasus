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
        if (args != null && args.length > 0 && args[0] instanceof String) {
            String pkg = (String) args[0];
            if (VirtualCore.get().isAppInstalled(pkg)) {
                args[0] = VirtualCore.get().getHostPkg();
            }
        } else if (Build.VERSION.SDK_INT >= 31 && args != null && args.length > 0
                && args[0] instanceof AttributionSource) {
            // phase2h: Android 12+ identifies the caller with an AttributionSource. The system
            // checks it against the real (host) uid. Pass a new object; the app keeps its own.
            String pkg = ((AttributionSource) args[0]).getPackageName();
            if (pkg != null && VirtualCore.get().isAppInstalled(pkg)) {
                args[0] = new AttributionSource.Builder(VirtualCore.get().myUid())
                        .setPackageName(VirtualCore.get().getHostPkg())
                        .build();
            }
        }
    }
}
