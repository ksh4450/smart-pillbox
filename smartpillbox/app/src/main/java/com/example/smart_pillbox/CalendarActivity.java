package com.example.smart_pillbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {
    private RecyclerView calendarRecyclerView;
    private CalendarAdapter calendarAdapter;
    private List<AlarmInfo> alarmList;

    private BroadcastReceiver calendarUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.test3.CALENDAR_UPDATE".equals(intent.getAction())) {
                updateCalendarData();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        loadAlarms(); // alarmList를 로드하는 메서드 호출

        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        calendarRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Calendar> months = generateMonths();
        //calendarAdapter = new CalendarAdapter(months);
        calendarAdapter = new CalendarAdapter(months, alarmList);
        calendarRecyclerView.setAdapter(calendarAdapter);

        // "X" 버튼에 대한 클릭 리스너 추가
        ImageButton closeButton = findViewById(R.id.closeButton);

        closeButton.setOnClickListener(v -> finish());

        IntentFilter filter = new IntentFilter("com.example.test3.CALENDAR_UPDATE");
        registerReceiver(calendarUpdateReceiver, filter);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(calendarUpdateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAlarms(); // alarmList를 다시 로드합니다.
        if (calendarAdapter != null) {
            calendarAdapter.updateAllAlarms(alarmList);
            calendarAdapter.notifyDataSetChanged();
        }
    }

    private void updateCalendarData() {
        loadAlarms();
        if (calendarAdapter != null) {
            calendarAdapter.updateAlarmList(alarmList);
            calendarAdapter.notifyDataSetChanged();
        }
        Log.d("CalendarActivity", "Calendar data updated with " + alarmList.size() + " alarms");
    }

    private List<Calendar> generateMonths() {
        List<Calendar> months = new ArrayList<>();
        Calendar currentMonth = Calendar.getInstance();

        // Add two months before the current month
        for (int i = -2; i <= 1; i++) {
            Calendar month = (Calendar) currentMonth.clone();
            month.add(Calendar.MONTH, i);
            months.add(month);
        }

        return months;
    }

    private void loadAlarms() {
        SharedPreferences prefs = getSharedPreferences("Alarms", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("alarmList", null);
        if (json != null) {
            Type type = new TypeToken<List<AlarmInfo>>(){}.getType();
            alarmList = gson.fromJson(json, type);
        } else {
            alarmList = new ArrayList<>();
        }
        Log.d("CalendarActivity", "Loaded " + alarmList.size() + " alarms");
        for (AlarmInfo alarm : alarmList) {
            Log.d("CalendarActivity", "Alarm: " + alarm.medicineName + ", Time: " + alarm.time + ", Schedule: " + alarm.schedule);
        }
    }
}
