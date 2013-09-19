package com.example.testapp1.runas;

/**
 * Interface exposed by remote process for invoking methods on behalf of it from UI
 *
 * Note: This interface is used only for internal IPC and doesn't have to be backward-compatible
 */
interface IRemoteInterface {
    void nop();
}
