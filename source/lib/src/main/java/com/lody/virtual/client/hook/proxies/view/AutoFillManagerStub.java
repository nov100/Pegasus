package com.lody.virtual.client.hook.proxies.view;

import android.annotation.SuppressLint;
import android.app.Application;
import android.util.Log;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import mirror.android.app.ActivityThread;
import mirror.android.view.IAutoFillManager;

public class AutoFillManagerStub extends BinderInvocationProxy {
    private static final String TAG = "AutoFillManagerStub";
    private static final String AUTO_FILL_NAME = "autofill";

    public AutoFillManagerStub() {
        super(IAutoFillManager.Stub.asInterface, AUTO_FILL_NAME);
    }

    @SuppressLint("WrongConstant")
    @Override
    public void inject() throws Throwable {
        super.inject();
        addPackageProxy("startSession");
        addPackageProxy("updateOrRestartSession");
        addPackageProxy("isServiceEnabled");

        Application application = ActivityThread.currentApplication.call();
        if (application == null) {
            Log.w(TAG, "Skip cached AutofillManager patch until Application is attached");
            return;
        }
        try {
            Object manager = application.getSystemService(AUTO_FILL_NAME);
            Object proxy = getInvocationStub().getProxyInterface();
            if (manager == null || proxy == null) {
                return;
            }
            Field service = manager.getClass().getDeclaredField("mService");
            service.setAccessible(true);
            service.set(manager, proxy);
        } catch (Throwable tr) {
            Log.w(TAG, "Unable to patch cached AutofillManager service: " + tr);
        }
    }

    private void addPackageProxy(final String name) {
        addMethodProxy(new MethodProxy() {
            @Override
            public String getMethodName() {
                return name;
            }

            @Override
            public boolean beforeCall(Object who, Method method, Object... args) {
                MethodParameterUtils.replaceLastAppPkg(args);
                return super.beforeCall(who, method, args);
            }
        });
    }
}
