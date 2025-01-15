package com.puzzletak.puzzletakprotector;

import android.app.Application;

import com.puzzletak.library.VirtualCheckCallback;
import com.puzzletak.library.PuzzleTakProtectorLib;

/**
 * Project Name:PuzzleTakProtector
 * Package Name:com.puzzletak.puzzletakprotector
 * Created by PuzzleTak on - Wednesday, 15 January.
 */
public class TestApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PuzzleTakProtectorLib.checkXposedExistAndDisableIt();
//        可以在启动时创建localServerSocket
//        PuzzleTakProtectorLib.checkIsRunningInVirtualApk(getPackageName(), new VirtualCheckCallback() {
//            @Override
//            public void findSuspect() {
//                System.exit(0);
//            }
//        });
    }
}
