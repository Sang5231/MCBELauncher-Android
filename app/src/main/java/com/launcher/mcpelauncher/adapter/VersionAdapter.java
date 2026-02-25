package com.launcher.mcpelauncher.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.launcher.mcpelauncher.R;
import com.launcher.mcpelauncher.model.VersionItem;
import java.util.List;

public class VersionAdapter extends RecyclerView.Adapter<VersionAdapter.ViewHolder> {

    private List<VersionItem> versionList;
    private OnVersionClickListener listener;

    public interface OnVersionClickListener {
        void onVersionClick(VersionItem version);
    }

    public VersionAdapter(List<VersionItem> versionList, OnVersionClickListener listener) {
        this.versionList = versionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_version_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VersionItem item = versionList.get(position);

        holder.tvEdition.setText(item.edition);
        holder.tvVersion.setText(item.version);
        holder.tvDate.setText("Release: " + item.date);

        // Load ảnh bằng Glide nếu có URL
        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.imageUrl)
                    .placeholder(R.drawable.furnace) // ảnh mặc định
                    .into(holder.ivBlock);
        }

        // Xử lý click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVersionClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return versionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBlock;
        TextView tvEdition, tvVersion, tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBlock = itemView.findViewById(R.id.iv_block);
            tvEdition = itemView.findViewById(R.id.tv_edition);
            tvVersion = itemView.findViewById(R.id.tv_version);
            tvDate = itemView.findViewById(R.id.tv_date);
        }
    }

    public void updateList(List<VersionItem> newList) {
        versionList = newList;
        notifyDataSetChanged();
    }
}