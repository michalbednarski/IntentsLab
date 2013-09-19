package android.content;

import android.os.Bundle;

/**
 * This is not real class, this is stub from AOSP to link against
 */
public interface IIntentReceiver {
    public abstract void performReceive(
            Intent intent,
            int resultCode,
            String data,
            Bundle extras,
            boolean ordered,
            boolean sticky,
            int sendingUser
    );

    public abstract void performReceive(
            Intent intent,
            int resultCode,
            String data,
            Bundle extras,
            boolean ordered,
            boolean sticky
    );

    public abstract void performReceive(
            Intent intent,
            int resultCode,
            String data,
            Bundle extras,
            boolean ordered
    );

    public abstract static class Stub implements IIntentReceiver {}
}
