package mirror.android.content;

import mirror.RefClass;
import mirror.RefObject;

/**
 * @author Lody
 *
 * mActions used to be an ArrayList<String> on older Android, but on newer
 * Android (seen on Android 15) the framework stores it as an
 * android.util.ArraySet<String> instead, which is a Set, not a List. Keep
 * this typed as Object so callers can check instanceof and handle either
 * shape instead of getting a ClassCastException from an implicit checkcast
 * to List here.
 */

public class IntentFilter {
    public static Class Class = RefClass.load(IntentFilter.class, android.content.IntentFilter.class);
    public static RefObject<Object> mActions;
}
