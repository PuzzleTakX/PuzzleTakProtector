package com.puzzletak.library;

/**
 * Project Name: PuzzleTakProtector
 * Package Name:com.puzzletak.library
 * Created by PuzzleTak on - Wednesday, 15 January.
 */
public interface LibLoader {
    void loadLibrary(String libName) throws UnsatisfiedLinkError, SecurityException;
}
