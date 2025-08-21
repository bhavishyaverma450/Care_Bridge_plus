package com.example.carebridge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
    private List<String> dates;
    private OnDateSelectedListener listener;
    private int selectedPosition = -1;

    public interface OnDateSelectedListener {
        void onDateSelected(String date);
    }

    public CalendarAdapter(List<String> dates, OnDateSelectedListener listener) {
        this.dates = dates;
        this.listener = listener;
        // Default to current date
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
        selectedPosition = dates.indexOf(currentDate);
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_item, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        String date = dates.get(position);
        holder.dateText.setText(formatDate(date));
        holder.itemView.setBackgroundResource(
                position == selectedPosition ? R.drawable.pill_selected_background : R.drawable.pill_background
        );
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (selectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousPosition);
                notifyItemChanged(selectedPosition);
                listener.onDateSelected(date);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    private String formatDate(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd", Locale.US);
            return outputFormat.format(inputFormat.parse(date));
        } catch (Exception e) {
            return date;
        }
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;

        CalendarViewHolder(View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.date_text);
        }
    }
}