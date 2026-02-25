package com.launcher.mcpelauncher.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.launcher.mcpelauncher.R;

public class InfoFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);

        TextView tvInfo = view.findViewById(R.id.tv_info);

        String infoText = "MCPE Launcher v1.0.0\n\n" +
                "Features:\n" +
                "• Browse Minecraft Bedrock versions\n" +
                "• Download APKs from trusted sources\n" +
                "• Edit player name in options.txt\n" +
                "• Direct launch Minecraft PE\n\n" +
                "Supported Sources:\n" +
                "• mcpedl.org\n" +
                "• mcpe-planet.com\n\n" +
                "Note: This app requires storage permissions to save APK files.";

        tvInfo.setText(infoText);

        return view;
    }
}