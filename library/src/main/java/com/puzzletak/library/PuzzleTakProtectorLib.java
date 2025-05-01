package com.puzzletak.library;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Pair;

import java.io.File;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Project Name:PuzzleTakProtector
 * Package Name:com.puzzletak.library
 * Created by PuzzleTak on - Wednesday, 15 January.
 */
public class PuzzleTakProtectorLib {

    public static String checkSignature(Context context) {
        return SecurityCheckUtil.getSingleInstance().getSignature(context);
    }

    public static boolean checkIsDebug(Context context) {
        return SecurityCheckUtil.getSingleInstance().checkIsDebugVersion(context) ||
                SecurityCheckUtil.getSingleInstance().checkIsDebuggerConnected();
    }

    public static boolean checkIsPortUsing(String host, int port) {
        try {
            return SecurityCheckUtil.getSingleInstance().isPortUsing(host, port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return true;
        }
    }

    public static boolean checkIsRoot() {
        return SecurityCheckUtil.getSingleInstance().isRoot();
    }

    public static boolean checkIsXposedExist() {
        return SecurityCheckUtil.getSingleInstance().isXposedExistByThrow();
    }

    public static boolean checkXposedExistAndDisableIt() {
        return SecurityCheckUtil.getSingleInstance().tryShutdownXposed();
    }

    public static boolean checkHasLoadSO(String soName) {
        return SecurityCheckUtil.getSingleInstance().hasReadProcMaps(soName);
    }

    public static boolean checkIsBeingTracedByJava() {
        return SecurityCheckUtil.getSingleInstance().readProcStatus();
    }

    public static void checkIsBeingTracedByC() {
        NDKUtil.loadLibrariesOnce(null);
//        NDKUtil.loadLibraryByName("antitrace");
    }

    public static boolean checkFilesExist(String[] files) {
        for (String filePath : files) {
            File file = new File(filePath);
            if (file.exists()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlueStacks() {
        String[] BLUE_STACKS_FILES = {
                "/mnt/windows/BstSharedFolder"
        };
        return checkFilesExist(BLUE_STACKS_FILES);
    }

    public static boolean checkTelephonyManager(Context context){
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String networkOperator = tm.getNetworkOperatorName();
        if ("Android".equals(networkOperator)) {
          return true;
        } else {
            return  false;
        }
    }

    public static boolean checkIsRunningInEmulator(Context context, EmulatorSuperCheckCallback callback) {
        return EmulatorSuperCheckUtil.getSingleInstance().readSysProperty(context, callback);
    }
    public static int checkIsRunningInEmulatorPT(Context context, EmulatorSuperCheckCallback callback) {
        return EmulatorSuperCheckUtil.getSingleInstance().readSysPropertyPT(context, callback);
    }
    public static void readSysPropertyPTDetailed(Context context, EmulatorDetailsCallback callback) {
        EmulatorSuperCheckUtil.getSingleInstance().readSysPropertyPTDetailed(context, callback);
    }
    public static boolean checkIsRunningInEmulatorPTResult(Context context, EmulatorSuperCheckCallback callback) {
        return EmulatorSuperCheckUtil.getSingleInstance().readSysPropertyPTResult(context, callback);
    }

    public static boolean checkIsRunningInVirtualApk(String uniqueMsg, VirtualCheckCallback callback) {
        return VirtualApkCheckUtil.getSingleInstance().checkByCreateLocalServerSocket(uniqueMsg, callback);
    }
}
