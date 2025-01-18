package com.puzzletak.library;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class SecurityCheckUtil {

    private static class SingletonHolder {
        private static final SecurityCheckUtil singleInstance = new SecurityCheckUtil();
    }

    private SecurityCheckUtil() {
    }

    public static final SecurityCheckUtil getSingleInstance() {
        return SingletonHolder.singleInstance;
    }

    public String getSignature(Context context) {
        try {
            PackageInfo packageInfo = context.
                    getPackageManager()
                    .getPackageInfo(context.getPackageName(),
                            PackageManager.GET_SIGNATURES);
            // Obtain the signature array from the returned package info.
            Signature[] signatures = packageInfo.signatures;
            // Iterate through the signature array and concatenate the signatures.
            StringBuilder builder = new StringBuilder();
            for (Signature signature : signatures) {
                builder.append(signature.toCharsString());
            }
            // Return the application signature.
            return builder.toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    public boolean checkIsDebugVersion(Context context) {
        return (context.getApplicationInfo().flags
                & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    public boolean checkIsDebuggerConnected() {
        return android.os.Debug.isDebuggerConnected();
    }

    public boolean checkIsUsbCharging(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);
        if (batteryStatus == null) return false;
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
    }

    public String getApplicationMetaValue(Context context, String name) {
        ApplicationInfo appInfo = context.getApplicationInfo();
        return appInfo.metaData.getString(name);
    }

    public boolean isLocalPortUsing(int port) {
        boolean flag = true;
        try {
            flag = isPortUsing("127.0.0.1", port);
        } catch (Exception e) {
        }
        return flag;
    }

    /**
     * Check if any port is in use.
     *
     * @param host
     * @param port
     * @return
     * @throws UnknownHostException
     */
    public boolean isPortUsing(String host, int port) throws UnknownHostException {
        boolean flag = false;
        InetAddress theAddress = InetAddress.getByName(host);
        try {
            Socket socket = new Socket(theAddress, port);
            flag = true;
        } catch (IOException e) {
        }
        return flag;
    }

    /**
     * Check root permissions.
     *
     * @return true if the device is rooted.
     */
    public boolean isRoot() {
        int secureProp = getroSecureProp();
        if (secureProp == 0) // eng/userdebug version with root permissions
            return true;
        else return isSUExist(); // user version, check for SU file
    }

    private int getroSecureProp() {
        int secureProp;
        String roSecureObj = CommandUtil.getSingleInstance().getProperty("ro.secure");
        if (roSecureObj == null) secureProp = 1;
        else {
            if ("0".equals(roSecureObj)) secureProp = 0;
            else secureProp = 1;
        }
        return secureProp;
    }

    private boolean isSUExist() {
        File file = null;
        String[] paths = {"/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"};
        for (String path : paths) {
            file = new File(path);
            if (file.exists()) return true;
        }
        return false;
    }

    private static final String XPOSED_HELPERS = "de.robv.android.xposed.XposedHelpers";
    private static final String XPOSED_BRIDGE = "de.robv.android.xposed.XposedBridge";

    /**
     * Check if the Xposed framework is loaded by verifying if its classes are loaded.
     *
     * @return true if Xposed is detected.
     */
    @Deprecated
    public boolean isXposedExists() {
        try {
            Object xpHelperObj = ClassLoader
                    .getSystemClassLoader()
                    .loadClass(XPOSED_HELPERS)
                    .newInstance();
        } catch (Exception e) {
            return true;
        }
        try {
            Object xpBridgeObj = ClassLoader
                    .getSystemClassLoader()
                    .loadClass(XPOSED_BRIDGE)
                    .newInstance();
        } catch (Exception e) {
            return true;
        }
        return true;
    }

    /**
     * Detect the Xposed framework by throwing an exception and checking the stack trace.
     *
     * @return true if Xposed is detected.
     */
    public boolean isXposedExistByThrow() {
        try {
            throw new Exception("gg");
        } catch (Exception e) {
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                if (stackTraceElement.getClassName().contains(XPOSED_BRIDGE)) return true;
            }
            return false;
        }
    }

    /**
     * Attempt to disable the Xposed framework.
     * First, check if Xposed exists using isXposedExistByThrow.
     * If present, hook the global variable disableHooks in the Xposed framework.
     *
     * @return true if successfully disabled.
     */
    public boolean tryShutdownXposed() {
        Field xpdisabledHooks = null;
        try {
            xpdisabledHooks = ClassLoader.getSystemClassLoader()
                    .loadClass(XPOSED_BRIDGE)
                    .getDeclaredField("disableHooks");
            xpdisabledHooks.setAccessible(true);
            xpdisabledHooks.set(null, Boolean.TRUE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Detect if any shared library (.so) is loaded by reading the /proc/<pid>/maps file.
     *
     * @param paramString the library name to check.
     * @return true if the library is found.
     */
    public boolean hasReadProcMaps(String paramString) {
        try {
            Set<String> libraries = new HashSet<>();
            BufferedReader reader =
                    new BufferedReader(new FileReader("/proc/" + Process.myPid() + "/maps"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.endsWith(".so") || line.endsWith(".jar")) {
                    libraries.add(line.substring(line.lastIndexOf(" ") + 1));
                }
            }
            reader.close();
            for (String lib : libraries) {
                if (lib.contains(paramString)) return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Detect debugging by checking the TracerPid value in the /proc/<pid>/status file.
     *
     * @return true if being debugged.
     */
    public boolean readProcStatus() {
        try {
            BufferedReader reader =
                    new BufferedReader(new FileReader("/proc/" + Process.myPid() + "/status"));
            String tracerPid = "";
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("TracerPid")) {
                    tracerPid = line.substring(line.indexOf(":") + 1).trim();
                    break;
                }
            }
            reader.close();
            return !"0".equals(tracerPid);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the current process name.
     *
     * @return the name of the current process.
     */
    public String getCurrentProcessName() {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream("/proc/self/cmdline");
            byte[] buffer = new byte[256];
            int len = 0;
            int b;
            while ((b = fis.read()) > 0 && len < buffer.length) {
                buffer[len++] = (byte) b;
            }
            if (len > 0) {
                return new String(buffer, 0, len, "UTF-8");
            }
        } catch (Exception e) {
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                }
            }
        }
        return null;
    }
}
