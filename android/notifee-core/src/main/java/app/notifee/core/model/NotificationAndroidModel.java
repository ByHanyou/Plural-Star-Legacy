package app.notifee.core.model;

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

import android.app.Notification;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import app.notifee.core.Logger;
import app.notifee.core.utility.BundleValueReader;
import app.notifee.core.utility.ObjectUtils;
import app.notifee.core.utility.ParcelableCompatReader;
import app.notifee.core.utility.ResourceUtils;
import java.util.ArrayList;
import java.util.Objects;

@Keep
public class NotificationAndroidModel {
  private static final String TAG = "NotificationAndroidModel";
  private Bundle mNotificationAndroidBundle;

  private NotificationAndroidModel(Bundle bundle) {
    mNotificationAndroidBundle = bundle;
  }

  public static NotificationAndroidModel fromBundle(Bundle bundle) {
    return new NotificationAndroidModel(bundle);
  }

  public @Nullable ArrayList<NotificationAndroidActionModel> getActions() {
    if (mNotificationAndroidBundle.containsKey("actions")) {
      ArrayList<Bundle> actionBundles =
          Objects.requireNonNull(
              ParcelableCompatReader.getParcelableArrayList(
                  mNotificationAndroidBundle, "actions", Bundle.class));
      ArrayList<NotificationAndroidActionModel> actions = new ArrayList<>(actionBundles.size());

      for (Bundle actionBundle : actionBundles) {
        actions.add(NotificationAndroidActionModel.fromBundle(actionBundle));
      }

      return actions;
    }

    return null;
  }

  @RequiresApi(api = Build.VERSION_CODES.Q)
  public int getForegroundServiceType() {
    if (!mNotificationAndroidBundle.containsKey("foregroundServiceTypes")) {
      return ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST;
    }

    ArrayList<?> foregroundServiceTypesArrayList =
        Objects.requireNonNull(
            BundleValueReader.getArrayListValue(
                mNotificationAndroidBundle, "foregroundServiceTypes"));

    if (foregroundServiceTypesArrayList.isEmpty()) {
      Logger.w(TAG, "foregroundServiceTypes is empty; treating as absent");
      return ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST;
    }

    int foregroundServiceType = 0;
    for (int i = 0; i < foregroundServiceTypesArrayList.size(); i++) {
      foregroundServiceType |= ObjectUtils.getInt(foregroundServiceTypesArrayList.get(i));
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        && foregroundServiceType == ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE) {
      return ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST;
    }

    return foregroundServiceType;
  }

  public Boolean getAsForegroundService() {
    return mNotificationAndroidBundle.getBoolean("asForegroundService", false);
  }

  public int getForegroundServiceBehavior() {
    return BundleValueReader.getIntPreserving(
        mNotificationAndroidBundle,
        "foregroundServiceBehavior",
        NotificationCompat.FOREGROUND_SERVICE_DEFAULT);
  }

  public Boolean getLightUpScreen() {
    return mNotificationAndroidBundle.getBoolean("lightUpScreen", false);
  }

  public Boolean getAutoCancel() {
    return mNotificationAndroidBundle.getBoolean("autoCancel", false);
  }

  public @Nullable Integer getBadgeIconType() {
    return BundleValueReader.getIntPreserving(
        mNotificationAndroidBundle, "badgeIconType", NotificationCompat.BADGE_ICON_LARGE);
  }

  public @NonNull String getChannelId() {
    if (mNotificationAndroidBundle.containsKey("channelId")) {
      return Objects.requireNonNull(mNotificationAndroidBundle.getString("channelId"));
    }

    return "";
  }

  public @Nullable String getCategory() {
    return mNotificationAndroidBundle.getString("category");
  }

  public @Nullable Integer getColor() {
    if (mNotificationAndroidBundle.containsKey("color")) {
      return Color.parseColor(mNotificationAndroidBundle.getString("color"));
    }

    return null;
  }

  public Boolean getColorized() {
    return mNotificationAndroidBundle.getBoolean("colorized", false);
  }

  public Boolean getChronometerCountDown() {
    if (mNotificationAndroidBundle.containsKey("chronometerDirection")) {
      String direction = mNotificationAndroidBundle.getString("chronometerDirection");
      return direction != null && direction.equals("down");
    }

    return false;
  }

  @SuppressWarnings("deprecation")
  public Integer getDefaults(Boolean hasCustomSound) {
    Integer defaults = 0;

    if (mNotificationAndroidBundle.containsKey("defaults")) {
      ArrayList<Integer> defaultsArray = mNotificationAndroidBundle.getIntegerArrayList("defaults");

      for (Integer integer : Objects.requireNonNull(defaultsArray)) {
        defaults |= integer;
      }
    } else {
      defaults = Notification.DEFAULT_ALL;
    }

    if (hasCustomSound) {
      defaults &= ~Notification.DEFAULT_SOUND;
    }

    if (!mNotificationAndroidBundle.containsKey("vibrationPattern")) {
      defaults &= ~Notification.DEFAULT_VIBRATE;
    }

    if (mNotificationAndroidBundle.containsKey("lights")) {
      defaults &= ~Notification.DEFAULT_LIGHTS;
    }

    return defaults;
  }

  public @Nullable String getTag() {
    return mNotificationAndroidBundle.getString("tag");
  }

  public @Nullable String getGroup() {
    return mNotificationAndroidBundle.getString("groupId");
  }

  public int getGroupAlertBehaviour() {
    return BundleValueReader.getIntPreserving(
        mNotificationAndroidBundle, "groupAlertBehavior", NotificationCompat.GROUP_ALERT_ALL);
  }

  public Boolean getGroupSummary() {
    return mNotificationAndroidBundle.getBoolean("groupSummary", false);
  }

  public @Nullable CharSequence[] getInputHistory() {
    if (mNotificationAndroidBundle.containsKey("inputHistory")) {
      ArrayList<String> inputHistoryArray =
          mNotificationAndroidBundle.getStringArrayList("inputHistory");
      return Objects.requireNonNull(inputHistoryArray)
          .toArray(new CharSequence[inputHistoryArray.size()]);
    }

    return null;
  }

  public Boolean hasLargeIcon() {
    return mNotificationAndroidBundle.containsKey("largeIcon");
  }

  public String getLargeIcon() {
    if (hasLargeIcon()) {
      return Objects.requireNonNull(mNotificationAndroidBundle.getString("largeIcon"));
    }

    return null;
  }

  public Boolean getCircularLargeIcon() {
    return Objects.requireNonNull(
        mNotificationAndroidBundle.getBoolean("circularLargeIcon", false));
  }

  @SuppressWarnings("unchecked")
  public @Nullable ArrayList<Integer> getLights() {
    if (mNotificationAndroidBundle.containsKey("lights")) {
      try {
        ArrayList<?> lightList =
            Objects.requireNonNull(
                BundleValueReader.getArrayListValue(mNotificationAndroidBundle, "lights"));
        String rawColor = (String) lightList.get(0);

        ArrayList<Integer> lights = new ArrayList<>(3);
        lights.add(Color.parseColor(rawColor));
        lights.add((Integer) lightList.get(1));
        lights.add((Integer) lightList.get(2));

        return lights;
      } catch (Exception e) {
        Logger.e(TAG, "getLights -> Failed to parse lights", e);
        return null;
      }
    }

    return null;
  }

  public Boolean getLocalOnly() {
    return mNotificationAndroidBundle.getBoolean("localOnly", false);
  }

  public Integer getNumber() {
    if (mNotificationAndroidBundle.containsKey("badgeCount")) {
      return BundleValueReader.getIntPreserving(mNotificationAndroidBundle, "badgeCount");
    }

    return null;
  }

  public Boolean getOngoing() {
    return mNotificationAndroidBundle.getBoolean("ongoing", false);
  }

  public Boolean getLoopSound() {
    return mNotificationAndroidBundle.getBoolean("loopSound", false);
  }

  public int[] getFlags() {
    if (!mNotificationAndroidBundle.containsKey("flags")) {
      return null;
    }

    ArrayList<?> flagsArrayList =
        Objects.requireNonNull(
            BundleValueReader.getArrayListValue(mNotificationAndroidBundle, "flags"));

    int[] flagsArray = new int[flagsArrayList.size()];

    for (int i = 0; i < flagsArrayList.size(); i++) {
      flagsArray[i] = ObjectUtils.getInt(flagsArrayList.get(i));
    }

    return flagsArray;
  }

  public Boolean getOnlyAlertOnce() {
    return mNotificationAndroidBundle.getBoolean("onlyAlertOnce", false);
  }

  public Boolean hasFullScreenAction() {
    return mNotificationAndroidBundle.containsKey("fullScreenAction");
  }

  public @Nullable Bundle getPressAction() {
    return mNotificationAndroidBundle.getBundle("pressAction");
  }

  public @Nullable NotificationAndroidPressActionModel getFullScreenAction() {
    if (!hasFullScreenAction()) {
      return null;
    }

    return NotificationAndroidPressActionModel.fromBundle(
        mNotificationAndroidBundle.getBundle("fullScreenAction"));
  }

  @SuppressWarnings("deprecation")
  public int getPriority() {
    if (!mNotificationAndroidBundle.containsKey("importance")) {
      return NotificationCompat.PRIORITY_DEFAULT;
    }

    int importance =
        BundleValueReader.getIntPreserving(
            mNotificationAndroidBundle, "importance", NotificationManagerCompat.IMPORTANCE_DEFAULT);
    switch (importance) {
      case NotificationManagerCompat.IMPORTANCE_HIGH:
        return NotificationCompat.PRIORITY_HIGH;
      case NotificationManagerCompat.IMPORTANCE_MIN:
        return NotificationCompat.PRIORITY_LOW;
      case NotificationManagerCompat.IMPORTANCE_NONE:
        return NotificationCompat.PRIORITY_MIN;
      case NotificationManagerCompat.IMPORTANCE_DEFAULT:
      default:
        return NotificationCompat.PRIORITY_DEFAULT;
    }
  }

  public @Nullable AndroidProgress getProgress() {
    if (mNotificationAndroidBundle.containsKey("progress")) {
      Bundle progressBundle =
          Objects.requireNonNull(mNotificationAndroidBundle.getBundle("progress"));

      return new AndroidProgress(
          BundleValueReader.getIntPreserving(progressBundle, "max"),
          BundleValueReader.getIntPreserving(progressBundle, "current"),
          progressBundle.getBoolean("indeterminate", false));
    }

    return null;
  }

  public @Nullable String getShortcutId() {
    return mNotificationAndroidBundle.getString("shortcutId");
  }

  public Boolean getShowTimestamp() {
    return mNotificationAndroidBundle.getBoolean("showTimestamp", false);
  }

  public @Nullable Integer getSmallIcon() {
    if (!mNotificationAndroidBundle.containsKey("smallIcon")) {
      return null;
    }

    String rawIcon = mNotificationAndroidBundle.getString("smallIcon");
    int smallIconId = ResourceUtils.getImageResourceId(rawIcon);

    if (smallIconId == 0) {
      Logger.w(
          TAG,
          String.format(
              "Notification small icon '%s' could not be resolved; falling back to the app"
                  + " launcher icon. Common causes: asset only in src/debug/res, R8 resource"
                  + " shrinking, or name mismatch (names must be lowercase with underscores)."
                  + " See README Troubleshooting.",
              rawIcon));
      return null;
    }

    return smallIconId;
  }

  public @Nullable Integer getSmallIconLevel() {
    if (!mNotificationAndroidBundle.containsKey("smallIconLevel")) {
      return null;
    }

    return BundleValueReader.getIntPreserving(mNotificationAndroidBundle, "smallIconLevel");
  }

  public @Nullable String getSortKey() {
    return mNotificationAndroidBundle.getString("sortKey");
  }

  public Boolean hasStyle() {
    return mNotificationAndroidBundle.containsKey("style");
  }

  public @Nullable NotificationAndroidStyleModel getStyle() {
    if (!hasStyle()) {
      return null;
    }

    return NotificationAndroidStyleModel.fromBundle(mNotificationAndroidBundle.getBundle("style"));
  }

  public @Nullable String getTicker() {
    return mNotificationAndroidBundle.getString("ticker");
  }

  public @Nullable Long getTimeoutAfter() {
    if (mNotificationAndroidBundle.containsKey("timeoutAfter")) {
      return BundleValueReader.getLongPreserving(mNotificationAndroidBundle, "timeoutAfter");
    }

    return null;
  }

  public Boolean getShowChronometer() {
    return mNotificationAndroidBundle.getBoolean("showChronometer", false);
  }

  public @Nullable String getSound() {
    if (!mNotificationAndroidBundle.containsKey("sound")) {
      return null;
    }

    return mNotificationAndroidBundle.getString("sound");
  }

  @SuppressWarnings("unchecked")
  public long[] getVibrationPattern() {
    if (!mNotificationAndroidBundle.containsKey("vibrationPattern")) {
      return new long[0];
    }

    ArrayList<?> vibrationPattern =
        Objects.requireNonNull(
            BundleValueReader.getArrayListValue(mNotificationAndroidBundle, "vibrationPattern"));

    long[] vibrateArray = new long[vibrationPattern.size()];

    for (int i = 0; i < vibrationPattern.size(); i++) {
      Integer value = (Integer) vibrationPattern.get(i);
      vibrateArray[i] = value.longValue();
    }

    return vibrateArray;
  }

  public int getVisibility() {
    return BundleValueReader.getIntPreserving(
        mNotificationAndroidBundle, "visibility", NotificationCompat.VISIBILITY_PRIVATE);
  }

  public long getTimestamp() {
    return BundleValueReader.getLongPreserving(mNotificationAndroidBundle, "timestamp", -1L);
  }

  public static class AndroidProgress {
    int max;
    int current;
    boolean indeterminate;

    AndroidProgress(int max, int current, boolean indeterminate) {
      this.max = max;
      this.current = current;
      this.indeterminate = indeterminate;
    }

    public int getMax() {
      return max;
    }

    public int getCurrent() {
      return current;
    }

    public boolean getIndeterminate() {
      return indeterminate;
    }
  }
}
