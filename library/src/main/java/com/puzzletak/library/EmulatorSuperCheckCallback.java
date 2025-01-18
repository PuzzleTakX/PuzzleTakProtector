package com.puzzletak.library;


import java.util.Map;

public interface EmulatorSuperCheckCallback {
    void findEmulator(String emulatorInfo);
    void checkEmulator(int emulatorInfo);
    void detailsEmulator(Map<String,Object> emulatorInfo);
}
