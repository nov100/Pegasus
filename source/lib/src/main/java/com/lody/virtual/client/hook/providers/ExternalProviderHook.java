package com.lody.virtual.client.hook.providers;

import android.content.AttributionSource;
import android.os.Build;

import com.lody.virtual.client.core.VirtualCore;

import java.lang.reflect.Method;

public class ExternalProviderHook extends ProviderHook {

    public ExternalProviderHook(Object base) {
        super(base);
    }

    @Override
    protected void processArgs(Method method, Object... args) {
        if (args == null || args.length == 0) {
            return;
        }

        if (args[0] instanceof String) {
            String pkg = (String) args[0];
            if (VirtualCore.get().isAppInstalled(pkg)) {
                args[0] = VirtualCore.get().getHostPkg();
            }
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && args[0] instanceof AttributionSource) {
            AttributionSource source = (AttributionSource) args[0];
            AttributionSource.Builder builder =
                    new AttributionSource.Builder(VirtualCore.get().myUid())
                            .setPackageName(VirtualCore.get().getHostPkg());
            if (source.getAttributionTag() != null) {
                builder.setAttributionTag(source.getAttributionTag());
            }
            args[0] = builder.build();
        }
    }
}
