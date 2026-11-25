package com.lody.virtual.client.hook.proxies.am;

import android.os.IBinder;
import android.os.IInterface;

import com.lody.virtual.client.hook.base.BinderInvocationStub;
import com.lody.virtual.client.hook.base.Inject;
import com.lody.virtual.client.hook.base.MethodInvocationProxy;
import com.lody.virtual.client.hook.base.MethodInvocationStub;

import mirror.android.app.ActivityTaskManager;
import mirror.android.os.ServiceManager;
import mirror.android.util.Singleton;

/**
 * Android 10+ : startActivity & friends are served by IActivityTaskManager.
 * Same method names as IActivityManager, so we reuse {@link MethodProxies}.
 */
@Inject(MethodProxies.class)
public class ActivityTaskManagerStub extends MethodInvocationProxy<MethodInvocationStub<IInterface>> {

    private static final String ACTIVITY_TASK_SERVICE = "activity_task";

    public ActivityTaskManagerStub() {
        super(new MethodInvocationStub<>(ActivityTaskManager.getService.call()));
    }

    @Override
    public void inject() throws Throwable {
        Object singleton = ActivityTaskManager.IActivityTaskManagerSingleton.get();
        Singleton.mInstance.set(singleton, getInvocationStub().getProxyInterface());
        try {
            BinderInvocationStub hookBinder = new BinderInvocationStub(getInvocationStub().getBaseInterface());
            hookBinder.copyMethodProxies(getInvocationStub());
            ServiceManager.sCache.get().put(ACTIVITY_TASK_SERVICE, (IBinder) hookBinder);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isEnvBad() {
        try {
            return ActivityTaskManager.getService.call() != getInvocationStub().getProxyInterface();
        } catch (Throwable e) {
            return false;
        }
    }
}
