package com.example.smart_pillbox;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.widget.Button;
import android.app.AlertDialog;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DayPagerAdapter extends RecyclerView.Adapter<DayPagerAdapter.DayViewHolder> {
    private Context context;
    private List<Date> dates;
    private List<List<AlarmInfo>> alarmsByDay;
    private OnAlarmDeleteListener onAlarmDeleteListener;
    private MainActivity mainActivity;
    private List<AlarmInfo> alarmList;
    private Map<Integer, WeekStatus> weekStatusMap = new HashMap<>();

    public interface OnAlarmDeleteListener {
        void onAlarmDelete(AlarmInfo alarm);
    }

    public void setOnAlarmDeleteListener(OnAlarmDeleteListener listener) {
        this.onAlarmDeleteListener = listener;
    }

    public DayPagerAdapter(MainActivity mainActivity, List<Date> dates, List<AlarmInfo> allAlarms) {
        this.context = mainActivity;
        this.mainActivity = mainActivity;
        this.dates = dates;
        this.alarmList = allAlarms;
        this.alarmsByDay = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            this.alarmsByDay.add(new ArrayList<>());
        }
        updateAllAlarms(allAlarms);
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        Date date = dates.get(position);
        List<AlarmInfo> alarms = alarmsByDay.get(position);
        //Log.d("DayPagerAdapter", "Binding view for position " + position + " with " + alarms.size() + " alarms");
        WeekStatus weekStatus = getOrCreateWeekStatus(date);
        holder.bind(date, alarms, weekStatus);
    }

    private WeekStatus getOrCreateWeekStatus(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        int key = year * 100 + weekOfYear;

        if (!weekStatusMap.containsKey(key)) {
            weekStatusMap.put(key, new WeekStatus());
        }
        return weekStatusMap.get(key);
    }

    public class WeekStatus {
        public int[] takenCounts = new int[7];
        public int[] totalCounts = new int[7];

        public void updateStatus(int dayOfWeek, int taken, int total) {
            takenCounts[dayOfWeek] = taken;
            totalCounts[dayOfWeek] = total;
        }
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    public void updateAllAlarms(List<AlarmInfo> allAlarms) {
        if (allAlarms == null) {
            Log.w("DayPagerAdapter", "Received null allAlarms list");
            return;
        }
        this.alarmList = new ArrayList<>(allAlarms);
        for (int i = 0; i < dates.size(); i++) {
            Date date = dates.get(i);
            List<AlarmInfo> alarmsForDay = filterAlarmsForDate(allAlarms, date);
            alarmsByDay.set(i, alarmsForDay);
        }
        //Log.d("DayPagerAdapter", "Updated all alarms. Total alarms: " + alarmList.size());
    }

    private List<AlarmInfo> filterAlarmsForDate(List<AlarmInfo> allAlarms, Date date) {
        List<AlarmInfo> filteredAlarms = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        for (AlarmInfo alarm : allAlarms) {
            if (!date.before(alarm.addedDate) && isAlarmScheduledForDay(alarm, dayOfWeek)) {
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

    class DayViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llAlarms;
        LinearLayout llWeekView;
        TextView[] tvDays = new TextView[7];
        ProgressBar[] progressCircles = new ProgressBar[7];
        ImageView[] ivChecks = new ImageView[7];

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            llAlarms = itemView.findViewById(R.id.llAlarms);
            llWeekView = itemView.findViewById(R.id.llWeekView);

            for (int i = 0; i < 7; i++) {
                int dayId = itemView.getResources().getIdentifier("tvDay" + (i + 1), "id", itemView.getContext().getPackageName());
                int progressId = itemView.getResources().getIdentifier("progressCircle" + (i + 1), "id", itemView.getContext().getPackageName());
                int checkId = itemView.getResources().getIdentifier("ivCheck" + (i + 1), "id", itemView.getContext().getPackageName());

                tvDays[i] = itemView.findViewById(dayId);
                progressCircles[i] = itemView.findViewById(progressId);
                ivChecks[i] = itemView.findViewById(checkId);
            }
        }

        void bind(Date date, List<AlarmInfo> alarms, WeekStatus weekStatus) {
            //Log.d("DayViewHolder", "Binding for date: " + date + " with " + alarms.size() + " alarms");
            llAlarms.removeAllViews();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;

            Calendar weekStart = (Calendar) calendar.clone();
            weekStart.add(Calendar.DAY_OF_WEEK, -currentDayOfWeek);

            String[] daysOfWeek = {"일", "월", "화", "수", "목", "금", "토"};
            Collections.sort(alarms, (a1, a2) -> a1.time.compareTo(a2.time));

            for (int i = 0; i < 7; i++) {
                Calendar dayOfWeek = (Calendar) weekStart.clone();
                dayOfWeek.add(Calendar.DAY_OF_WEEK, i);
                String formattedDate = formatDate(dayOfWeek.getTime());

                int takenCount = 0;
                int totalCount = 0;

                for (AlarmInfo alarm : alarmList) {
                    if (isAlarmScheduledForDay(alarm, i) && !dayOfWeek.getTime().before(alarm.addedDate)) {
                        totalCount++;
                        if (alarm.isTakenForDate(formattedDate)) {
                            takenCount++;
                        }
                    }
                }

                weekStatus.updateStatus(i, takenCount, totalCount);
            }

            for (int i = 0; i < 7; i++) {
                tvDays[i].setText(daysOfWeek[i]);
                tvDays[i].setTextColor(i == currentDayOfWeek ? Color.BLUE : Color.BLACK);
                updateProgressAndCheck(i, weekStatus.takenCounts[i], weekStatus.totalCounts[i]);
            }


            for (AlarmInfo alarm : alarms) {
                if (!date.before(alarm.addedDate)) {
                    addAlarmView(alarm, date);
                    //Log.d("DayViewHolder", "Added alarm view for " + alarm.medicineName);
                }
            }

            updateWeekView(date, alarms);
        }

        private void addAlarmView(AlarmInfo alarm, Date date) {
            //Log.d("DayViewHolder", "Adding alarm view for " + alarm.medicineName);
            View alarmView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.item_alarm, llAlarms, false);
            TextView tvTime = alarmView.findViewById(R.id.tvTime);
            TextView tvMedicineName = alarmView.findViewById(R.id.tvMedicineName);
            Button btnTaken = alarmView.findViewById(R.id.btnTaken);

            tvTime.setText(alarm.time);
            tvMedicineName.setText(alarm.medicineName);

            updateAlarmViewState(alarmView, alarm, date);

            btnTaken.setOnClickListener(v -> {
                //Log.d("DayViewHolder", "Taken button clicked for " + alarm.medicineName);
                String formattedDate = formatDate(date);
                if (!alarm.isTakenForDate(formattedDate)) {
                    //Log.d("DayViewHolder", "Marking as taken");
                    markAsTaken(alarm, date);
                } else {
                    //Log.d("DayViewHolder", "Showing options dialog");
                    showOptionsDialog(alarm, alarmView, date);
                }
                //Log.d("DayViewHolder", "Updating alarm view state");
                updateAlarmViewState(alarmView, alarm, date);
                //Log.d("DayViewHolder", "Updating week view");
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    updateWeekView(date, alarmsByDay.get(position));
                    // Log.d("DayViewHolder", "Notifying item changed");
                    notifyItemChanged(position);
                } else {
                    //Log.w("DayViewHolder", "Invalid adapter position, skipping update");
                }
            });

            llAlarms.addView(alarmView);
            //Log.d("DayViewHolder", "Alarm view added");
        }

        private void updateProgressAndCheck(int dayIndex, int takenCount, int totalCount) {
            if (totalCount > 0) {
                int progress = (int) ((float) takenCount / totalCount * 100);
                progressCircles[dayIndex].setProgress(progress);
                if (takenCount == totalCount) {
                    ivChecks[dayIndex].setVisibility(View.VISIBLE);
                } else {
                    ivChecks[dayIndex].setVisibility(View.GONE);
                }
            } else {
                progressCircles[dayIndex].setProgress(0);
                ivChecks[dayIndex].setVisibility(View.GONE);
            }
        }

        private void updateWeekView(Date date, List<AlarmInfo> alarms) {
            if (date == null) {
                Log.w("DayViewHolder", "Date is null, skipping week view update");
                return;
            }
            if (alarms == null) {
                Log.w("DayViewHolder", "Alarms list is null, skipping week view update");
                return;
            }
            //Log.d("DayViewHolder", "Updating week view for date: " + date + " with " + alarms.size() + " alarms");

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;

            Calendar weekStart = (Calendar) calendar.clone();
            weekStart.add(Calendar.DAY_OF_WEEK, -currentDayOfWeek);

            for (int i = 0; i < 7; i++) {
                Calendar dayOfWeek = (Calendar) weekStart.clone();
                dayOfWeek.add(Calendar.DAY_OF_WEEK, i);
                String formattedDate = formatDate(dayOfWeek.getTime());

                int takenCount = 0;
                int totalCount = 0;

                for (AlarmInfo alarm : alarmList) {
                    if (isAlarmScheduledForDay(alarm, i) && !dayOfWeek.getTime().before(alarm.addedDate)) {
                        totalCount++;
                        if (alarm.isTakenForDate(formattedDate)) {
                            takenCount++;
                        }
                    }
                }

                updateProgressAndCheck(i, takenCount, totalCount);
            }
        }

        private void updateAlarmViewState(View alarmView, AlarmInfo alarm, Date date) {
            Button btnTaken = alarmView.findViewById(R.id.btnTaken);
            String formattedDate = formatDate(date);
            if (alarm.isTakenForDate(formattedDate)) {
                alarmView.setBackgroundColor(Color.GREEN);
                btnTaken.setText(alarm.getTakenTimeForDate(formattedDate) + "\n먹었습니다");
                btnTaken.setBackgroundColor(Color.GREEN);
                btnTaken.setTextColor(Color.BLACK);
            } else {
                alarmView.setBackgroundColor(Color.WHITE);
                btnTaken.setText("먹었어요");
                btnTaken.setBackgroundColor(Color.LTGRAY);
                btnTaken.setTextColor(Color.BLACK);
            }
        }

        private void showOptionsDialog(AlarmInfo alarm, View alarmView, Date date) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            builder.setItems(new CharSequence[]{"삭제", "안 먹었어요"}, (dialog, which) -> {
                if (which == 0) {
                    showDeleteConfirmationDialog(alarm);
                } else {
                    String formattedDate = formatDate(date);
                    alarm.takenDates.remove(formattedDate);
                    updateAlarmViewState(alarmView, alarm, date);
                    mainActivity.saveAlarms();
                }
                // RecyclerView의 position에 의존하지 않고 직접 updateWeekView를 호출
                updateWeekView(date, mainActivity.getAlarmsForDate(date));
                // 전체 RecyclerView를 갱신
                mainActivity.refreshRecyclerView();
            });
            builder.show();
        }

        private void showDeleteConfirmationDialog(AlarmInfo alarm) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            builder.setTitle("알람 삭제");
            builder.setMessage("이 알람을 삭제하시겠습니까?");
            builder.setPositiveButton("삭제", (dialog, which) -> {
                deleteAlarm(alarm);
            });
            builder.setNegativeButton("취소", null);
            builder.show();
        }


        private void markAsTaken(AlarmInfo alarm, Date date) {
            try {
                //Log.d("DayViewHolder", "Marking as taken: " + alarm.medicineName);
                String formattedDate = formatDate(date);
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String takenTime = sdf.format(new Date());
                alarm.setTakenForDate(formattedDate, takenTime);
                //Log.d("DayViewHolder", "Alarm marked as taken, saving alarms");
                mainActivity.saveAlarms();
                //Log.d("DayViewHolder", "Alarms saved, updating week view");
                updateWeekView(date, mainActivity.getAlarmsForDate(date));
                mainActivity.refreshRecyclerView();
            } catch (Exception e) {
                Log.e("DayViewHolder", "Error in markAsTaken", e);
                Toast.makeText(itemView.getContext(), "알람 상태 업데이트 중 오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }


        private void deleteAlarm(AlarmInfo alarm) {
            if (onAlarmDeleteListener != null) {
                onAlarmDeleteListener.onAlarmDelete(alarm);
            }
            mainActivity.refreshRecyclerView();
        }
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(date);
    }
}
