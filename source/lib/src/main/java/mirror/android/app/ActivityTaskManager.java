package mirror.android.app;

import android.os.IInterface;

import mirror.RefClass;
import mirror.RefStaticMethod;
import mirror.RefStaticObject;

/**
 * Android 10+ : activity start calls moved from IActivityManager to IActivityTaskManager.
 */
public class ActivityTaskManager {
    public static Class<?> TYPE = RefClass.load(ActivityTaskManager.class, "android.app.ActivityTaskManager");

    public static RefStaticMethod<IInterface> getService;
    public static RefStaticObject<Object> IActivityTaskManagerSingleton;
}
