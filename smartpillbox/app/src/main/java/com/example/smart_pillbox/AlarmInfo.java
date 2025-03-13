package com.example.smart_pillbox;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Date;

public class AlarmInfo {
    public int id; // 알람의 고유 식별자
    public String medicineName; // 약 이름
    public String schedule; // 복용 일정 ("매일", "주중", "주말")
    public String time; // 복용 시간
    public boolean isTaken;  // 복용 여부
    public String takenTime; // 복용한 시간
    public String selectedDays; // 선택된 요일들 (주간 일정에 사용)
    public Map<String, String> takenDates = new HashMap<>(); // 날짜별 복용 여부를 저장하는 맵
    public Date addedDate; // 알람이 추가된 날짜
    public Map<Integer, Boolean> takenByDay; // 요일별 복용 상태 (0:일요일, 1:월요일, ..., 6:토요일)
    public Map<Integer, String> takenTimeByDay; // 요일별 복용 시간

    // 생성자
    public AlarmInfo(String medicineName, String schedule, String time, String selectedDays) {
        this.id = generateUniqueId();
        this.medicineName = medicineName;
        this.schedule = schedule;
        this.time = time;
        this.isTaken = false;
        this.takenTime = "";
        this.selectedDays = selectedDays;
        this.takenDates = new HashMap<>();
        this.addedDate = new Date(); // 알람 추가 시 현재 날짜 저장

        this.takenByDay = new HashMap<>();
        this.takenTimeByDay = new HashMap<>();
        // 모든 요일에 대해 초기 복용 상태와 시간 설정
        for (int i = 0; i < 7; i++) {
            takenByDay.put(i, false);
            takenTimeByDay.put(i, "");
        }
    }

    // 고유 ID 생성 메서드
    private int generateUniqueId() {
        // 현재 시간을 밀리초 단위로 사용하여 간단한 고유 ID 생성
        return (int) System.currentTimeMillis();
    }
    // 특정 요일의 복용 상태와 시간 설정 메서드
    public void setTakenForDate(String date, String takenTime) {
        if (date == null || takenTime == null) {
            Log.e("AlarmInfo", "Attempting to set null date or time");
            return;
        }
        takenDates.put(date, takenTime);
        //Log.d("AlarmInfo", "Set taken for date: " + date + " at time: " + takenTime);
    }

    // 특정 요일의 복용 여부 확인 메서드
    public boolean isTakenForDate(String date) {
        if (date == null) {
            Log.e("AlarmInfo", "Checking taken status for null date");
            return false;
        }
        boolean isTaken = takenDates.containsKey(date);
        //Log.d("AlarmInfo", "Checking if taken for date: " + date + ", result: " + isTaken);
        return isTaken;
    }
    public String getTakenTimeForDate(String date) {
        return takenDates.get(date);
    }

    // 객체 비교를 위한 equals 메서드 오버라이드
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlarmInfo alarmInfo = (AlarmInfo) o;
        return id == alarmInfo.id &&
                Objects.equals(medicineName, alarmInfo.medicineName) &&
                Objects.equals(time, alarmInfo.time) &&
                Objects.equals(schedule, alarmInfo.schedule) &&
                Objects.equals(takenDates, alarmInfo.takenDates);
    }

    // 해시 코드 생성을 위한 hashCode 메서드 오버라이드
    @Override
    public int hashCode() {
        return Objects.hash(id, medicineName, time, schedule, takenDates);
    }
}
