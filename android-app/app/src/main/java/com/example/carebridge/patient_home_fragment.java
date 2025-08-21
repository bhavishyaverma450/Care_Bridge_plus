package com.example.carebridge;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class patient_home_fragment extends Fragment {

    private Button bookAppointmentBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.patient_home_fragment, container, false);

        bookAppointmentBtn = view.findViewById(R.id.bookAppointmentBtn);

        bookAppointmentBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), BookAppointmentActivity.class);
            startActivity(intent);
        });

        return view;
    }
}
