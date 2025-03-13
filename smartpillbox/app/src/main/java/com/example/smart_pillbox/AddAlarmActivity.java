package com.example.smart_pillbox;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.RadioButton;

import android.widget.TextView;

import android.util.Log;


public class AddAlarmActivity extends AppCompatActivity {
    private EditText etMedicineName;
    private RadioGroup rgSchedule;
    private TimePicker timePicker;
    private RadioButton rbDaily, rbWeekday, rbWeekend;
    private TextView[] dayViews;
    private boolean[] selectedDays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_alarm);

        // UI 요소 초기화
        etMedicineName = findViewById(R.id.etMedicineName);
        rgSchedule = findViewById(R.id.rgSchedule);
        timePicker = findViewById(R.id.timePicker);

        // 저장 버튼 설정
        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveAlarm());

        // 라디오 버튼 초기화
        rbDaily = findViewById(R.id.rbDaily);
        rbWeekday = findViewById(R.id.rbWeekday);
        rbWeekend = findViewById(R.id.rbWeekend);


        // 요일 뷰 초기화
        dayViews = new TextView[]{
                findViewById(R.id.tvSun), findViewById(R.id.tvMon), findViewById(R.id.tvTue),
                findViewById(R.id.tvWed), findViewById(R.id.tvThu), findViewById(R.id.tvFri),
                findViewById(R.id.tvSat)
        };
        selectedDays = new boolean[7];

        // 스케줄 라디오 그룹 리스너 설정
        RadioGroup rgSchedule = findViewById(R.id.rgSchedule);
        rgSchedule.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDaily) {
                setAllDays(true);
            } else if (checkedId == R.id.rbWeekday) {
                setWeekdays(true);
            } else if (checkedId == R.id.rbWeekend) {
                setWeekends(true);
            }
            updateRadioButtons();
        });


        for (int i = 0; i < dayViews.length; i++) {
            final int day = i;
            dayViews[i].setOnClickListener(v -> {
                toggleDay(day);
                updateRadioButtons();
            });
        }
    }

    // 라디오 버튼 상태 업데이트
    private void updateRadioButtons() {
        boolean allSelected = true;
        boolean onlyWeekdaysSelected = true;
        boolean onlyWeekendsSelected = true;


        // 선택된 요일 확인
        for (int i = 0; i < selectedDays.length; i++) {
            if (!selectedDays[i]) {
                allSelected = false;
            }
            if (i > 0 && i < 6 && !selectedDays[i]) {
                onlyWeekdaysSelected = false;
            }
            if ((i == 0 || i == 6) && !selectedDays[i]) {
                onlyWeekendsSelected = false;
            }
        }

        // 주말만 선택되었는지 확인 (일요일과 토요일만 선택)
        boolean onlyWeekendsSelectedStrict = selectedDays[0] && selectedDays[6] &&
                !selectedDays[1] && !selectedDays[2] && !selectedDays[3] && !selectedDays[4] && !selectedDays[5];

        // 주중만 선택되었는지 확인 (월요일부터 금요일까지만 선택)
        boolean onlyWeekdaysSelectedStrict = !selectedDays[0] && !selectedDays[6] &&
                selectedDays[1] && selectedDays[2] && selectedDays[3] && selectedDays[4] && selectedDays[5];

        // 라디오 버튼 리스너를 임시로 제거
        rgSchedule.setOnCheckedChangeListener(null);

        if (allSelected) {
            rbDaily.setChecked(true);
        } else if (onlyWeekdaysSelectedStrict) {
            rbWeekday.setChecked(true);
        } else if (onlyWeekendsSelectedStrict) {
            rbWeekend.setChecked(true);
        } else {
            rgSchedule.clearCheck();
        }

        // 라디오 버튼 리스너를 다시 설정
        rgSchedule.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDaily) {
                setAllDays(true);
            } else if (checkedId == R.id.rbWeekday) {
                setWeekdays(true);
            } else if (checkedId == R.id.rbWeekend) {
                setWeekends(true);
            }
            updateRadioButtons();
        });
    }


    // 알람 저장 메서드
    private void saveAlarm() {
        String medicineName = etMedicineName.getText().toString();
        if (medicineName.isEmpty()) {
            Toast.makeText(this, "약 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 스케줄 설정
        String schedule = "";
        int checkedId = rgSchedule.getCheckedRadioButtonId();
        if (checkedId == R.id.rbDaily) {
            schedule = "매일";
        } else if (checkedId == R.id.rbWeekday) {
            schedule = "주중";
        } else if (checkedId == R.id.rbWeekend) {
            schedule = "주말";
        }

        String selectedDaysStr = getSelectedDaysString();

        // 시간 설정
        String time = String.format("%02d:%02d", timePicker.getHour(), timePicker.getMinute());
        AlarmInfo alarmInfo = new AlarmInfo(medicineName, schedule, time, selectedDaysStr);

        // 결과 인텐트 생성
        Intent resultIntent = new Intent();
        resultIntent.putExtra("medicineName", alarmInfo.medicineName);
        resultIntent.putExtra("schedule", alarmInfo.schedule);
        resultIntent.putExtra("time", alarmInfo.time);
        resultIntent.putExtra("selectedDays", selectedDaysStr);
        setResult(RESULT_OK, resultIntent);


        Toast.makeText(this, "알람이 저장되었습니다.", Toast.LENGTH_SHORT).show();
        finish();

    }

    // 선택된 요일을 문자열로 반환
    private String getSelectedDaysString() {
        StringBuilder selectedDaysStr = new StringBuilder();
        for (int i = 0; i < selectedDays.length; i++) {
            if (selectedDays[i]) {
                selectedDaysStr.append(i).append(",");
            }
        }
        if (selectedDaysStr.length() > 0) {
            selectedDaysStr.setLength(selectedDaysStr.length() - 1); // 마지막 쉼표 제거
        }
        return selectedDaysStr.toString();
    }

    // 모든 요일 설정
    private void setAllDays(boolean selected) {
        Log.d("AddAlarmActivity", "setAllDays called with: " + selected);
        for (int i = 0; i < selectedDays.length; i++) {
            selectedDays[i] = selected;
            updateDayView(i);
            Log.d("AddAlarmActivity", "Day " + i + " set to: " + selected);
        }
        //updateRadioButtons();
    }

    // 평일 설정
    private void setWeekdays(boolean selected) {
        for (int i = 0; i < selectedDays.length; i++) {
            selectedDays[i] = (i > 0 && i < 6) ? selected : !selected;
            updateDayView(i);
        }
        updateRadioButtons();
    }
    // 주말 설정

    private void setWeekends(boolean selected) {
        for (int i = 0; i < selectedDays.length; i++) {
            selectedDays[i] = (i == 0 || i == 6) ? selected : !selected;
            updateDayView(i);
        }
        updateRadioButtons();
    }

    // 요일 토글
    private void toggleDay(int day) {
        selectedDays[day] = !selectedDays[day];
        updateDayView(day);

    }

    // 요일 뷰 업데이트
    private void updateDayView(int day) {
        dayViews[day].setBackgroundResource(selectedDays[day] ?
                R.drawable.circle_background_selected : R.drawable.circle_background);
    }
}
