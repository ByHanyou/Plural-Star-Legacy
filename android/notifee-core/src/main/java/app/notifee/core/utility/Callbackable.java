package app.notifee.core.utility;

import androidx.annotation.Nullable;

public interface Callbackable<T> {

  void call(@Nullable Exception e, @Nullable T result);
}
