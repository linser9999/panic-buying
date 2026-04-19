package com.linser.utils.lock;

public interface Lock {

    boolean onLock(long timeOut);

    void unLock();
}
