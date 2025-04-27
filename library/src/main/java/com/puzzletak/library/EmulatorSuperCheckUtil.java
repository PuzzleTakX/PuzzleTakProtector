package com.puzzletak.library;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.text.TextUtils;

import static android.content.Context.SENSOR_SERVICE;
import static com.puzzletak.library.CheckResult.RESULT_EMULATOR;
import static com.puzzletak.library.CheckResult.RESULT_MAYBE_EMULATOR;
import static com.puzzletak.library.CheckResult.RESULT_UNKNOWN;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for detecting if the app is running on an emulator.
 * Made with ❤ by puzzletak
 */
public class EmulatorSuperCheckUtil {

    // Private constructor to enforce Singleton pattern.
    private EmulatorSuperCheckUtil() {
    }

    // Inner static class for holding the Singleton instance.
    private static class SingletonHolder {
        private static final EmulatorSuperCheckUtil INSTANCE = new EmulatorSuperCheckUtil();
    }

    // Public method to access the Singleton instance.
    public static final EmulatorSuperCheckUtil getSingleInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Main method to detect emulator based on various parameters.
     *
     * @param context  Application context
     * @param callback Callback to report findings
     * @return true if emulator detected, false otherwise
     */
    public boolean readSysProperty(Context context, EmulatorSuperCheckCallback callback) {
        if (context == null)
            throw new IllegalArgumentException("context must not be null");

        int suspectCount = 0;

        // Check hardware name
        CheckResult hardwareResult = checkFeaturesByHardware();
        switch (hardwareResult.result) {
            case RESULT_MAYBE_EMULATOR:
                ++suspectCount;
                break;
            case RESULT_EMULATOR:
                if (callback != null) callback.findEmulator("hardware -= " + hardwareResult.value);
                return true;
        }

        // Check host name
        CheckResult hostResult = checkFeaturesByHost();
        switch (hostResult.result) {
            case RESULT_MAYBE_EMULATOR:
                ++suspectCount;
                break;
            case RESULT_EMULATOR:
                if (callback != null) callback.findEmulator("host = " + hostResult.value);
                return true;
        }

        // Check build flavor
        CheckResult flavorResult = checkFeaturesByFlavor();
        switch (flavorResult.result) {
            case RESULT_MAYBE_EMULATOR:
                ++suspectCount;
                break;
            case RESULT_EMULATOR:
                if (callback != null) callback.findEmulator("flavor = " + flavorResult.value);
                return true;
        }

        // Check device model
        CheckResult modelResult = checkFeaturesByModel();
        switch (modelResult.result) {
            case RESULT_MAYBE_EMULATOR:
                ++suspectCount;
                break;
            case RESULT_EMULATOR:
                if (callback != null) callback.findEmulator("model = " + modelResult.value);
                return true;
        }

        // Check manufacturer
        CheckResult manufacturerResult = checkFeaturesByManufacturer();
        switch (manufacturerResult.result) {
            case RESULT_MAYBE_EMULATOR:
                ++suspectCount;
                break;
            case RESULT_EMULATOR:
                if (callback != null)
                    callback.findEmulator("manufacturer = " + manufacturerResult.value);
                return true;
        }

        // Check board name
        CheckResult boardResult = checkFeaturesByBoard();
        switch (boardResult.result) {
            case RESULT_MAYBE_EMULATOR:
                ++suspectCount;
                break;
            case RESULT_EMULATOR:
                if (callback != null) callback.findEmulator("board = " + boardResult.value);
                return true;
        }

        // Check platform name
        CheckResult platformResult = checkFeaturesByPlatform();
        switch (platformResult.result) {
            case RESULT_MAYBE_EMULATOR:
                ++suspectCount;
                break;
            case RESULT_EMULATOR:
                if (callback != null) callback.findEmulator("platform = " + platformResult.value);
                return true;
        }

        // Check baseband information
        CheckResult baseBandResult = checkFeaturesByBaseBand();
        switch (baseBandResult.result) {
            case RESULT_MAYBE_EMULATOR:
                suspectCount += 2; // Baseband info being null strongly indicates an emulator.
                break;
            case RESULT_EMULATOR:
                if (callback != null) callback.findEmulator("baseBand = " + baseBandResult.value);
                return true;
        }

        // Check sensor count
        int sensorNumber = getSensorNumber(context);
        if (sensorNumber <= 7) ++suspectCount;

        // Check number of installed third-party apps
        int userAppNumber = getUserAppNumber();
        if (userAppNumber <= 5) ++suspectCount;

        // Check if camera flash is supported
//        boolean supportCameraFlash = supportCameraFlash(context);
//        if (!supportCameraFlash) ++suspectCount;

        // Check if camera is supported
        boolean supportCamera = supportCamera(context);
        if (!supportCamera) ++suspectCount;

        // Check if Bluetooth is supported
        boolean supportBluetooth = supportBluetooth(context);
        if (!supportBluetooth) ++suspectCount;

        // Check light sensor presence
//        boolean hasLightSensor = hasLightSensor(context);
//        if (!hasLightSensor) ++suspectCount;

        // Check cgroup information
        CheckResult cgroupResult = checkFeaturesByCgroup();
        if (cgroupResult.result == RESULT_MAYBE_EMULATOR) ++suspectCount;

        // Provide callback with detailed results
        if (callback != null) {
            StringBuffer stringBuffer = new StringBuffer("Test start")
                    .append("\r\n").append("hardware = ").append(hardwareResult.value)
                    .append("\r\n").append("host = ").append(hostResult.value)
                    .append("\r\n").append("flavor = ").append(flavorResult.value)
                    .append("\r\n").append("model = ").append(modelResult.value)
                    .append("\r\n").append("manufacturer = ").append(manufacturerResult.value)
                    .append("\r\n").append("board = ").append(boardResult.value)
                    .append("\r\n").append("platform = ").append(platformResult.value)
                    .append("\r\n").append("baseBand = ").append(baseBandResult.value)
                    .append("\r\n").append("sensorNumber = ").append(sensorNumber)
                    .append("\r\n").append("userAppNumber = ").append(userAppNumber)
                    .append("\r\n").append("supportCamera = ").append(supportCamera)
//                    .append("\r\n").append("supportCameraFlash = ").append(supportCameraFlash)
                    .append("\r\n").append("supportBluetooth = ").append(supportBluetooth)
//                    .append("\r\n").append("hasLightSensor = ").append(hasLightSensor)
                    .append("\r\n").append("cgroupResult = ").append(cgroupResult.value)
                    .append("\r\n").append("suspectCount = ").append(suspectCount);
            callback.findEmulator(stringBuffer.toString());
            callback.checkEmulator(suspectCount);
        }

        // If suspicion count is greater than 3, consider it an emulator
        return suspectCount > 3;
    }


    public int readSysPropertyPT(Context context, EmulatorSuperCheckCallback callback) {
        if (context == null)
            throw new IllegalArgumentException("context must not be null");

        int suspectCount = 0;

        // Check hardware name
        CheckResult hardwareResult = checkFeaturesByHardware();
        if (hardwareResult.result == RESULT_MAYBE_EMULATOR) {
            ++suspectCount;
        }

        // Check host name
        CheckResult hostResult = checkFeaturesByHost();
        if (hostResult.result == RESULT_MAYBE_EMULATOR) {
            ++suspectCount;
        }

        // Check build flavor
        CheckResult flavorResult = checkFeaturesByFlavor();
        if (flavorResult.result == RESULT_MAYBE_EMULATOR) {
            ++suspectCount;
        }

        // Check device model
        CheckResult modelResult = checkFeaturesByModel();
        if (modelResult.result == RESULT_MAYBE_EMULATOR) {
            ++suspectCount;
        }

        // Check manufacturer
        CheckResult manufacturerResult = checkFeaturesByManufacturer();
        if (manufacturerResult.result == RESULT_MAYBE_EMULATOR) {
            ++suspectCount;
        }

        // Check board name
        CheckResult boardResult = checkFeaturesByBoard();
        if (boardResult.result == RESULT_MAYBE_EMULATOR) {
            ++suspectCount;
        }

        // Check platform name
        CheckResult platformResult = checkFeaturesByPlatform();
        if (platformResult.result == RESULT_MAYBE_EMULATOR) {
            ++suspectCount;
        }

        // Check baseband information
        CheckResult baseBandResult = checkFeaturesByBaseBand();
        if (baseBandResult.result == RESULT_MAYBE_EMULATOR) {
            suspectCount += 2; // Baseband info being null strongly indicates an emulator.
        }

        // Check sensor count
        int sensorNumber = getSensorNumber(context);
        if (sensorNumber <= 7) ++suspectCount;

        // Check number of installed third-party apps
        int userAppNumber = getUserAppNumber();
        if (userAppNumber <= 5) ++suspectCount;

        // Check if camera flash is supported
//        boolean supportCameraFlash = supportCameraFlash(context);
//        if (!supportCameraFlash) ++suspectCount;

        // Check if camera is supported
        boolean supportCamera = supportCamera(context);
        if (!supportCamera) ++suspectCount;

        // Check if Bluetooth is supported
        boolean supportBluetooth = supportBluetooth(context);
        if (!supportBluetooth) ++suspectCount;

        // Check light sensor presence
//        boolean hasLightSensor = hasLightSensor(context);
//        if (!hasLightSensor) ++suspectCount;

        // Check cgroup information
        CheckResult cgroupResult = checkFeaturesByCgroup();
        if (cgroupResult.result == RESULT_MAYBE_EMULATOR) ++suspectCount;

        // Provide callback with detailed results
        if (callback != null) {
            callback.checkEmulator(suspectCount);
        }

        // If suspicion count is greater than 3, consider it an emulator
        return suspectCount;
    }
    public boolean readSysPropertyPTResult(Context context, EmulatorSuperCheckCallback callback) {
        if (context == null)
            throw new IllegalArgumentException("context must not be null");

        int suspectCount = 0;

        // Check hardware name
        CheckResult hardwareResult = checkFeaturesByHardware();
        if (hardwareResult.result == RESULT_MAYBE_EMULATOR) {
            ++suspectCount;
        }

        // Check host name
        CheckResult hostResult = checkFeaturesByHost();
        if (hostResult.result == RESULT_MAYBE_EMULATOR) {
            ++suspectCount;
        }

        // Check build flavor
        CheckResult flavorResult = checkFeaturesByFlavor();
        if (flavorResult.result == RESULT_MAYBE_EMULATOR) {
            ++suspectCount;
        }

        // Check device model
        CheckResult modelResult = checkFeaturesByModel();
        if (modelResult.result == RESULT_MAYBE_EMULATOR) {
            ++suspectCount;
        }

        // Check manufacturer
        CheckResult manufacturerResult = checkFeaturesByManufacturer();
        if (manufacturerResult.result == RESULT_MAYBE_EMULATOR) {
            ++suspectCount;
        }

        // Check board name
        CheckResult boardResult = checkFeaturesByBoard();
        if (boardResult.result == RESULT_MAYBE_EMULATOR) {
            ++suspectCount;
        }

        // Check platform name
        CheckResult platformResult = checkFeaturesByPlatform();
        if (platformResult.result == RESULT_MAYBE_EMULATOR) {
            ++suspectCount;
        }

        // Check baseband information
        CheckResult baseBandResult = checkFeaturesByBaseBand();
        if (baseBandResult.result == RESULT_MAYBE_EMULATOR) {
            suspectCount += 2; // Baseband info being null strongly indicates an emulator.
        }

        // Check sensor count
        int sensorNumber = getSensorNumber(context);
        if (sensorNumber <= 7) ++suspectCount;

        // Check number of installed third-party apps
        int userAppNumber = getUserAppNumber();
        if (userAppNumber <= 5) ++suspectCount;

        // Check if camera flash is supported
//        boolean supportCameraFlash = supportCameraFlash(context);
//        if (!supportCameraFlash) ++suspectCount;

        // Check if camera is supported
        boolean supportCamera = supportCamera(context);
        if (!supportCamera) ++suspectCount;

        // Check if Bluetooth is supported
        boolean supportBluetooth = supportBluetooth(context);
        if (!supportBluetooth) ++suspectCount;

        // Check light sensor presence
//        boolean hasLightSensor = hasLightSensor(context);
//        if (!hasLightSensor) ++suspectCount;

        // Check cgroup information
        CheckResult cgroupResult = checkFeaturesByCgroup();
        if (cgroupResult.result == RESULT_MAYBE_EMULATOR) ++suspectCount;

        // Provide callback with detailed results
        if (callback != null) {
            Map<String, Object> emulatorInfo = new HashMap<>();
            emulatorInfo.put("hardware", hardwareResult.value);
            emulatorInfo.put("host", hostResult.value);
            emulatorInfo.put("flavor", flavorResult.value);
            emulatorInfo.put("model", modelResult.value);
            emulatorInfo.put("manufacturer", manufacturerResult.value);
            emulatorInfo.put("board", boardResult.value);
            emulatorInfo.put("platform", platformResult.value);
            emulatorInfo.put("baseBand", baseBandResult.value);
            emulatorInfo.put("sensorNumber", sensorNumber);
            emulatorInfo.put("userAppNumber", userAppNumber);
            emulatorInfo.put("supportCamera", supportCamera);
//            emulatorInfo.put("supportCameraFlash", supportCameraFlash);
            emulatorInfo.put("supportBluetooth", supportBluetooth);
//            emulatorInfo.put("hasLightSensor", hasLightSensor);
            emulatorInfo.put("cgroupResult", cgroupResult.value);
            emulatorInfo.put("suspectCount", suspectCount);

// Call the callback with the map
            callback.detailsEmulator(emulatorInfo);

        }

        // If suspicion count is greater than 3, consider it an emulator
        return suspectCount > 3;
    }

    // Additional helper methods follow, providing specific checks (e.g., hardware, flavor, sensors, etc.).
    // Each method is documented inline.

    // Method implementations like getUserAppNumber, supportCamera, hasLightSensor, etc., are already self-explanatory.

    private int getUserAppNum(String userApps) {
        if (TextUtils.isEmpty(userApps)) return 0;

        String[] result = userApps.split("package:");

        return result.length;
    }

    private String getProperty(String propName) {
        String property = CommandUtil.getSingleInstance().getProperty(propName);
        return TextUtils.isEmpty(property) ? null : property;
    }

    private CheckResult checkFeaturesByHost() {
        String hardware = getProperty("ro.build.host");
        if (TextUtils.isEmpty(hardware)) {
            return new CheckResult(RESULT_UNKNOWN, null);
        }

        int result = RESULT_UNKNOWN;
        assert hardware != null;
        String tempValue = hardware.toLowerCase();

        switch (tempValue) {
            case "dev":
            case "Build2":
            case "buildbot":
            case "google_sdk":
            case "android-build":
                result = RESULT_EMULATOR;
                break;
        }

        return new CheckResult(result, tempValue);
    }

    private CheckResult checkFeaturesByHardware() {
        String hardware = getProperty("ro.hardware");
        if (null == hardware) return new CheckResult(RESULT_MAYBE_EMULATOR, null);

        int result;
        String tempValue = hardware.toLowerCase();

        switch (tempValue) {
            case "ttvm": // TTVM Emulator
            case "nox": // Nox Emulator
            case "cancro": // NetEase MUMU Emulator
            case "intel": // Microvirt Emulator
            case "vbox":
            case "vbox86": // Tencent Emulator
            case "android_x86": // LDPlayer Emulator
                result = RESULT_EMULATOR;
                break;
            default:
                result = RESULT_UNKNOWN;
                break;
        }

        return new CheckResult(result, hardware);
    }

    /**
     * Feature parameter - channel
     *
     * @return 0 indicates it may be an emulator, 1 indicates an emulator, 2 indicates it may be a real device
     */
    private CheckResult checkFeaturesByFlavor() {
        String flavor = getProperty("ro.build.flavor");
        if (null == flavor) return new CheckResult(RESULT_MAYBE_EMULATOR, null);

        int result;
        String tempValue = flavor.toLowerCase();

        if (tempValue.contains("vbox")) result = RESULT_EMULATOR;
        else if (tempValue.contains("sdk_gphone")) result = RESULT_EMULATOR;
        else result = RESULT_UNKNOWN;

        return new CheckResult(result, flavor);
    }

    /**
     * Feature parameter - device model
     *
     * @return 0 indicates it may be an emulator, 1 indicates an emulator, 2 indicates it may be a real device
     */
    private CheckResult checkFeaturesByModel() {
        String model = getProperty("ro.product.model");
        if (null == model) return new CheckResult(RESULT_MAYBE_EMULATOR, null);

        int result;
        String tempValue = model.toLowerCase();

        if (tempValue.contains("google_sdk")) result = RESULT_EMULATOR;
        else if (tempValue.contains("emulator")) result = RESULT_EMULATOR;
        else if (tempValue.contains("android sdk built for x86")) result = RESULT_EMULATOR;
        else result = RESULT_UNKNOWN;

        return new CheckResult(result, model);
    }

    /**
     * Feature parameter - manufacturer
     *
     * @return 0 indicates it may be an emulator, 1 indicates an emulator, 2 indicates it may be a real device
     */
    private CheckResult checkFeaturesByManufacturer() {
        String manufacturer = getProperty("ro.product.manufacturer");
        if (null == manufacturer) return new CheckResult(RESULT_MAYBE_EMULATOR, null);

        int result;
        String tempValue = manufacturer.toLowerCase();

        if (tempValue.contains("genymotion")) result = RESULT_EMULATOR;
        else if (tempValue.contains("netease")) result = RESULT_EMULATOR; // NetEase MUMU Emulator
        else result = RESULT_UNKNOWN;

        return new CheckResult(result, manufacturer);
    }

    /**
     * Feature parameter - board name
     *
     * @return 0 indicates it may be an emulator, 1 indicates an emulator, 2 indicates it may be a real device
     */
    private CheckResult checkFeaturesByBoard() {
        String board = getProperty("ro.product.board");
        if (null == board) return new CheckResult(RESULT_MAYBE_EMULATOR, null);

        int result;
        String tempValue = board.toLowerCase();

        if (tempValue.contains("android")) result = RESULT_EMULATOR;
        else if (tempValue.contains("goldfish")) result = RESULT_EMULATOR;
        else result = RESULT_UNKNOWN;

        return new CheckResult(result, board);
    }

    /**
     * Feature parameter - platform
     *
     * @return 0 indicates it may be an emulator, 1 indicates an emulator, 2 indicates it may be a real device
     */
    private CheckResult checkFeaturesByPlatform() {
        String platform = getProperty("ro.board.platform");
        if (null == platform) return new CheckResult(RESULT_MAYBE_EMULATOR, null);

        int result;
        String tempValue = platform.toLowerCase();

        if (tempValue.contains("android")) result = RESULT_EMULATOR;
        else result = RESULT_UNKNOWN;

        return new CheckResult(result, platform);
    }

    /**
     * Feature parameter - baseband information
     *
     * @return 0 indicates it may be an emulator, 1 indicates an emulator, 2 indicates it may be a real device
     */
    private CheckResult checkFeaturesByBaseBand() {
        String baseBandVersion = getProperty("gsm.version.baseband");
        if (null == baseBandVersion) return new CheckResult(RESULT_MAYBE_EMULATOR, null);

        int result;
        if (baseBandVersion.contains("1.0.0.0")) result = RESULT_EMULATOR;
        else result = RESULT_UNKNOWN;

        return new CheckResult(result, baseBandVersion);
    }

    /**
     * Get the number of sensors
     */
    private int getSensorNumber(Context context) {
        SensorManager sm = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        return sm.getSensorList(Sensor.TYPE_ALL).size();
    }

    /**
     * Get the number of installed third-party applications
     */
    private int getUserAppNumber() {
        String userApps = CommandUtil.getSingleInstance().exec("pm list package -3");
        return getUserAppNum(userApps);
    }

    /**
     * Whether the camera is supported
     */
    private boolean supportCamera(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        }
        return false;
    }

    /**
     * Whether the flashlight is supported
     */
    private boolean supportCameraFlash(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * Whether Bluetooth is supported
     */
    private boolean supportBluetooth(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    /**
     * Determine whether there is a light sensor to determine whether it is an emulator
     * Some real devices also lack temperature and pressure sensors. Other sensors may also exist in emulators.
     *
     * @return false indicates it is an emulator
     */
    private boolean hasLightSensor(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT); // Light sensor
        if (null == sensor) return false;
        else return true;
    }

    /**
     * Feature parameter - cgroup information
     */
    private CheckResult checkFeaturesByCgroup() {
        String filter = CommandUtil.getSingleInstance().exec("cat /proc/self/cgroup");
        if (null == filter) return new CheckResult(RESULT_MAYBE_EMULATOR, null);
        return new CheckResult(RESULT_UNKNOWN, filter);
    }
}

// Made with ❤ by puzzletak
