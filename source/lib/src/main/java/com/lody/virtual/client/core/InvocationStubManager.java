package com.lody.virtual.client.core;

import android.os.Build;

import com.lody.virtual.client.hook.base.MethodInvocationProxy;
import com.lody.virtual.client.hook.base.MethodInvocationStub;
import com.lody.virtual.client.hook.delegate.AppInstrumentation;
import com.lody.virtual.client.hook.proxies.account.AccountManagerStub;
import com.lody.virtual.client.hook.proxies.alarm.AlarmManagerStub;
import com.lody.virtual.client.hook.proxies.am.ActivityManagerStub;
import com.lody.virtual.client.hook.proxies.am.HCallbackStub;
import com.lody.virtual.client.hook.proxies.appops.AppOpsManagerStub;
import com.lody.virtual.client.hook.proxies.appwidget.AppWidgetManagerStub;
import com.lody.virtual.client.hook.proxies.audio.AudioManagerStub;
import com.lody.virtual.client.hook.proxies.backup.BackupManagerStub;
import com.lody.virtual.client.hook.proxies.bluetooth.BluetoothStub;
import com.lody.virtual.client.hook.proxies.clipboard.ClipBoardStub;
import com.lody.virtual.client.hook.proxies.connectivity.ConnectivityStub;
import com.lody.virtual.client.hook.proxies.content.ContentServiceStub;
import com.lody.virtual.client.hook.proxies.context_hub.ContextHubServiceStub;
import com.lody.virtual.client.hook.proxies.devicepolicy.DevicePolicyManagerStub;
import com.lody.virtual.client.hook.proxies.display.DisplayStub;
import com.lody.virtual.client.hook.proxies.dropbox.DropBoxManagerStub;
import com.lody.virtual.client.hook.proxies.fingerprint.FingerprintManagerStub;
import com.lody.virtual.client.hook.proxies.graphics.GraphicsStatsStub;
import com.lody.virtual.client.hook.proxies.imms.MmsStub;
import com.lody.virtual.client.hook.proxies.input.InputMethodManagerStub;
import com.lody.virtual.client.hook.proxies.isms.ISmsStub;
import com.lody.virtual.client.hook.proxies.isub.ISubStub;
import com.lody.virtual.client.hook.proxies.job.JobServiceStub;
import com.lody.virtual.client.hook.proxies.libcore.LibCoreStub;
import com.lody.virtual.client.hook.proxies.location.LocationManagerStub;
import com.lody.virtual.client.hook.proxies.media.router.MediaRouterServiceStub;
import com.lody.virtual.client.hook.proxies.media.session.SessionManagerStub;
import com.lody.virtual.client.hook.proxies.mount.MountServiceStub;
import com.lody.virtual.client.hook.proxies.network.NetworkManagementStub;
import com.lody.virtual.client.hook.proxies.notification.NotificationManagerStub;
import com.lody.virtual.client.hook.proxies.persistent_data_block.PersistentDataBlockServiceStub;
import com.lody.virtual.client.hook.proxies.phonesubinfo.PhoneSubInfoStub;
import com.lody.virtual.client.hook.proxies.pm.PackageManagerStub;
import com.lody.virtual.client.hook.proxies.power.PowerManagerStub;
import com.lody.virtual.client.hook.proxies.restriction.RestrictionStub;
import com.lody.virtual.client.hook.proxies.search.SearchManagerStub;
import com.lody.virtual.client.hook.proxies.shortcut.ShortcutServiceStub;
import com.lody.virtual.client.hook.proxies.telephony.TelephonyRegistryStub;
import com.lody.virtual.client.hook.proxies.telephony.TelephonyStub;
import com.lody.virtual.client.hook.proxies.usage.UsageStatsManagerStub;
import com.lody.virtual.client.hook.proxies.user.UserManagerStub;
import com.lody.virtual.client.hook.proxies.vibrator.VibratorStub;
import com.lody.virtual.client.hook.proxies.view.AutoFillManagerStub;
import com.lody.virtual.client.hook.proxies.wifi.WifiManagerStub;
import com.lody.virtual.client.hook.proxies.wifi_scanner.WifiScannerStub;
import com.lody.virtual.client.hook.proxies.window.WindowManagerStub;
import com.lody.virtual.client.interfaces.IInjector;

import java.util.HashMap;
import java.util.Map;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;

/**
 * @author Lody
 *
 */
public final class InvocationStubManager {

    private static InvocationStubManager sInstance = new InvocationStubManager();
    private static boolean sInit;

	private Map<Class<?>, IInjector> mInjectors = new HashMap<>(13);

	private InvocationStubManager() {
	}

	public static InvocationStubManager getInstance() {
		return sInstance;
	}

	void injectAll() throws Throwable {
		for (IInjector injector : new java.util.ArrayList<>(mInjectors.values())) {
			try {
				injector.inject();
			} catch (Throwable e) {
				// one broken hook must not kill the whole process (old hooks vs. new Android)
				android.util.Log.e("VA++", "inject failed: " + injector.getClass().getName(), e);
			}
		}
		// XXX: Lazy inject the Instrumentation,
		safeAdd(new Factory() {
			@Override
			public IInjector create() throws Throwable {
				return AppInstrumentation.getDefault();
			}
		});
	}

    /**
	 * @return if the InvocationStubManager has been initialized.
	 */
	public boolean isInit() {
		return sInit;
	}


	public void init() throws Throwable {
		if (isInit()) {
			throw new IllegalStateException("InvocationStubManager Has been initialized.");
		}
		injectInternal();
		sInit = true;

	}

	private void injectInternal() throws Throwable {
		if (VirtualCore.get().isMainProcess()) {
			return;
		}
		if (VirtualCore.get().isServerProcess()) {
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new ActivityManagerStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new PackageManagerStub(); } });
			return;
		}
		if (VirtualCore.get().isVAppProcess()) {
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new LibCoreStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new ActivityManagerStub(); } });
			if (Build.VERSION.SDK_INT >= 29) {
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new com.lody.virtual.client.hook.proxies.am.ActivityTaskManagerStub(); } });
			}
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new PackageManagerStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return HCallbackStub.getDefault(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new ISmsStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new ISubStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new DropBoxManagerStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new NotificationManagerStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new LocationManagerStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new WindowManagerStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new ClipBoardStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new MountServiceStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new BackupManagerStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new TelephonyStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new TelephonyRegistryStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new PhoneSubInfoStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new PowerManagerStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new AppWidgetManagerStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new AccountManagerStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new AudioManagerStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new SearchManagerStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new ContentServiceStub(); } });
			safeAdd(new Factory() { public IInjector create() throws Throwable { return new ConnectivityStub(); } });

			if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR2) {
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new VibratorStub(); } });
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new WifiManagerStub(); } });
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new BluetoothStub(); } });
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new ContextHubServiceStub(); } });
			}
			if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new UserManagerStub(); } });
			}

			if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new DisplayStub(); } });
			}
			if (Build.VERSION.SDK_INT >= LOLLIPOP) {
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new PersistentDataBlockServiceStub(); } });
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new InputMethodManagerStub(); } });
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new MmsStub(); } });
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new SessionManagerStub(); } });
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new JobServiceStub(); } });
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new RestrictionStub(); } });
			}
			if (Build.VERSION.SDK_INT >= KITKAT) {
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new AlarmManagerStub(); } });
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new AppOpsManagerStub(); } });
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new MediaRouterServiceStub(); } });
			}
			if (Build.VERSION.SDK_INT >= LOLLIPOP_MR1) {
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new GraphicsStatsStub(); } });
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new UsageStatsManagerStub(); } });
			}
			if (Build.VERSION.SDK_INT >= M) {
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new FingerprintManagerStub(); } });
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new NetworkManagementStub(); } });
			}
			if (Build.VERSION.SDK_INT >= N) {
                safeAdd(new Factory() { public IInjector create() throws Throwable { return new WifiScannerStub(); } });
                safeAdd(new Factory() { public IInjector create() throws Throwable { return new ShortcutServiceStub(); } });
                safeAdd(new Factory() { public IInjector create() throws Throwable { return new DevicePolicyManagerStub(); } });
            }
            if (Build.VERSION.SDK_INT >= 26) {
				safeAdd(new Factory() { public IInjector create() throws Throwable { return new AutoFillManagerStub(); } });
			}
		}
	}

	private interface Factory {
		IInjector create() throws Throwable;
	}

	private void safeAdd(Factory factory) {
		try {
			IInjector injector = factory.create();
			if (injector != null) {
				addInjector(injector);
			}
		} catch (Throwable e) {
			android.util.Log.e("VA++", "create hook failed: " + e, e);
		}
	}

	private void addInjector(IInjector IInjector) {
		mInjectors.put(IInjector.getClass(), IInjector);
	}

	public <T extends IInjector> T findInjector(Class<T> clazz) {
		// noinspection unchecked
		return (T) mInjectors.get(clazz);
	}

	public <T extends IInjector> void checkEnv(Class<T> clazz) {
		IInjector IInjector = findInjector(clazz);
		if (IInjector != null && IInjector.isEnvBad()) {
			try {
				IInjector.inject();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	public <T extends IInjector, H extends MethodInvocationStub> H getInvocationStub(Class<T> injectorClass) {
		T injector = findInjector(injectorClass);
		if (injector != null && injector instanceof MethodInvocationProxy) {
			// noinspection unchecked
			return (H) ((MethodInvocationProxy) injector).getInvocationStub();
		}
		return null;
	}

}