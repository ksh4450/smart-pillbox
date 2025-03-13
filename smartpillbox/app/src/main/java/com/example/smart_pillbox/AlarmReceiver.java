package com.example.smart_pillbox;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import androidx.core.content.ContextCompat;
import android.app.AlarmManager;
import android.app.PendingIntent;


import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.Manifest;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "MedicineAlarmChannel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String medicineName = intent.getStringExtra("medicineName");
            String selectedDays = intent.getStringExtra("selectedDays");

            // 현재 요일 확인 (0: 일요일, 1: 월요일, ..., 6: 토요일)
            Calendar calendar = Calendar.getInstance();
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;

            // 선택된 요일 확인
            if (selectedDays == null || selectedDays.contains(String.valueOf(dayOfWeek))) {
                Log.d("AlarmReceiver", "Alarm received for " + medicineName);

                // 알림 생성 및 표시
                createNotification(context, medicineName);

                // 진동
                vibrate(context);
            } else {
                Log.d("AlarmReceiver", "Today is not a selected day for this alarm");
            }

            // 다음 알람 설정
            setNextAlarm(context, intent, medicineName, selectedDays);
        } catch (Exception e) {
            Log.e("AlarmReceiver", "Error in AlarmReceiver", e);
        }
    }
    private boolean isSelectedDay(String selectedDays, int todayDay) {
        return selectedDays.contains(String.valueOf(todayDay));
    }
    private void createNotification(Context context, String medicineName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("약 복용 시간")
                .setContentText(medicineName + " 복용 시간입니다.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // 큰 텍스트 스타일 적용
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(medicineName + " 복용 시간입니다.\n약을 복용해주세요.")
                        .setBigContentTitle("약 복용 알림")
                        .setSummaryText("복용 알림"))
                // 진동 패턴 설정
                .setVibrate(new long[]{0, 1000, 500, 1000})
                // 알림음 설정
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                // LED 설정
                .setLights(Color.BLUE, 1000, 500);

        // 알림을 탭했을 때 실행될 Intent 설정
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } else {
            Log.w("AlarmReceiver", "Notification permission not granted");
        }
    }

    private void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(1000);
            }
        }
    }

    private void setNextAlarm(Context context, Intent intent, String medicineName, String selectedDays) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 다음 알람 시간 계산
        Calendar nextAlarmTime = calculateNextAlarmTime(intent.getStringExtra("time"), selectedDays);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmTime.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmTime.getTimeInMillis(), pendingIntent);
        }

        Log.d("AlarmReceiver", "Next alarm set for " + medicineName + " at " + nextAlarmTime.getTime());
    }

    private Calendar calculateNextAlarmTime(String time, String selectedDays) {
        Calendar now = Calendar.getInstance();
        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        Calendar nextAlarm = Calendar.getInstance();
        nextAlarm.set(Calendar.HOUR_OF_DAY, hour);
        nextAlarm.set(Calendar.MINUTE, minute);
        nextAlarm.set(Calendar.SECOND, 0);
        nextAlarm.set(Calendar.MILLISECOND, 0);

        if (nextAlarm.before(now)) {
            nextAlarm.add(Calendar.DAY_OF_YEAR, 1);
        }

        // 선택된 요일 중 가장 가까운 날짜 찾기
        while (!isSelectedDay(selectedDays, nextAlarm.get(Calendar.DAY_OF_WEEK) - 1)) {
            nextAlarm.add(Calendar.DAY_OF_YEAR, 1);
        }

        return nextAlarm;
    }
}
