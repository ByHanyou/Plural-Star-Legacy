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

import static app.notifee.core.ContextHolder.getApplicationContext;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.AlarmManagerCompat;
import app.notifee.core.database.WorkDataEntity;
import app.notifee.core.database.WorkDataRepository;
import app.notifee.core.event.NotificationEvent;
import app.notifee.core.model.NotificationAndroidModel;
import app.notifee.core.model.NotificationModel;
import app.notifee.core.model.TimestampTriggerModel;
import app.notifee.core.utility.AlarmUtils;
import app.notifee.core.utility.BundleValueReader;
import app.notifee.core.utility.ExtendedListenableFuture;
import app.notifee.core.utility.ObjectUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

class NotifeeAlarmManager {
  private static final String TAG = "NotifeeAlarmManager";
  private static final String NOTIFICATION_ID_INTENT_KEY = "notificationId";
  private static final ExecutorService alarmManagerExecutor = Executors.newCachedThreadPool();
  private static final ListeningExecutorService alarmManagerListeningExecutor =
      MoreExecutors.listeningDecorator(alarmManagerExecutor);

  private static final long STALE_TRIGGER_GRACE_PERIOD_MS = TimeUnit.HOURS.toMillis(24);

  private static final AtomicBoolean rescheduleInProgress = new AtomicBoolean(false);

  private static final ScheduledExecutorService TIMEOUT_SCHEDULER =
      Executors.newSingleThreadScheduledExecutor();
  private static final long RECEIVER_WRITE_TIMEOUT_SECONDS = 8;

  private static void finishReceiverWhenDone(
      @NonNull ListenableFuture<?> future,
      @Nullable BroadcastReceiver.PendingResult pendingResult,
      @Nullable Runnable beforeFinish,
      @NonNull String logContext) {
    ListenableFuture<?> bounded =
        Futures.withTimeout(
            future, RECEIVER_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS, TIMEOUT_SCHEDULER);
    Futures.addCallback(
        bounded,
        new FutureCallback<Object>() {
          @Override
          public void onSuccess(Object result) {
            if (beforeFinish != null) {
              beforeFinish.run();
            }
            if (pendingResult != null) {
              pendingResult.finish();
            }
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            if (t instanceof TimeoutException) {
              Logger.w(
                  TAG,
                  "Room write for "
                      + logContext
                      + " did not complete within "
                      + RECEIVER_WRITE_TIMEOUT_SECONDS
                      + "s; finishing BroadcastReceiver anyway to avoid ANR");
            } else {
              Logger.e(TAG, "Failure in " + logContext, new Exception(t));
            }
            if (beforeFinish != null) {
              beforeFinish.run();
            }
            if (pendingResult != null) {
              pendingResult.finish();
            }
          }
        },
        alarmManagerListeningExecutor);
  }

  static void displayScheduledNotification(
      Bundle alarmManagerNotification, @Nullable BroadcastReceiver.PendingResult pendingResult) {
    if (alarmManagerNotification == null) {
      if (pendingResult != null) {
        pendingResult.finish();
      }
      return;
    }
    String id = alarmManagerNotification.getString(NOTIFICATION_ID_INTENT_KEY);

    if (id == null) {
      if (pendingResult != null) {
        pendingResult.finish();
      }
      return;
    }

    WorkDataRepository workDataRepository = new WorkDataRepository(getApplicationContext());

    ListenableFuture<Void> displayAndPersistFuture =
        Futures.transformAsync(
            workDataRepository.getWorkDataById(id),
            workDataEntity -> {
              if (workDataEntity == null
                  || workDataEntity.getNotification() == null
                  || workDataEntity.getTrigger() == null) {
                Logger.w(
                    TAG, "Attempted to handle doScheduledWork but no notification data was found.");
                return Futures.immediateFuture(null);
              }

              Bundle triggerBundle = ObjectUtils.bytesToBundle(workDataEntity.getTrigger());
              Bundle notificationBundle =
                  ObjectUtils.bytesToBundle(workDataEntity.getNotification());
              NotificationModel notificationModel =
                  NotificationModel.fromBundle(notificationBundle);

              return Futures.transformAsync(
                  NotificationManager.displayNotification(notificationModel, triggerBundle),
                  voidDisplayedNotification -> {
                    if (triggerBundle.containsKey("repeatFrequency")
                        && BundleValueReader.getIntPreserving(triggerBundle, "repeatFrequency")
                            != -1) {
                      TimestampTriggerModel trigger =
                          TimestampTriggerModel.fromBundle(triggerBundle);
                      scheduleTimestampTriggerNotification(notificationModel, trigger);
                      return WorkDataRepository.getInstance(getApplicationContext())
                          .update(
                              new WorkDataEntity(
                                  id,
                                  workDataEntity.getNotification(),
                                  ObjectUtils.bundleToBytes(trigger.toBundle()),
                                  true));
                    }
                    return WorkDataRepository.getInstance(getApplicationContext()).deleteById(id);
                  },
                  alarmManagerExecutor);
            },
            alarmManagerExecutor);

    finishReceiverWhenDone(
        displayAndPersistFuture, pendingResult, null, "displayScheduledNotification[" + id + "]");
  }

  public static PendingIntent getAlarmManagerIntentForNotification(String notificationId) {
    try {
      Context context = getApplicationContext();
      Intent notificationIntent = new Intent(context, NotificationAlarmReceiver.class);
      notificationIntent.putExtra(NOTIFICATION_ID_INTENT_KEY, notificationId);
      return PendingIntent.getBroadcast(
          context,
          notificationId.hashCode(),
          notificationIntent,
          PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

    } catch (Exception e) {
      Logger.e(TAG, "Unable to create AlarmManager intent", e);
    }

    return null;
  }

  static void scheduleTimestampTriggerNotification(
      NotificationModel notificationModel, TimestampTriggerModel timestampTrigger) {

    PendingIntent pendingIntent = getAlarmManagerIntentForNotification(notificationModel.getId());

    if (pendingIntent == null) {
      Logger.w(
          TAG, "Failed to create PendingIntent for notification: " + notificationModel.getId());
      return;
    }

    AlarmManager alarmManager = AlarmUtils.getAlarmManager();

    TimestampTriggerModel.AlarmType alarmType = timestampTrigger.getAlarmType();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

      boolean isExactAlarm =
          Arrays.asList(
                  TimestampTriggerModel.AlarmType.SET_EXACT,
                  TimestampTriggerModel.AlarmType.SET_EXACT_AND_ALLOW_WHILE_IDLE,
                  TimestampTriggerModel.AlarmType.SET_ALARM_CLOCK)
              .contains(alarmType);
      if (isExactAlarm && !alarmManager.canScheduleExactAlarms()) {
        Logger.w(
            TAG, "SCHEDULE_EXACT_ALARM permission not granted. Falling back to inexact alarm.");
        timestampTrigger.setNextTimestamp();
        AlarmManagerCompat.setAndAllowWhileIdle(
            alarmManager, AlarmManager.RTC_WAKEUP, timestampTrigger.getTimestamp(), pendingIntent);
        return;
      }
    }

    timestampTrigger.setNextTimestamp();

    try {
      switch (alarmType) {
        case SET:
          alarmManager.set(AlarmManager.RTC_WAKEUP, timestampTrigger.getTimestamp(), pendingIntent);
          break;
        case SET_AND_ALLOW_WHILE_IDLE:
          AlarmManagerCompat.setAndAllowWhileIdle(
              alarmManager,
              AlarmManager.RTC_WAKEUP,
              timestampTrigger.getTimestamp(),
              pendingIntent);
          break;
        case SET_EXACT:
          AlarmManagerCompat.setExact(
              alarmManager,
              AlarmManager.RTC_WAKEUP,
              timestampTrigger.getTimestamp(),
              pendingIntent);
          break;
        case SET_EXACT_AND_ALLOW_WHILE_IDLE:
          AlarmManagerCompat.setExactAndAllowWhileIdle(
              alarmManager,
              AlarmManager.RTC_WAKEUP,
              timestampTrigger.getTimestamp(),
              pendingIntent);
          break;
        case SET_ALARM_CLOCK:
          Bundle showIntentPressAction = buildShowIntentPressActionBundle(notificationModel);
          PendingIntent pendingLaunchIntent =
              NotificationPendingIntent.createIntent(
                  notificationModel.getHashCode(),
                  showIntentPressAction,
                  NotificationEvent.TYPE_PRESS,
                  new String[] {"notification"},
                  notificationModel.toBundle());

          AlarmManagerCompat.setAlarmClock(
              alarmManager, timestampTrigger.getTimestamp(), pendingLaunchIntent, pendingIntent);
          break;
      }
    } catch (SecurityException e) {
      Logger.w(
          TAG,
          "SecurityException scheduling exact alarm, falling back to inexact: " + e.getMessage());
      try {
        AlarmManagerCompat.setAndAllowWhileIdle(
            alarmManager, AlarmManager.RTC_WAKEUP, timestampTrigger.getTimestamp(), pendingIntent);
      } catch (SecurityException e2) {
        Logger.e(TAG, "Failed to schedule even inexact alarm", e2);
      }
    }
  }

  @VisibleForTesting
  static Bundle buildShowIntentPressActionBundle(NotificationModel notificationModel) {
    NotificationAndroidModel androidModel = notificationModel.getAndroid();
    Bundle pressAction = androidModel != null ? androidModel.getPressAction() : null;

    if (pressAction == null
        || NotificationPendingIntent.PRESS_ACTION_OPT_OUT_ID.equals(pressAction.getString("id"))) {
      Bundle defaultPressAction = new Bundle();
      defaultPressAction.putString("id", "default");
      defaultPressAction.putString("launchActivity", "default");
      return defaultPressAction;
    }

    return pressAction;
  }

  ListenableFuture<List<WorkDataEntity>> getScheduledNotifications() {
    WorkDataRepository workDataRepository = new WorkDataRepository(getApplicationContext());
    return workDataRepository.getAllWithAlarmManager(true);
  }

  public static void cancelNotification(String notificationId) {
    PendingIntent pendingIntent = getAlarmManagerIntentForNotification(notificationId);
    AlarmManager alarmManager = AlarmUtils.getAlarmManager();
    if (pendingIntent != null) {
      alarmManager.cancel(pendingIntent);
    }
  }

  public static ListenableFuture<Void> cancelAllNotifications() {
    WorkDataRepository workDataRepository = WorkDataRepository.getInstance(getApplicationContext());

    return new ExtendedListenableFuture<>(workDataRepository.getAllWithAlarmManager(true))
        .continueWith(
            workDataEntities -> {
              if (workDataEntities != null) {
                for (WorkDataEntity workDataEntity : workDataEntities) {
                  NotifeeAlarmManager.cancelNotification(workDataEntity.getId());
                }
              }
              return Futures.immediateFuture(null);
            },
            alarmManagerListeningExecutor);
  }

  ListenableFuture<Void> rescheduleNotification(WorkDataEntity workDataEntity) {
    if (workDataEntity.getNotification() == null || workDataEntity.getTrigger() == null) {
      return Futures.immediateFuture(null);
    }

    byte[] notificationBytes = workDataEntity.getNotification();
    byte[] triggerBytes = workDataEntity.getTrigger();
    Bundle triggerBundle = ObjectUtils.bytesToBundle(triggerBytes);

    NotificationModel notificationModel =
        NotificationModel.fromBundle(ObjectUtils.bytesToBundle(notificationBytes));

    int triggerType = BundleValueReader.getIntPreserving(triggerBundle, "type");

    switch (triggerType) {
      case 0:
        TimestampTriggerModel trigger = TimestampTriggerModel.fromBundle(triggerBundle);
        if (!trigger.getWithAlarmManager()) {
          return Futures.immediateFuture(null);
        }

        ListenableFuture<Void> staleResult =
            handleStaleNonRepeatingTrigger(notificationModel, trigger);
        if (staleResult != null) {
          return staleResult;
        }

        scheduleTimestampTriggerNotification(notificationModel, trigger);
        return WorkDataRepository.getInstance(getApplicationContext())
            .update(
                new WorkDataEntity(
                    workDataEntity.getId(),
                    workDataEntity.getNotification(),
                    ObjectUtils.bundleToBytes(trigger.toBundle()),
                    workDataEntity.getWithAlarmManager()));
      default:
        return Futures.immediateFuture(null);
    }
  }

  @Nullable
  static ListenableFuture<Void> handleStaleNonRepeatingTrigger(
      NotificationModel notificationModel, TimestampTriggerModel trigger) {
    return handleStaleNonRepeatingTrigger(
        notificationModel, trigger, alarmManagerListeningExecutor);
  }

  @VisibleForTesting
  @Nullable
  static ListenableFuture<Void> handleStaleNonRepeatingTrigger(
      NotificationModel notificationModel, TimestampTriggerModel trigger, Executor executor) {
    if (trigger.getRepeatFrequency() != null) {
      return null;
    }
    long nowMs = System.currentTimeMillis();
    long triggerTs = trigger.getTimestamp();
    if (triggerTs >= nowMs) {
      return null;
    }

    String id = notificationModel.getId();
    WorkDataRepository workRepo = WorkDataRepository.getInstance(getApplicationContext());
    long stalenessMs = nowMs - triggerTs;

    if (stalenessMs > STALE_TRIGGER_GRACE_PERIOD_MS) {
      Logger.i(
          TAG,
          "Deleting stale non-repeating trigger (age "
              + stalenessMs
              + "ms > "
              + STALE_TRIGGER_GRACE_PERIOD_MS
              + "ms grace period): "
              + id);
      return workRepo.deleteById(id);
    }

    Logger.i(
        TAG,
        "Firing stale non-repeating trigger once within grace period (age "
            + stalenessMs
            + "ms): "
            + id);

    ListenableFuture<Void> displayAttempt =
        Futures.submitAsync(
            () -> NotificationManager.displayNotification(notificationModel, null), executor);

    ListenableFuture<Void> resilientDisplay =
        Futures.catchingAsync(
            displayAttempt,
            Exception.class,
            t -> {
              Logger.w(
                  TAG, "Late-fire of stale trigger " + id + " failed, proceeding to delete row", t);
              return Futures.immediateFuture(null);
            },
            executor);

    return Futures.transformAsync(resilientDisplay, ignored -> workRepo.deleteById(id), executor);
  }

  void rescheduleNotifications(@Nullable BroadcastReceiver.PendingResult pendingResult) {
    if (!rescheduleInProgress.compareAndSet(false, true)) {
      Logger.i(TAG, "Reschedule already in progress, skipping duplicate request");
      if (pendingResult != null) {
        pendingResult.finish();
      }
      return;
    }

    final Runnable releaseLock = () -> rescheduleInProgress.set(false);

    Logger.d(TAG, "Reschedule Notifications on reboot");
    Futures.addCallback(
        getScheduledNotifications(),
        new FutureCallback<List<WorkDataEntity>>() {
          @Override
          public void onSuccess(List<WorkDataEntity> workDataEntities) {
            Logger.d(
                TAG,
                "Reschedule starting for "
                    + (workDataEntities != null ? workDataEntities.size() : 0)
                    + " recurring alarms");

            if (workDataEntities == null || workDataEntities.isEmpty()) {
              releaseLock.run();
              if (pendingResult != null) {
                pendingResult.finish();
              }
              return;
            }

            List<ListenableFuture<Void>> updateFutures = new ArrayList<>(workDataEntities.size());
            for (WorkDataEntity workDataEntity : workDataEntities) {
              try {
                updateFutures.add(rescheduleNotification(workDataEntity));
              } catch (Throwable t) {
                Logger.w(
                    TAG,
                    "Failed to reschedule entity "
                        + workDataEntity.getId()
                        + ": "
                        + t.getMessage());
              }
            }

            ListenableFuture<List<Void>> combined = Futures.allAsList(updateFutures);
            finishReceiverWhenDone(combined, pendingResult, releaseLock, "rescheduleNotifications");
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Logger.e(TAG, "Failed to reschedule notifications", new Exception(t));
            releaseLock.run();
            if (pendingResult != null) {
              pendingResult.finish();
            }
          }
        },
        alarmManagerListeningExecutor);
  }
}
