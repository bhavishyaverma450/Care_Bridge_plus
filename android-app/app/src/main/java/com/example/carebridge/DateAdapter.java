package com.example.carebridge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.DateViewHolder> {

    public interface OnDateClickListener {
        void onDateClick(int position);
    }

    private final List<CalendarDate> dateList;
    private final OnDateClickListener listener;
    private final int todayPosition;
    private int selectedPosition = -1;

    public DateAdapter(List<CalendarDate> dateList, OnDateClickListener listener, int todayPosition) {
        this.dateList = dateList;
        this.listener = listener;
        this.todayPosition = todayPosition;
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        CalendarDate date = dateList.get(position);
        holder.dayText.setText(date.getDay());
        holder.dateText.setText(date.getDate());

        if (position == todayPosition) {
            holder.itemView.setBackgroundResource(R.drawable.today_highlight); // you should define this drawable
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                selectedPosition = position;
                notifyDataSetChanged();
                listener.onDateClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView dayText, dateText;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.dayText);
            dateText = itemView.findViewById(R.id.dateText);
        }
    }
}
