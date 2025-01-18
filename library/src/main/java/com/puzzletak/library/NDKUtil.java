package com.puzzletak.library;

/**
 * Project Name: PuzzleTakProtector
 * Package Name: com.puzzletak.library
 * Created by PuzzleTak on - Wednesday, 15 January.
 */
public class NDKUtil {
    private static volatile boolean mIsLibLoaded = false;
    private static volatile boolean mIsNativeInited = false;

    // Default library loader
    private static LibLoader localLibLoader = new LibLoader() {
        @Override
        public void loadLibrary(String libName) throws UnsatisfiedLinkError, SecurityException {
            System.loadLibrary(libName);
        }
    };

    /**
     * This method is only used to load the "antitrace.so" library.
     *
     * @param libLoader custom LibLoader, if null the default loader is used.
     */
    public static void loadLibrariesOnce(LibLoader libLoader) {
        synchronized (NDKUtil.class) {
            if (!mIsLibLoaded) {
                if (libLoader == null) {
                    libLoader = localLibLoader;
                }
                libLoader.loadLibrary("antitrace");
                mIsLibLoaded = true;
            }
        }
    }

    /**
     * Use this method to load other native libraries using NDKUtil.
     *
     * @param libName the name of the native library to load.
     */
    public static void loadLibraryByName(String libName) {
        if (libName == null || libName.isEmpty()) return;
        synchronized (NDKUtil.class) {
            localLibLoader.loadLibrary(libName);
        }
    }

    // Constructor to initialize the default library loader
    public NDKUtil() {
        this(localLibLoader);
    }

    // Constructor with a custom library loader
    public NDKUtil(LibLoader libLoader) {
        initNDK(libLoader);
    }

    // Initialize the NDK environment by loading libraries and initializing native code
    private void initNDK(LibLoader libLoader) {
        loadLibrariesOnce(libLoader);
        initNativeOnce();
    }

    // Ensure native initialization happens only once
    private void initNativeOnce() {
        synchronized (NDKUtil.class) {
            if (!mIsNativeInited) {
                // Uncomment the following line to initialize native code
                // native_init();
                mIsNativeInited = true;
            }
        }
    }

    // Uncomment and implement this method to initialize native code
    // private static native void native_init();
}
