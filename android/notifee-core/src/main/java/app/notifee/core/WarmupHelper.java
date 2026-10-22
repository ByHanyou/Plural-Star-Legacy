package app.notifee.core;

/*
 * Copyright (c) 2016-present Invertase Limited & Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this library except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import android.content.Context;
import android.os.Trace;
import androidx.core.app.NotificationManagerCompat;

@KeepForSdk
public final class WarmupHelper {

  private static final String TAG = "WarmupHelper";

  static final String[] WARMUP_CLASSES = {
    "app.notifee.core.ForegroundService",
    "app.notifee.core.NotificationManager",
    "app.notifee.core.model.NotificationModel",
    "app.notifee.core.model.NotificationAndroidModel",
    "app.notifee.core.model.ChannelModel",
    "androidx.core.app.NotificationCompat$Builder",
    "androidx.core.app.NotificationManagerCompat",
  };

  private WarmupHelper() {}

  @KeepForSdk
  public static void runWarmup(Context context) {
    Trace.beginSection("notifee:warmup");
    try {
      ClassLoader classLoader = context.getClassLoader();

      for (String className : WARMUP_CLASSES) {
        try {
          Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException e) {
          Logger.d(TAG, "Warmup class not found: " + className);
        }
      }

      try {
        NotificationManagerCompat.from(context).getNotificationChannels();
      } catch (Exception e) {
        Logger.d(TAG, "Warmup Binder pre-warm failed: " + e.getMessage());
      }
    } finally {
      Trace.endSection();
    }
  }
}
