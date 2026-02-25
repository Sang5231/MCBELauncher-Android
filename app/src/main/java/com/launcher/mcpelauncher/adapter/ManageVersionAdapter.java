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

public class ManageVersionAdapter extends RecyclerView.Adapter<ManageVersionAdapter.ViewHolder> {

    private List<VersionItem> versionList;
    private OnVersionActionListener listener;

    public interface OnVersionActionListener {
        void onEditClicked(VersionItem version);
        void onDeleteClicked(VersionItem version);
    }

    public ManageVersionAdapter(List<VersionItem> versionList, OnVersionActionListener listener) {
        this.versionList = versionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.manage_version_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VersionItem item = versionList.get(position);

        // Kiểm tra null
        if (item == null) return;

        // Đặt giá trị với kiểm tra null
        if (holder.tvEdition != null) {
            holder.tvEdition.setText(item.edition != null ? item.edition : "Unknown");
        }

        if (holder.tvVersion != null) {
            holder.tvVersion.setText(item.version != null ? item.version : "N/A");
        }

        if (holder.tvDate != null) {
            holder.tvDate.setText("Release: " + (item.date != null ? item.date : "Unknown"));
        }

        // Load ảnh
        if (holder.ivBlock != null) {
            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.imageUrl)
                        .placeholder(R.drawable.furnace)
                        .into(holder.ivBlock);
            } else {
                holder.ivBlock.setImageResource(R.drawable.furnace);
            }
        }

        // Xử lý nút Edit
        if (holder.btnEdit != null) {
            holder.btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClicked(item);
                }
            });
        }

        // Xử lý nút Delete
        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClicked(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return versionList != null ? versionList.size() : 0;
    }

    public void updateList(List<VersionItem> newList) {
        versionList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBlock;
        TextView tvEdition, tvVersion, tvDate; // ĐÃ XÓA tvStatus
        ImageView btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBlock = itemView.findViewById(R.id.iv_block);
            tvEdition = itemView.findViewById(R.id.tv_edition);
            tvVersion = itemView.findViewById(R.id.tv_version);
            tvDate = itemView.findViewById(R.id.tv_date);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}