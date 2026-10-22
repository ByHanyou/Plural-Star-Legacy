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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Trace;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import app.notifee.core.event.ForegroundServiceEvent;
import app.notifee.core.event.NotificationEvent;
import app.notifee.core.interfaces.MethodCallResult;
import app.notifee.core.model.NotificationModel;
import app.notifee.core.utility.ParcelableCompatReader;

public class ForegroundService extends Service {
  private static final String TAG = "ForegroundService";
  public static final String START_FOREGROUND_SERVICE_ACTION =
      "app.notifee.core.ForegroundService.START";
  public static final String STOP_FOREGROUND_SERVICE_ACTION =
      "app.notifee.core.ForegroundService.STOP";

  private static final Object sLock = new Object();
  private static final String DEFENSIVE_CHANNEL_ID = "notifee_fg_default";
  private static final int DEFENSIVE_NOTIFICATION_ID = Integer.MAX_VALUE - 1;
  private static final String PLACEHOLDER_CHANNEL_ID = "ps_fg_placeholder";

  private boolean mPlaceholderIsForeground = false;

  private volatile boolean mStartForegroundCalled = false;

  public static String mCurrentNotificationId = null;

  public static int mCurrentForegroundServiceType = -1;

  private static Bundle mCurrentNotificationBundle = null;

  static void start(int hashCode, Notification notification, Bundle notificationBundle) {
    Context context = ContextHolder.getApplicationContext();
    if (context == null) {
      Logger.e(TAG, "Application context is null; cannot start ForegroundService.");
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      try {
        ComponentName component = new ComponentName(context, ForegroundService.class);
        ServiceInfo info =
            context.getPackageManager().getServiceInfo(component, PackageManager.GET_META_DATA);
        if (info.getForegroundServiceType() == ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE) {
          Logger.e(
              TAG,
              "No foregroundServiceType declared for app.notifee.core.ForegroundService in"
                  + " your AndroidManifest.xml. Android 14+ requires an explicit"
                  + " foregroundServiceType. Add <service"
                  + " android:name=\"app.notifee.core.ForegroundService\""
                  + " android:foregroundServiceType=\"yourType\" /> to your app manifest."
                  + " Aborting foreground service start.");
          return;
        }
      } catch (PackageManager.NameNotFoundException e) {
        Logger.e(TAG, "ForegroundService not found in manifest", e);
        return;
      }
    }

    Intent intent = new Intent(context, ForegroundService.class);
    intent.setAction(START_FOREGROUND_SERVICE_ACTION);
    intent.putExtra("hashCode", hashCode);
    intent.putExtra("notification", notification);
    intent.putExtra("notificationBundle", notificationBundle);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      try {
        context.startForegroundService(intent);
      } catch (Exception e) {
        Logger.w(TAG, "startForegroundService() failed; posting notification without FGS", e);
        try {
          NotificationManagerCompat.from(context).notify(hashCode, notification);
        } catch (Exception notifyError) {
          Logger.e(TAG, "Fallback notify() after failed FGS start also failed", notifyError);
        }
      }
    } else {
      try {
        context.startService(intent);
      } catch (Exception e) {
        Logger.w(TAG, "startService() failed; posting notification without FGS", e);
        try {
          NotificationManagerCompat.from(context).notify(hashCode, notification);
        } catch (Exception notifyError) {
          Logger.e(TAG, "Fallback notify() after failed service start also failed", notifyError);
        }
      }
    }
  }

  static void stop() {
    Context context = ContextHolder.getApplicationContext();
    if (context == null) {
      Logger.e(TAG, "Application context is null; cannot stop ForegroundService.");
      return;
    }

    Intent intent = new Intent(context, ForegroundService.class);
    intent.setAction(STOP_FOREGROUND_SERVICE_ACTION);

    try {
      context.startService(intent);
    } catch (IllegalStateException illegalStateException) {
      Logger.w(
          TAG,
          "startService() threw IllegalStateException on STOP path;"
              + " falling back to stopService()",
          illegalStateException);
      context.stopService(intent);
    } catch (Exception exception) {
      Logger.e(TAG, "Unable to stop foreground service", exception);
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();
    satisfyContractEarly();
  }

  @SuppressLint({"ForegroundServiceType", "MissingPermission"})
  private void satisfyContractEarly() {
    if (mStartForegroundCalled) {
      return;
    }
    try {
      int declaredTypes = 0;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        declaredTypes = getDeclaredForegroundServiceType();
        if (!hasAnyForegroundServiceType(declaredTypes)) {
          return;
        }
      }
      ensurePlaceholderChannel();
      int appIcon = getApplicationInfo().icon;
      Notification placeholder =
          new NotificationCompat.Builder(this, PLACEHOLDER_CHANNEL_ID)
              .setSmallIcon(appIcon != 0 ? appIcon : android.R.drawable.ic_dialog_info)
              .setContentTitle(getApplicationInfo().loadLabel(getPackageManager()))
              .setPriority(NotificationCompat.PRIORITY_MIN)
              .setSilent(true)
              .setOngoing(true)
              .build();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
            DEFENSIVE_NOTIFICATION_ID,
            placeholder,
            selectDefensiveForegroundServiceType(declaredTypes));
      } else {
        startForeground(DEFENSIVE_NOTIFICATION_ID, placeholder);
      }
      mStartForegroundCalled = true;
      mPlaceholderIsForeground = true;
    } catch (Exception e) {
      Logger.w(TAG, "Early startForeground() placeholder failed; continuing", e);
    }
  }

  @SuppressLint({"ForegroundServiceType", "MissingPermission"})
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Trace.beginSection("notifee:ForegroundService.onStartCommand");
    try {
      if (intent == null || STOP_FOREGROUND_SERVICE_ACTION.equals(intent.getAction())) {
        ensureStartForegroundContractSatisfied();
        stopSelf();
        synchronized (sLock) {
          mCurrentNotificationId = null;
          mCurrentForegroundServiceType = -1;
          mCurrentNotificationBundle = null;
        }
        return Service.START_STICKY_COMPATIBILITY;
      }

      Bundle extras = intent.getExtras();

      if (extras != null) {
        int hashCode = extras.getInt("hashCode");
        Notification notification =
            ParcelableCompatReader.getParcelable(extras, "notification", Notification.class);
        Bundle bundle = extras.getBundle("notificationBundle");

        if (notification != null && bundle != null) {
          NotificationModel notificationModel = NotificationModel.fromBundle(bundle);

          Object pendingEvent = null;
          boolean noneEarlyReturn = false;

          synchronized (sLock) {
            if (mCurrentNotificationId == null) {
              mCurrentNotificationId = notificationModel.getId();
              mCurrentNotificationBundle = bundle;

              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int foregroundServiceType =
                    notificationModel.getAndroid().getForegroundServiceType();
                if (foregroundServiceType == ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST) {
                  foregroundServiceType = resolveManifestServiceType();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    && foregroundServiceType == ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE) {
                  Logger.e(
                      TAG,
                      "Resolved foreground service type is NONE on API 34+; aborting"
                          + " startForeground to avoid InvalidForegroundServiceTypeException.");
                  mCurrentNotificationId = null;
                  mCurrentForegroundServiceType = -1;
                  mCurrentNotificationBundle = null;
                  noneEarlyReturn = true;
                } else {
                  Trace.beginSection("notifee:startForeground");
                  try {
                    startForeground(hashCode, notification, foregroundServiceType);
                  } finally {
                    Trace.endSection();
                  }
                  mStartForegroundCalled = true;
                  mPlaceholderIsForeground = false;
                  mCurrentForegroundServiceType = foregroundServiceType;
                }
              } else {
                Trace.beginSection("notifee:startForeground");
                try {
                  startForeground(hashCode, notification);
                } finally {
                  Trace.endSection();
                }
                mStartForegroundCalled = true;
                mPlaceholderIsForeground = false;
              }

              if (!noneEarlyReturn) {
                final MethodCallResult<Void> methodCallResult =
                    (e, aVoid) -> {
                      stopForegroundCompat();
                      synchronized (sLock) {
                        mCurrentNotificationId = null;
                        mCurrentForegroundServiceType = -1;
                        mCurrentNotificationBundle = null;
                      }
                    };

                pendingEvent = new ForegroundServiceEvent(notificationModel, methodCallResult);
              }
            } else {
              if (mCurrentNotificationId.equals(notificationModel.getId())) {
                boolean shouldPostNotificationAgain = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                  int foregroundServiceType =
                      notificationModel.getAndroid().getForegroundServiceType();
                  if (foregroundServiceType == ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST) {
                    foregroundServiceType = resolveManifestServiceType();
                  }
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                      && foregroundServiceType == ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE) {
                    Logger.e(
                        TAG,
                        "Resolved foreground service type is NONE on API 34+; skipping type"
                            + " change.");
                  } else if (foregroundServiceType != mCurrentForegroundServiceType
                      || mPlaceholderIsForeground) {
                    Trace.beginSection("notifee:startForeground");
                    try {
                      startForeground(hashCode, notification, foregroundServiceType);
                    } finally {
                      Trace.endSection();
                    }
                    mStartForegroundCalled = true;
                    mPlaceholderIsForeground = false;
                    mCurrentForegroundServiceType = foregroundServiceType;
                    shouldPostNotificationAgain = false;
                  }
                } else if (mPlaceholderIsForeground) {
                  Trace.beginSection("notifee:startForeground");
                  try {
                    startForeground(hashCode, notification);
                  } finally {
                    Trace.endSection();
                  }
                  mStartForegroundCalled = true;
                  mPlaceholderIsForeground = false;
                  shouldPostNotificationAgain = false;
                }
                if (shouldPostNotificationAgain) {
                  NotificationManagerCompat.from(ContextHolder.getApplicationContext())
                      .notify(hashCode, notification);
                }
              } else {
                pendingEvent =
                    new NotificationEvent(
                        NotificationEvent.TYPE_FG_ALREADY_EXIST, notificationModel);
              }
            }
          }

          if (noneEarlyReturn) {
            ensureStartForegroundContractSatisfied();
            return START_NOT_STICKY;
          }

          try {
            NotificationManagerCompat.from(ContextHolder.getApplicationContext())
                .cancel(DEFENSIVE_NOTIFICATION_ID);
          } catch (Exception ignored) {}

          if (pendingEvent != null) {
            EventBus.post(pendingEvent);
          }
        }
      }

      return START_NOT_STICKY;
    } finally {
      Trace.endSection();
    }
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
  @Override
  public void onTimeout(int startId) {
    handleTimeout(startId, -1);
  }

  @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
  @Override
  public void onTimeout(int startId, int fgsType) {
    handleTimeout(startId, fgsType);
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  private static int resolveManifestServiceType() {
    try {
      Context context = ContextHolder.getApplicationContext();
      if (context == null) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ? ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
            : ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST;
      }
      ComponentName component = new ComponentName(context, ForegroundService.class);
      ServiceInfo info =
          context.getPackageManager().getServiceInfo(component, PackageManager.GET_META_DATA);
      int type = info.getForegroundServiceType();
      if (type != ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE) {
        return type;
      }
    } catch (PackageManager.NameNotFoundException e) {
      Logger.e(TAG, "ForegroundService not found in manifest", e);
    }
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ? ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
        : ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST;
  }

  private void ensureDefensiveChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      if (nm != null && nm.getNotificationChannel(DEFENSIVE_CHANNEL_ID) == null) {
        NotificationChannel channel =
            new NotificationChannel(
                DEFENSIVE_CHANNEL_ID, "Foreground Service", NotificationManager.IMPORTANCE_MIN);
        channel.setShowBadge(false);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        nm.createNotificationChannel(channel);
      }
    }
  }

  private void ensurePlaceholderChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      if (nm != null && nm.getNotificationChannel(PLACEHOLDER_CHANNEL_ID) == null) {
        NotificationChannel channel =
            new NotificationChannel(
                PLACEHOLDER_CHANNEL_ID, "Background service", NotificationManager.IMPORTANCE_MIN);
        channel.setShowBadge(false);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        nm.createNotificationChannel(channel);
      }
    }
  }

  @Override
  public void onDestroy() {
    try {
      NotificationManagerCompat.from(ContextHolder.getApplicationContext())
          .cancel(DEFENSIVE_NOTIFICATION_ID);
    } catch (Exception ignored) {}
    mPlaceholderIsForeground = false;
    super.onDestroy();
  }

  @SuppressWarnings("deprecation")
  private void stopForegroundCompat() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      stopForeground(STOP_FOREGROUND_REMOVE);
    } else {
      stopForeground(true);
    }
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  private int getDeclaredForegroundServiceType() {
    try {
      ComponentName component = new ComponentName(this, ForegroundService.class);
      ServiceInfo info =
          getPackageManager().getServiceInfo(component, PackageManager.GET_META_DATA);
      return info.getForegroundServiceType();
    } catch (PackageManager.NameNotFoundException e) {
      return 0;
    }
  }

  @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
  private static int selectDefensiveForegroundServiceType(int declaredTypes) {
    int[] preferredTypes = {
      ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
      ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
      ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
      ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
    };

    for (int type : preferredTypes) {
      if (isDeclared(declaredTypes, type)) {
        return type;
      }
    }

    return declaredTypes;
  }

  private static boolean isDeclared(int declaredTypes, int type) {
    return (declaredTypes & type) == type;
  }

  private static boolean hasAnyForegroundServiceType(int foregroundServiceTypes) {
    return foregroundServiceTypes != ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
        && foregroundServiceTypes != ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST;
  }

  @SuppressLint({"ForegroundServiceType", "MissingPermission"})
  private void ensureStartForegroundContractSatisfied() {
    if (mStartForegroundCalled) {
      return;
    }

    int declaredTypes = 0;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      declaredTypes = getDeclaredForegroundServiceType();
      if (!hasAnyForegroundServiceType(declaredTypes)) {
        String msg =
            "react-native-notify-kit: ForegroundService cannot start — "
                + "no foregroundServiceType declared in AndroidManifest.xml on API 34+. "
                + "See https://github.com/marcocrupi/react-native-notify-kit"
                + "#foreground-service-setup-android-14";
        Logger.e(TAG, msg);
        throw new RuntimeException(msg);
      }
    }

    try {
      ensureDefensiveChannel();
      Notification placeholder =
          new NotificationCompat.Builder(this, DEFENSIVE_CHANNEL_ID)
              .setSmallIcon(android.R.drawable.ic_dialog_info)
              .build();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        int defensiveTypes = selectDefensiveForegroundServiceType(declaredTypes);
        startForeground(DEFENSIVE_NOTIFICATION_ID, placeholder, defensiveTypes);
      } else {
        startForeground(DEFENSIVE_NOTIFICATION_ID, placeholder);
      }
      mStartForegroundCalled = true;
      stopForegroundCompat();
    } catch (SecurityException e) {
      Logger.w(
          TAG,
          "Defensive startForeground() was denied by Android while stopping the service; "
              + "continuing with stopSelf() to avoid a fatal app crash.",
          e);
    } catch (IllegalStateException e) {
      Logger.w(
          TAG,
          "Defensive startForeground() was not allowed (background start restriction); "
              + "continuing with stopSelf().",
          e);
    } catch (Exception e) {
      String msg =
          "react-native-notify-kit: defensive startForeground() failed. "
              + "This indicates an inconsistent service state and the process cannot recover.";
      Logger.e(TAG, msg, e);
      throw new RuntimeException(msg, e);
    }
  }

  private void handleTimeout(int startId, int fgsType) {
    Logger.e(
        TAG,
        "Foreground service timed out (startId="
            + startId
            + ", type="
            + fgsType
            + "). Stopping service.");

    Bundle notifBundle;
    synchronized (sLock) {
      notifBundle = mCurrentNotificationBundle;
      mCurrentNotificationId = null;
      mCurrentForegroundServiceType = -1;
      mCurrentNotificationBundle = null;
    }

    stopForegroundCompat();
    stopSelf(startId);

    if (notifBundle != null) {
      NotificationModel model = NotificationModel.fromBundle(notifBundle);
      Bundle extras = new Bundle();
      extras.putInt("startId", startId);
      extras.putInt("fgsType", fgsType);
      EventBus.post(new NotificationEvent(NotificationEvent.TYPE_FG_TIMEOUT, model, extras));
    }
  }
}
