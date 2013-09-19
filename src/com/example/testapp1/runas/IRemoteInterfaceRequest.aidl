package com.example.testapp1.runas;

/**
 * Interface used for returning {@link RemoteInterfaceImpl} to UI application process
 *
 * Note: This interface is used only for internal IPC and doesn't have to be backward-compatible
 */
import com.example.testapp1.runas.IRemoteInterface;

interface IRemoteInterfaceRequest {
    boolean setRemoteInterface(IRemoteInterface remoteInterface);
}
