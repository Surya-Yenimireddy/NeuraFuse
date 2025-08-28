package com.example.NeuraFuse.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.NeuraFuse.R;
import com.example.NeuraFuse.utils.AppLabelHelper;

import java.util.List;
import java.util.Map;

public class AppUsageAdapter extends RecyclerView.Adapter<AppUsageAdapter.ViewHolder> {
    private final List<Map.Entry<String, Long>> usageList;
    private final Context context;

    public AppUsageAdapter(Context context, List<Map.Entry<String, Long>> usageList) {
        this.context = context;
        this.usageList = usageList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView appName, usageTime;
        ImageView appIcon;

        public ViewHolder(View view) {
            super(view);
            appName = view.findViewById(R.id.txtAppName);
            usageTime = view.findViewById(R.id.txtAppUsage);
            appIcon = view.findViewById(R.id.imgAppIcon);
        }
    }

    @Override
    public AppUsageAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_usage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Map.Entry<String, Long> entry = usageList.get(position);
        String label = AppLabelHelper.getAppLabel(context, entry.getKey());
        long time = entry.getValue();

        long totalMinutes = time / 1000 / 60;
        long hours = totalMinutes / 60;
        long mins = totalMinutes % 60;

        holder.appName.setText(label);
        holder.usageTime.setText(hours + "h " + mins + "m");
        holder.appIcon.setImageDrawable(AppLabelHelper.getAppIcon(context, entry.getKey()));
    }

    @Override
    public int getItemCount() {
        return usageList.size();
    }
}
