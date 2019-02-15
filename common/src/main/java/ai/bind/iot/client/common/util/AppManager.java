/*
 * Copyright (c) 2018 CELLA
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.bind.iot.client.common.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by w5e.
 */
public class AppManager {
    private static final boolean DEBUG = true;

    public static boolean isAppInstalled(Context context, String packageName) {
        return getInstalledAppInfo(context, packageName) != null;
    }

    public static PackageInfo getPackageInfo(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo pi = null;
        try {
            pi = packageManager.getPackageInfo(packageName, 0);
        } catch (NameNotFoundException ignore) {
        }
        return pi;
    }

    public static String getVersionName(Context context, String packageName) {
        PackageInfo pi = getPackageInfo(context, packageName);
        if (pi != null) {
            return pi.versionName;
        }
        return null;
    }

    public static long getVersionCode(Context context, String packageName) {
        PackageInfo pi = getPackageInfo(context, packageName);
        if (pi != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return pi.getLongVersionCode();
            }
            return pi.versionCode;
        }
        return -1;
    }

    public static ResolveInfo getInstalledAppInfo(Context context, String packageName) {
        if (context == null || packageName == null) {
            return null;
        }
        PackageManager packageManager = context.getPackageManager();
        PackageInfo pi;
        try {
            pi = packageManager.getPackageInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            return null;
        }
        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resolveIntent.setPackage(pi.packageName);

        List<ResolveInfo> apps = packageManager.queryIntentActivities(
                resolveIntent, 0);
        return apps.iterator().next();
    }

    /**
     * <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
     */
    public static boolean installApp(Context context, String authority, File file) {
        if (context == null || file == null || !file.exists() || !file.canRead()) {
            return false;
        }
        //http://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (authority == null) {
                return false;
            }
            uri = FileProvider.getUriForFile(context, authority, file);
        } else {
            uri = Uri.fromFile(file);
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.getApplicationContext().startActivity(intent);
        return true;
    }

    /**
     * <uses-permission android:name="android.permission.REQUEST_DELETE_PACKAGES"/>
     */
    public static void uninstallApp(Context context, String packageName) {
        if (context == null || packageName == null) {
            return;
        }
        Uri packageUri = Uri.parse("package:" + packageName);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_DELETE);
        intent.setData(packageUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(intent);
    }

    /**启动成功会返回Activity的名字*/
    public static String startApp(Context context, String packageName) {
        if (context == null || packageName == null) {
            return null;
        }
        ResolveInfo ri = getInstalledAppInfo(context, packageName);
        return startApp(context, packageName, ri);
    }

    public static String startApp(Context context, String packageName, ResolveInfo ri) {
        if (context == null || packageName == null || ri == null) {
            return null;
        }
        String activityName = ri.activityInfo.name;

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        ComponentName componentName = new ComponentName(packageName, activityName);
        intent.setComponent(componentName);

        context.startActivity(intent);
        return activityName;
    }

    /**
     * 静默安装
     *
     * @param apkPath APK绝对路径
     * @return 错误信息
     */
    public static String install(String apkPath) {
        //String[] args = {"pm", "install", "-r", apkPath};
        //return runCommand(args);
        return runCommand("LD_LIBRARY_PATH=/vendor/lib*:/system/lib* pm install -r " + apkPath);
    }

    public static String uninstall(String packageName) {
        //String[] args = {"pm", "uninstall", packageName};
        //return runCommand(args);
        return runCommand("LD_LIBRARY_PATH=/vendor/lib*:/system/lib* pm uninstall " + packageName);
    }

    //private static String runCommand(String... command) {
    //    ProcessBuilder processBuilder = new ProcessBuilder(command);
    //    Process process = null;
    //    try {
    //        process = processBuilder.start();
    //        return getCommandResult(process);
    //    } catch (Exception e) {
    //        return e.getLocalizedMessage();
    //    } finally {
    //        if (process != null) {
    //            process.destroy();
    //        }
    //    }
    //}

    public static String runCommand(String command) {
        Process process = null;
        DataOutputStream cmdOutputStream = null;
        try {
            //目前只使用su命令
            process = Runtime.getRuntime().exec("su");
            cmdOutputStream = new DataOutputStream(process.getOutputStream());
            // 执行命令
            cmdOutputStream.write((command).getBytes(Charset.forName("utf-8")));
            cmdOutputStream.writeBytes("\n");
            cmdOutputStream.flush();
            cmdOutputStream.writeBytes("exit\n");
            cmdOutputStream.flush();
            process.waitFor();
            return getCommandResult(process);
        } catch (Exception e) {
            return e.getLocalizedMessage();
        } finally {
            try {
                if (cmdOutputStream != null) {
                    cmdOutputStream.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * @param process 当前进程
     * @return 值为null说明成功，否则返回错误信息
     */
    private static String getCommandResult(@NonNull Process process) {
        String successMsg;
        try (BufferedReader successMsgReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            successMsg = read(successMsgReader);
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
        if (successMsg != null
                && (successMsg.contains("Success") || successMsg.contains("success"))) {
            return null;
        }
        if (DEBUG) {
            try (BufferedReader errorMsgReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                return read(errorMsgReader);
            } catch (Exception ignored) {
            }
        }
        return "UNKNOWN ERROR!";
    }

    private static String read(BufferedReader reader) throws IOException {
        if (reader == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        // 读取命令的执行结果
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        String msg = stringBuilder.toString();
        return TextUtils.isEmpty(msg) ? null : msg;
    }

}
