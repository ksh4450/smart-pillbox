package com.example.smart_pillbox;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {
    private Context context;
    private List<AlarmInfo> alarms;

    public AlarmAdapter(Context context, List<AlarmInfo> alarms) {
        this.context = context;
        this.alarms = alarms;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        AlarmInfo alarm = alarms.get(position);
        holder.bind(alarm);
    }

    @Override
    public int getItemCount() {
        return alarms.size();
    }

    class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlarmInfo;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        void bind(AlarmInfo alarm) {
            tvAlarmInfo.setText(String.format("%s - %s", alarm.medicineName, alarm.time));
        }
    }
}
