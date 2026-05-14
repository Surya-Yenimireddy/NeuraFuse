package com.example.neurafuse;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

    private final List<ApplicationInfo> appList;
    private final PackageManager pm;
    private final Set<String> selectedApps;

    public AppListAdapter(List<ApplicationInfo> appList, PackageManager pm, Set<String> selectedApps) {
        this.appList = appList;
        this.pm = pm;
        this.selectedApps = selectedApps;
    }

    @NonNull
    @Override
    public AppListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppListAdapter.ViewHolder holder, int position) {
        ApplicationInfo appInfo = appList.get(position);
        Drawable icon = pm.getApplicationIcon(appInfo);
        String label = pm.getApplicationLabel(appInfo).toString();

        holder.appName.setText(label);
        holder.appIcon.setImageDrawable(icon);
        holder.checkBox.setChecked(selectedApps.contains(appInfo.packageName));

        holder.itemView.setOnClickListener(v -> {
            boolean newState = !holder.checkBox.isChecked();
            holder.checkBox.setChecked(newState);
            if (newState)
                selectedApps.add(appInfo.packageName);
            else
                selectedApps.remove(appInfo.packageName);
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}
