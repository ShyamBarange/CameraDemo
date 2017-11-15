package com.sysfore.camerademo;

/**
 * Created by Xplorazzi on 03-Aug-17.
 */

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class Constants {
    public static String getRootDirPath(Context context) {
        File fl = new File(Environment.getExternalStorageDirectory() + "/" + context.getString(R.string.app_name));
        if (!fl.exists()) {
            fl.mkdirs();
        }
        return fl.getPath();
    }
}
