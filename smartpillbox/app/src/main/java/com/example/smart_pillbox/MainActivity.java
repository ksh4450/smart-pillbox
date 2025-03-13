package com.example.smart_pillbox;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import java.util.Calendar;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import android.Manifest;


import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_CONNECTION = 1;
    public static final String PREF_NAME = "BluetoothPrefs";
    public static final String LAST_CONNECTED_DEVICE = "LastConnectedDevice";
    private static final int REQUEST_ADD_ALARM = 1;
    private List<AlarmInfo> alarmList = new ArrayList<>();
    private BluetoothManager bluetoothManager;
    private ViewPager2 viewPager;
    private TextView tvCurrentDate;
    private DayPagerAdapter pagerAdapter;
    private List<Date> dates;
    private Button btnGoToToday;
    private static final int TOTAL_DAYS = 62;
    private int todayPosition;
    private ImageButton btnCalendar;
    private FrameLayout loadingOverlay;
    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    private static final String CHANNEL_ID = "MedicineAlarmChannel";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "onCreate called");

        loadingOverlay = findViewById(R.id.loadingOverlay);

        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        viewPager = findViewById(R.id.viewPager);
        btnGoToToday = findViewById(R.id.btnGoToToday);

        loadAlarms();
        //Log.d("MainActivity", "Loaded alarms count: " + alarmList.size());

        setupDates();
        setupViewPager();
        updateCurrentDate(dates.get(todayPosition));
        updateUI();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String lastConnectedDevice = prefs.getString(LAST_CONNECTED_DEVICE, null);
        Log.d(TAG, "Last connected device from SharedPreferences: " + lastConnectedDevice);

        // 블루투스 권한 확인
        boolean hasBluetoothPermission = checkBluetoothPermissions();
        Log.d("MainActivity", "Has Bluetooth permission: " + hasBluetoothPermission);

        bluetoothManager = BluetoothManager.getInstance(this);
        checkAlarmPermission();
        autoConnect();

        btnGoToToday.setOnClickListener(v -> goToToday());
        Button btnHome = findViewById(R.id.btnHome);
        Button btnSmartPillBox = findViewById(R.id.btnSmartPillBox);
        Button btnAddAlarm = findViewById(R.id.btnAddAlarm);

        btnAddAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddAlarmActivity.class);
            startActivityForResult(intent, REQUEST_ADD_ALARM);
        });

        //btnHome.setOnClickListener(v -> Toast.makeText(this, "홈 화면입니다", Toast.LENGTH_SHORT).show());

        btnSmartPillBox.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SmartPillBoxActivity.class);
            startActivity(intent);
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateCurrentDate(dates.get(position));
                viewPager.post(() -> {
                    loadAlarms();
                    pagerAdapter.updateAllAlarms(alarmList);
                    pagerAdapter.notifyItemChanged(position);
                });
            }
        });

        btnCalendar = findViewById(R.id.btnCalendar);
        btnCalendar.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e("MainActivity", "Error starting CalendarActivity", e);
                Toast.makeText(this, "달력을 열 수 없습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        Log.d("MainActivity", "onCreate completed");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "약 복용 알림",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("약 복용 시간을 알려주는 알림입니다.");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "알림 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 6.0 미만에서는 권한 체크가 필요 없습니다.
            return true;
        }
    }

    private void showLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingOverlay.setVisibility(View.GONE);
    }

    private void refreshAppWithLoadingEffect() {
        showLoading();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 데이터 새로고침
                loadAlarms();
                setupDates();
                setupViewPager();
                updateCurrentDate(dates.get(todayPosition));

                // UI 업데이트
                if (pagerAdapter != null) {
                    pagerAdapter.updateAllAlarms(alarmList);
                    pagerAdapter.notifyDataSetChanged();
                }
                viewPager.setCurrentItem(todayPosition, false);

                // 로딩 숨기기
                hideLoading();

                //Toast.makeText(MainActivity.this, "데이터가 새로고침되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }, 1500); // 1.5초 동안 로딩 효과 표시
    }

    private void setupDates() {
        dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 0);

        Date today = calendar.getTime();

        for (int i = 0; i < TOTAL_DAYS; i++) {
            dates.add(0, calendar.getTime());
            calendar.add(Calendar.DATE, -1);
        }
        todayPosition = dates.indexOf(today);
    }

    private void setupViewPager() {
        pagerAdapter = new DayPagerAdapter(this, dates, alarmList);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setCurrentItem(todayPosition, false);
        //Log.d("MainActivity", "ViewPager setup complete. Today position: " + todayPosition);
        pagerAdapter.setOnAlarmDeleteListener(new DayPagerAdapter.OnAlarmDeleteListener() {
            @Override
            public void onAlarmDelete(AlarmInfo alarm) {
                deleteAlarm(alarm);
                pagerAdapter.updateAllAlarms(alarmList);
            }
        });

        updateCurrentDate(dates.get(todayPosition));
    }

    private void goToToday() {
        viewPager.setCurrentItem(todayPosition, true);
    }

    private void updateCurrentDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.getDefault());
        tvCurrentDate.setText(sdf.format(date));
    }

    private void autoConnect() {
        Log.d("MainActivity", "autoConnect called");
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String lastConnectedDevice = prefs.getString(LAST_CONNECTED_DEVICE, null);

        Log.d("MainActivity", "Attempting to auto-connect to: " + lastConnectedDevice);

        if (lastConnectedDevice != null) {
            bluetoothManager.connect(lastConnectedDevice);
        } else {
            Log.d("MainActivity", "No last connected device found");
        }
    }

    /*private void updateUI() {
        pagerAdapter.updateAllAlarms(alarmList);
        pagerAdapter.notifyDataSetChanged();
        //Log.d("MainActivity", "UI updated and ViewPager refreshed");
    }*/
    private void updateUI() {
        runOnUiThread(() -> {
            loadAlarms();
            if (pagerAdapter != null) {
                pagerAdapter.updateAllAlarms(alarmList);
                pagerAdapter.notifyDataSetChanged();
            }
            updateCurrentDate(dates.get(viewPager.getCurrentItem()));
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_ALARM && resultCode == RESULT_OK && data != null) {
            String medicineName = data.getStringExtra("medicineName");
            String schedule = data.getStringExtra("schedule");
            String time = data.getStringExtra("time");
            String selectedDays = data.getStringExtra("selectedDays");

            if (medicineName != null && schedule != null && time != null) {
                AlarmInfo newAlarm = new AlarmInfo(medicineName, schedule, time, selectedDays);
                newAlarm.addedDate = new Date();

                alarmList.add(newAlarm);
                // Log.d("MainActivity", "Alarm added. Total alarms: " + alarmList.size());
                //Log.d("MainActivity", "New alarm details: " + newAlarm.medicineName + ", " + newAlarm.time + ", " + newAlarm.schedule);

                saveAlarms();
                setAlarm(newAlarm);

                // 브로드캐스트 전송
                Intent updateIntent = new Intent("com.example.test3.CALENDAR_UPDATE");
                sendBroadcast(updateIntent);

                Toast.makeText(this, "알람이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                refreshAppWithLoadingEffect();
            } else {
                Toast.makeText(this, "알람 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_BLUETOOTH_CONNECTION && resultCode == RESULT_OK && data != null) {
            String connectedDeviceAddress = data.getStringExtra("DEVICE_ADDRESS");
            Log.d(TAG, "Bluetooth connection successful. Device address: " + connectedDeviceAddress);

            SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
            editor.putString(LAST_CONNECTED_DEVICE, connectedDeviceAddress);
            editor.apply();

            Log.d(TAG, "Saved last connected device address: " + connectedDeviceAddress);

            bluetoothManager.connect(connectedDeviceAddress);

            Intent intent = new Intent(MainActivity.this, SmartPillBoxActivity.class);
            startActivity(intent);
        }
    }

    private void restartApp() {
        // 1. 현재 액티비티 종료
        finish();

        // 2. 앱 프로세스 종료하지 않고 액티비티만 다시 시작
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // 3. 액티비티 전환 애니메이션 제거 (선택사항)
        overridePendingTransition(0, 0);
    }

    private void checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    public void saveAlarms() {
        //Log.d("MainActivity", "Saving alarms");
        SharedPreferences prefs = getSharedPreferences("Alarms", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(alarmList);
        editor.putString("alarmList", json);
        editor.apply();
        //Log.d("MainActivity", "Alarms saved");

        loadAlarms();
    }

    public void updateAllPages() {
        //Log.d("MainActivity", "Loading alarms");
        loadAlarms();
        pagerAdapter.updateAllAlarms(alarmList);
        pagerAdapter.notifyDataSetChanged();
        Log.d("MainActivity", "Loaded " + alarmList.size() + " alarms");
    }

    public void loadAlarms() {
        //Log.d("MainActivity", "Loading alarms");
        SharedPreferences prefs = getSharedPreferences("Alarms", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("alarmList", null);
        if (json != null) {
            Type type = new TypeToken<List<AlarmInfo>>(){}.getType();
            alarmList = gson.fromJson(json, type);
            //Log.d("MainActivity", "Loaded " + alarmList.size() + " alarms");
        } else {
            alarmList = new ArrayList<>();
            //Log.d("MainActivity", "No alarms found, created new list");
        }
        Log.d("MainActivity", "Loaded " + alarmList.size() + " alarms");

        if (pagerAdapter != null) {
            //Log.d("MainActivity", "Updating pagerAdapter");
            pagerAdapter.updateAllAlarms(alarmList);
            pagerAdapter.notifyDataSetChanged();
        }
    }

    public List<AlarmInfo> getAlarmsForDate(Date date) {
        return filterAlarmsForDate(alarmList, date);
    }

    public void refreshRecyclerView() {
        if (pagerAdapter != null) {
            pagerAdapter.updateAllAlarms(alarmList);
            pagerAdapter.notifyDataSetChanged();
        }
    }

    private List<AlarmInfo> filterAlarmsForDate(List<AlarmInfo> allAlarms, Date date) {
        List<AlarmInfo> filteredAlarms = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        for (AlarmInfo alarm : allAlarms) {
            if (date.before(alarm.addedDate)) continue;
            if (isAlarmScheduledForDay(alarm, dayOfWeek)) {
                filteredAlarms.add(alarm);
            }
        }

        return filteredAlarms;
    }

    private boolean isAlarmScheduledForDay(AlarmInfo alarm, int dayOfWeek) {
        if (alarm.selectedDays != null && !alarm.selectedDays.isEmpty()) {
            return alarm.selectedDays.contains(String.valueOf(dayOfWeek));
        } else {
            return alarm.schedule.equals("매일") ||
                    (alarm.schedule.equals("주중") && dayOfWeek >= 1 && dayOfWeek <= 5) ||
                    (alarm.schedule.equals("주말") && (dayOfWeek == 0 || dayOfWeek == 6));
        }
    }

    private void setAlarm(AlarmInfo alarm) {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(this, AlarmReceiver.class);
            alarmIntent.putExtra("medicineName", alarm.medicineName);
            alarmIntent.putExtra("selectedDays", alarm.selectedDays);
            alarmIntent.putExtra("time", alarm.time);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarm.id, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Calendar firstAlarmTime = calculateNextAlarmTime(alarm.time, alarm.selectedDays);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, firstAlarmTime.getTimeInMillis(), pendingIntent);
                } else {
                    throw new SecurityException("Exact alarm permission not granted");
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, firstAlarmTime.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, firstAlarmTime.getTimeInMillis(), pendingIntent);
            }

            Toast.makeText(this, "알람이 성공적으로 설정되었습니다.", Toast.LENGTH_SHORT).show();
            Log.d("AlarmSet", "Alarm set for " + alarm.medicineName + " at " + firstAlarmTime.getTime() + " for days: " + alarm.selectedDays);
        } catch (SecurityException se) {
            Log.e("AlarmSet", "Security exception: " + se.getMessage());
            Toast.makeText(this, "정확한 알람 설정 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            checkAlarmPermission();
        } catch (Exception e) {
            Log.e("AlarmSet", "Error setting alarm: " + e.getMessage());
            Toast.makeText(this, "알람 설정 중 오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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

        while (!isSelectedDay(selectedDays, nextAlarm.get(Calendar.DAY_OF_WEEK) - 1)) {
            nextAlarm.add(Calendar.DAY_OF_YEAR, 1);
        }

        return nextAlarm;
    }

    private boolean isSelectedDay(String selectedDays, int day) {
        return selectedDays == null || selectedDays.isEmpty() || selectedDays.contains(String.valueOf(day));
    }

    private void deleteAlarm(AlarmInfo alarmToDelete) {
        alarmList.remove(alarmToDelete);
        saveAlarms();
        pagerAdapter.updateAllAlarms(alarmList);
        pagerAdapter.notifyDataSetChanged();
        Toast.makeText(this, "알람이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}