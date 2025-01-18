
/**
 * Made with ♥ by PuzzleTak
 */
package com.puzzletak.library;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.LocalServerSocket;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Utility class for detecting multi-instance or virtual environment scenarios for Android applications.
 */
public class VirtualApkCheckUtil {

    private static final String TAG = "VirtualApkCheckUtil";
    private static volatile VirtualApkCheckUtil singleInstance;

    private VirtualApkCheckUtil() {
    }

    /**
     * Returns the singleton instance of VirtualApkCheckUtil.
     */
    public static VirtualApkCheckUtil getSingleInstance() {
        if (singleInstance == null) {
            synchronized (VirtualApkCheckUtil.class) {
                if (singleInstance == null) {
                    singleInstance = new VirtualApkCheckUtil();
                }
            }
        }
        return singleInstance;
    }

    // List of known multi-instance application package names
    private final String[] virtualPkgs = {
            "com.bly.dkplat", // Dual App
            "com.by.chaos", // Chaos Engine
            "com.lbe.parallel", // Parallel Space
            "com.excelliance.dualaid", // Dual Aid
            "com.lody.virtual", // VirtualXposed, VirtualApp
            "com.qihoo.magic" // 360 Dual Master
    };

    /**
     * Detects multi-instance applications by examining the private file path.
     * @param context Application context.
     * @param callback Callback invoked when a suspect is found.
     * @return True if a multi-instance environment is detected.
     */
    public boolean checkByPrivateFilePath(Context context, VirtualCheckCallback callback) {
        String path = context.getFilesDir().getPath();
        for (String virtualPkg : virtualPkgs) {
            if (path.contains(virtualPkg)) {
                if (callback != null) callback.findSuspect();
                return true;
            }
        }
        return false;
    }

    /**
     * Checks for multi-instance environments by detecting duplicate package names.
     * @param context Application context.
     * @param callback Callback invoked when a suspect is found.
     * @return True if duplicates are detected.
     */
    public boolean checkByOriginApkPackageName(Context context, VirtualCheckCallback callback) {
        try {
            if (context == null)
                throw new IllegalArgumentException("Context must not be null.");

            int count = 0;
            String packageName = context.getPackageName();
            PackageManager pm = context.getPackageManager();
            List<PackageInfo> pkgs = pm.getInstalledPackages(0);
            for (PackageInfo info : pkgs) {
                if (packageName.equals(info.packageName)) {
                    count++;
                }
            }
            if (count > 1 && callback != null) callback.findSuspect();
            return count > 1;
        } catch (Exception ignore) {
        }
        return false;
    }

    /**
     * Checks if loaded shared libraries contain known multi-instance package names.
     * @param callback Callback invoked when a suspect is found.
     * @return True if multi-instance libraries are detected.
     */
    public boolean checkByMultiApkPackageName(VirtualCheckCallback callback) {
        BufferedReader bufr = null;
        try {
            bufr = new BufferedReader(new FileReader("/proc/self/maps"));
            String line;
            while ((line = bufr.readLine()) != null) {
                for (String pkg : virtualPkgs) {
                    if (line.contains(pkg)) {
                        if (callback != null) callback.findSuspect();
                        return true;
                    }
                }
            }
        } catch (Exception ignore) {
        } finally {
            if (bufr != null) {
                try {
                    bufr.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing BufferedReader", e);
                }
            }
        }
        return false;
    }

    /**
     * Checks for multi-instance environments by verifying if multiple processes share the same UID.
     * @param callback Callback invoked when a suspect is found.
     * @return True if duplicate UIDs are detected.
     */
    public boolean checkByHasSameUid(VirtualCheckCallback callback) {
        String filter = getUidStrFormat();
        if (TextUtils.isEmpty(filter)) return false;

        String result = CommandUtil.getSingleInstance().exec("ps");
        if (TextUtils.isEmpty(result)) return false;

        String[] lines = result.split("\n");
        if (lines == null || lines.length == 0) return false;

        int exitDirCount = 0;

        for (String line : lines) {
            if (line.contains(filter)) {
                int pkgStartIndex = line.lastIndexOf(" ");
                String processName = line.substring(pkgStartIndex <= 0 ? 0 : pkgStartIndex + 1);
                File dataFile = new File(String.format("/data/data/%s", processName));
                if (dataFile.exists()) {
                    exitDirCount++;
                }
            }
        }
        if (exitDirCount > 1 && callback != null) callback.findSuspect();
        return exitDirCount > 1;
    }


    private String getUidStrFormat() {
        String filter = CommandUtil.getSingleInstance().exec("cat /proc/self/cgroup");
        if (filter == null || filter.length() == 0) {
            return null;
        }

        int uidStartIndex = filter.lastIndexOf("uid");
        int uidEndIndex = filter.lastIndexOf("/pid");
        if (uidStartIndex < 0) {
            return null;
        }
        if (uidEndIndex <= 0) {
            uidEndIndex = filter.length();
        }

        filter = filter.substring(uidStartIndex + 4, uidEndIndex);
        try {
            String strUid = filter.replaceAll("\n", "");
            if (isNumber(strUid)) {
                int uid = Integer.valueOf(strUid);
                filter = String.format("u0_a%d", uid - 10000);
                return filter;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isNumber(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Port monitoring: First scan the open ports and connect to them.
     * If communication is possible and the communication information is consistent,
     * it is considered that there is already a duplicate instance of the app running (multi-instance detected).
     * Otherwise, start monitoring.
     * This method is not as simple as the checkByCreateLocalServerSocket method and is not recommended.
     *
     * @param secret
     * @param callback
     */
    @Deprecated
    public void checkByPortListening(String secret, VirtualCheckCallback callback) {
        startClient(secret);
        new ServerThread(secret, callback).start();
    }

    // At this point, the app acts as the receiver of the secret, i.e., the server role.
    private class ServerThread extends Thread {
        String secret;
        VirtualCheckCallback callback;

        private ServerThread(String secret, VirtualCheckCallback callback) {
            this.secret = secret;
            this.callback = callback;
        }

        @Override
        public void run() {
            super.run();
            startServer(secret, callback);
        }
    }

    // Find an unused port and start monitoring.
    // If a connection is detected, start the read thread.
    private void startServer(String secret, VirtualCheckCallback callback) {
        Random random = new Random();
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress("127.0.0.1",
                    random.nextInt(55534) + 10000));
            while (true) {
                Socket socket = serverSocket.accept();
                ReadThread readThread = new ReadThread(secret, socket, callback);
                readThread.start();
//                serverSocket.close();
            }
        } catch (BindException e) {
            startServer(secret, callback); // May cause an infinite loop
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Read thread to process stream information. If the stream contains the secret, it is considered as a multi-instance detection.
    private class ReadThread extends Thread {
        private ReadThread(String secret, Socket socket, VirtualCheckCallback callback) {
            InputStream inputStream = null;
            try {
                inputStream = socket.getInputStream();
                byte buffer[] = new byte[1024 * 4];
                int temp = 0;
                while ((temp = inputStream.read(buffer)) != -1) {
                    String result = new String(buffer, 0, temp);
                    if (result.contains(secret) && callback != null)
                        callback.findSuspect();
                }
                inputStream.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Read files to scan open ports, add them to the port list, and try to connect to each port.
    private void startClient(String secret) {
        String tcp6 = CommandUtil.getSingleInstance().exec("cat /proc/net/tcp6");
        if (TextUtils.isEmpty(tcp6)) return;
        String[] lines = tcp6.split("\n");
        ArrayList<Integer> portList = new ArrayList<>();
        for (int i = 0, len = lines.length; i < len; i++) {
            int localHost = lines[i].indexOf("0100007F:"); // 127.0.0.1:
            if (localHost < 0) continue;
            String singlePort = lines[i].substring(localHost + 9, localHost + 13);
            Integer port = Integer.parseInt(singlePort, 16);
            portList.add(port);
        }
        if (portList.isEmpty()) return;
        for (int port : portList) {
            new ClientThread(secret, port).start();
        }
    }

    // At this point, the app acts as the sender of the secret (i.e., the client role), sends the secret, and then ends.
    private class ClientThread extends Thread {
        String secret;
        int port;

        private ClientThread(String secret, int port) {
            this.secret = secret;
            this.port = port;
        }

        @Override
        public void run() {
            super.run();
            try {
                Socket socket = new Socket("127.0.0.1", port);
                socket.setSoTimeout(2000);
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write((secret + "\n").getBytes("utf-8"));
                outputStream.flush();
                socket.shutdownOutput();

                InputStream inputStream = socket.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String info = null;
                while ((info = bufferedReader.readLine()) != null) {
                    Log.i(TAG, "ClientThread: " + info);
                }

                bufferedReader.close();
                inputStream.close();
                socket.close();
            } catch (ConnectException e) {
                Log.i(TAG, port + " port refused");
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * As discussed in issue #25:
     * https://github.com/PuzzleTakX/PuzzleTakProtector/
     * Thanks to https://github.com/wangkunlin for providing this method.
     *
     * @param uniqueMsg
     * @param callback
     * @return
     */
    private volatile LocalServerSocket localServerSocket;

    /**
     * @param uniqueMsg Do not use fixed values, as it may cause false positives when multiple package variants or processes are used.
     *                  For single-process use, it is recommended to use context.getPackageName().
     *                  For multi-process scenarios, it is recommended to use the process name {@link SecurityCheckUtil#getCurrentProcessName()}.
     * @param callback
     * @return
     */
    public boolean checkByCreateLocalServerSocket(String uniqueMsg, VirtualCheckCallback callback) {
        if (localServerSocket != null) return false;
        try {
            localServerSocket = new LocalServerSocket(uniqueMsg);
            return false;
        } catch (IOException e) {
            if (callback != null) callback.findSuspect();
            return true;
        }
    }
    /**
     * Idea for checking the top task using TopActivity.
     * Source: https://github.com/109021017/android-TopActivity
     * TopActivity is treated as a separate process (acting as an observer).
     *
     * It can correctly identify the correct package name and class name of apps created using cloning/multi-account apps.
     * This is why it is possible to recognize that the package name of apps cloned using such apps is randomized.
     * Here, I am only providing the method for calling; it might be removed at any time.
     */
    public String checkByTopTask(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> rtis = am.getRunningTasks(1);
        return rtis.get(0).topActivity.getPackageName();
    }

    public String checkByTopActivity(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> rtis = am.getRunningTasks(1);
        return rtis.get(0).topActivity.getClassName();
    }

}
