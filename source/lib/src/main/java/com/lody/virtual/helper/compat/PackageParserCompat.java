package com.lody.virtual.helper.compat;

import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;
import android.os.Build;
import android.util.DisplayMetrics;

import com.lody.virtual.client.core.VirtualCore;

import java.io.File;

import mirror.android.content.pm.PackageParserJellyBean;
import mirror.android.content.pm.PackageParserJellyBean17;
import mirror.android.content.pm.PackageParserLollipop;
import mirror.android.content.pm.PackageParserLollipop22;
import mirror.android.content.pm.PackageParserMarshmallow;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;

/**
 * @author Lody
 */

public class PackageParserCompat {

    public static final int[] GIDS = VirtualCore.get().getGids();
    private static final int API_LEVEL = Build.VERSION.SDK_INT;


    public static PackageParser createParser(File packageFile) {
        if (API_LEVEL >= M) {
            return PackageParserMarshmallow.ctor.newInstance();
        } else if (API_LEVEL >= LOLLIPOP_MR1) {
            return PackageParserLollipop22.ctor.newInstance();
        } else if (API_LEVEL >= LOLLIPOP) {
            return PackageParserLollipop.ctor.newInstance();
        } else if (API_LEVEL >= JELLY_BEAN_MR1) {
            return PackageParserJellyBean17.ctor.newInstance(packageFile.getAbsolutePath());
        } else if (API_LEVEL >= JELLY_BEAN) {
            return PackageParserJellyBean.ctor.newInstance(packageFile.getAbsolutePath());
        } else {
            return mirror.android.content.pm.PackageParser.ctor.newInstance(packageFile.getAbsolutePath());
        }
    }

    public static Package parsePackage(PackageParser parser, File packageFile, int flags) throws Throwable {
        if (API_LEVEL >= M) {
            return PackageParserMarshmallow.parsePackage.callWithException(parser, packageFile, flags);
        } else if (API_LEVEL >= LOLLIPOP_MR1) {
            return PackageParserLollipop22.parsePackage.callWithException(parser, packageFile, flags);
        } else if (API_LEVEL >= LOLLIPOP) {
            return PackageParserLollipop.parsePackage.callWithException(parser, packageFile, flags);
        } else if (API_LEVEL >= JELLY_BEAN_MR1) {
            return PackageParserJellyBean17.parsePackage.callWithException(parser, packageFile, null,
                    new DisplayMetrics(), flags);
        } else if (API_LEVEL >= JELLY_BEAN) {
            return PackageParserJellyBean.parsePackage.callWithException(parser, packageFile, null,
                    new DisplayMetrics(), flags);
        } else {
            return mirror.android.content.pm.PackageParser.parsePackage.callWithException(parser, packageFile, null,
                    new DisplayMetrics(), flags);
        }
    }
}
