package com.example.looplab.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.looplab.R;
import com.example.looplab.data.model.Models;

public class EditUserDialog extends DialogFragment {

    private Models.UserProfile user;

    public static EditUserDialog newInstance(Models.UserProfile user) {
        EditUserDialog dialog = new EditUserDialog();
        Bundle args = new Bundle();
        args.putString("uid", user.uid);
        args.putString("name", user.name);
        args.putString("email", user.email);
        args.putString("role", user.role);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_LoopLab_Dialog);

        if (getArguments() != null) {
            user = new Models.UserProfile();
            user.uid = getArguments().getString("uid");
            user.name = getArguments().getString("name");
            user.email = getArguments().getString("email");
            user.role = getArguments().getString("role");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Redirect to AddEditUserActivity with existing user
        if (getActivity() != null && user != null) {
            android.content.Intent intent = new android.content.Intent(getActivity(), AddEditUserActivity.class);
            intent.putExtra("isEdit", true);
            intent.putExtra("userId", user.uid);
            startActivity(intent);
            dismiss();
        }
    }
}
