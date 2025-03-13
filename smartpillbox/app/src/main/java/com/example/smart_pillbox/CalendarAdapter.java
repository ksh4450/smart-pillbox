package com.example.smart_pillbox;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.text.ParseException;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.MonthViewHolder> {
    private List<Calendar> months;
    private List<AlarmInfo> alarmList;
    private Map<String, Boolean> dateCompletionStatus;

    public CalendarAdapter(List<Calendar> months, List<AlarmInfo> alarmList) {
        this.months = months;
        this.alarmList = alarmList;
        this.dateCompletionStatus = new HashMap<>();
        updateDateCompletionStatus();
        Log.d("CalendarAdapter", "Constructor called, alarmList size: " + alarmList.size());
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.month_view, parent, false);
        return new MonthViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        Calendar month = months.get(position);
        holder.bind(month);
    }

    @Override
    public int getItemCount() {
        return months.size();
    }



    public void updateAlarmList(List<AlarmInfo> newAlarmList) {
        this.alarmList = newAlarmList;
        updateDateCompletionStatus();
        Log.d("CalendarAdapter", "Alarm list updated with " + alarmList.size() + " alarms");
        for (AlarmInfo alarm : alarmList) {
            Log.d("CalendarAdapter", "Alarm: " + alarm.medicineName + ", Time: " + alarm.time + ", Schedule: " + alarm.schedule);
        }
        notifyDataSetChanged();
    }

    public void updateAllAlarms(List<AlarmInfo> newAlarmList) {
        this.alarmList = newAlarmList;
        updateDateCompletionStatus();
        notifyDataSetChanged();
    }


    private void updateDateCompletionStatus() {
        dateCompletionStatus.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar startDate = (Calendar) months.get(0).clone();
        startDate.set(Calendar.DAY_OF_MONTH, 1);
        Calendar endDate = (Calendar) months.get(months.size() - 1).clone();
        endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH));

        Calendar current = (Calendar) startDate.clone();
        while (!current.after(endDate)) {
            String date = sdf.format(current.getTime());
            int completionStatus = getCompletionStatus(date);
            dateCompletionStatus.put(date, completionStatus == 3);
            Log.d("CalendarAdapter", "Date: " + date + ", CompletionStatus: " + completionStatus);
            current.add(Calendar.DAY_OF_MONTH, 1);
        }
    }


    private int getScheduledAlarmsCountForDate(String date) {
        int count = 0;
        for (AlarmInfo alarm : alarmList) {
            if (isAlarmScheduledForDate(alarm, date)) {
                count++;
            }
        }
        return count;
    }


    private boolean checkAllAlarmsTakenForDate(String date) {
        int scheduledCount = 0;
        int takenCount = 0;
        Date currentDate = null;
        try {
            currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }

        for (AlarmInfo alarm : alarmList) {
            if (isAlarmScheduledForDate(alarm, date) && !currentDate.before(alarm.addedDate)) {
                scheduledCount++;
                if (alarm.isTakenForDate(date)) {
                    takenCount++;
                }
            }
        }

        Log.d("CalendarAdapter", "Date: " + date + ", Scheduled: " + scheduledCount + ", Taken: " + takenCount);
        return scheduledCount > 0 && takenCount == scheduledCount;
    }

    private int getCompletionStatus(String date) {
        int scheduledCount = 0;
        int takenCount = 0;
        Date currentDate = null;
        try {
            currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }

        for (AlarmInfo alarm : alarmList) {
            if (isAlarmScheduledForDate(alarm, date)) {
                // Remove the check for addedDate here
                scheduledCount++;
                if (alarm.isTakenForDate(date)) {
                    takenCount++;
                }
            }
        }

        Log.d("CalendarAdapter", "Date: " + date + ", Scheduled: " + scheduledCount + ", Taken: " + takenCount);
        if (scheduledCount == 0) return 0;
        if (takenCount == 0) return 1;
        if (takenCount == scheduledCount) return 3;
        return 2;
    }

    private boolean isAlarmScheduledForDate(AlarmInfo alarm, String date) {
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        if (alarm.selectedDays != null && !alarm.selectedDays.isEmpty()) {
            return alarm.selectedDays.contains(String.valueOf(dayOfWeek));
        } else {
            return alarm.schedule.equals("매일") ||
                    (alarm.schedule.equals("주중") && dayOfWeek >= 1 && dayOfWeek <= 5) ||
                    (alarm.schedule.equals("주말") && (dayOfWeek == 0 || dayOfWeek == 6));
        }
    }



    class MonthViewHolder extends RecyclerView.ViewHolder {
        TextView monthYearText;
        GridLayout calendarGrid;

        public MonthViewHolder(@NonNull View itemView) {
            super(itemView);
            monthYearText = itemView.findViewById(R.id.monthYearText);
            calendarGrid = itemView.findViewById(R.id.calendarGrid);
        }

        void bind(Calendar month) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MMMM", Locale.getDefault());
            monthYearText.setText(sdf.format(month.getTime()));

            Calendar currentMonth = Calendar.getInstance();
            if (month.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
                    month.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)) {
                monthYearText.setTextColor(Color.BLUE);
            } else {
                monthYearText.setTextColor(Color.BLACK);
            }

            int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int day = 1; day <= daysInMonth; day++) {
                Calendar date = (Calendar) month.clone();
                date.set(Calendar.DAY_OF_MONTH, day);
                String formattedDate = formatDate(date.getTime());

                boolean allTaken = dateCompletionStatus.getOrDefault(formattedDate, false);
                boolean isToday = isToday(date);
                addDateView(String.valueOf(day), allTaken, isToday);
            }




            createCalendarGrid(month);
        }


        void createCalendarGrid(Calendar month) {
            calendarGrid.removeAllViews();

            String[] weekdays = {"일", "월", "화", "수", "목", "금", "토"};
            for (String day : weekdays) {
                addHeaderView(day);
            }

            int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);

            // 월의 첫 날짜로 설정
            month.set(Calendar.DAY_OF_MONTH, 1);

            // 첫 날의 요일을 가져옴 (1: 일요일, 2: 월요일, ..., 7: 토요일)
            int firstDayOfWeek = month.get(Calendar.DAY_OF_WEEK);

            // 빈 셀 추가 (이전 달의 날짜)
            for (int i = 1; i < firstDayOfWeek; i++) {
                addDateView("", false, false);
            }

            // 이번 달의 날짜 추가
            for (int day = 1; day <= daysInMonth; day++) {
                Calendar date = (Calendar) month.clone();
                date.set(Calendar.DAY_OF_MONTH, day);
                String formattedDate = formatDate(date.getTime());

                boolean allTaken = dateCompletionStatus.getOrDefault(formattedDate, false);
                boolean isToday = isToday(date);
                addDateView(String.valueOf(day), allTaken, isToday);
            }

            // 필요한 경우 다음 달의 날짜로 나머지 셀 채우기
            int fillerDays = 42 - (firstDayOfWeek - 1 + daysInMonth); // 6주 * 7일 = 42
            for (int i = 0; i < fillerDays; i++) {
                addDateView("", false, false);
            }
        }

        private void addHeaderView(String text) {
            TextView headerView = new TextView(itemView.getContext());
            headerView.setText(text);
            headerView.setGravity(Gravity.CENTER);
            headerView.setTextColor(Color.BLACK);
            headerView.setTypeface(null, Typeface.BOLD);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            headerView.setLayoutParams(params);

            calendarGrid.addView(headerView);
        }

        private void addDateView(String text, boolean allTaken, boolean isToday) {
            Log.d("CalendarAdapter", "Date: " + text + ", allTaken: " + allTaken);
            FrameLayout dateContainer = new FrameLayout(itemView.getContext());

            TextView dateView = new TextView(itemView.getContext());
            dateView.setText(text);
            dateView.setGravity(Gravity.CENTER);
            dateView.setTextColor(isToday ? Color.BLUE : Color.BLACK);
            dateContainer.addView(dateView);

            if (!text.isEmpty()) {
                View circleView = new View(itemView.getContext());
                int circleSize = dpToPx(30);
                FrameLayout.LayoutParams circleParams = new FrameLayout.LayoutParams(circleSize, circleSize);
                circleParams.gravity = Gravity.CENTER;
                circleView.setLayoutParams(circleParams);

                GradientDrawable circleDrawable = new GradientDrawable();
                circleDrawable.setShape(GradientDrawable.OVAL);
                circleDrawable.setColor(allTaken ? Color.GREEN : Color.TRANSPARENT);
                circleDrawable.setStroke(dpToPx(1), isToday ? Color.BLUE : Color.BLACK);
                circleView.setBackground(circleDrawable);

                dateContainer.addView(circleView);

                if (allTaken) {
                    ImageView checkView = new ImageView(itemView.getContext());
                    checkView.setImageResource(R.drawable.ic_check);
                    int checkSize = dpToPx(20);
                    FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(checkSize, checkSize);
                    checkParams.gravity = Gravity.CENTER;
                    checkView.setLayoutParams(checkParams);
                    dateContainer.addView(checkView);
                }
            }

            GridLayout.LayoutParams containerParams = new GridLayout.LayoutParams();
            containerParams.width = 0;
            containerParams.height = dpToPx(40);
            containerParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            dateContainer.setLayoutParams(containerParams);

            calendarGrid.addView(dateContainer);
        }

        private int dpToPx(int dp) {
            float density = itemView.getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }

        private String formatDate(Date date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.format(date);
        }

        private boolean isToday(Calendar date) {
            Calendar today = Calendar.getInstance();
            return date.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    date.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    date.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);
        }
    }
}
