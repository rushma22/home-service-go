package lk.rush.homeservicego.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import lk.rush.homeservicego.R;
import lk.rush.homeservicego.models.Service;

public class AdminServiceAdapter extends RecyclerView.Adapter<AdminServiceAdapter.ServiceViewHolder> {

    private List<Service> serviceList;

    public interface OnActionListener {
        void onEdit(Service service, int position);
        void onDelete(Service service, int position);
    }

    private OnActionListener listener;

    public AdminServiceAdapter(List<Service> serviceList, OnActionListener listener) {
        this.serviceList = serviceList;
        this.listener    = listener;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_service, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        Service service = serviceList.get(position);

        holder.tvName.setText(service.getName());
        holder.tvCategory.setText(service.getCategory());
        holder.tvPrice.setText("Rs. " + (long) service.getPrice());

        // Show image if available, otherwise show the placeholder colour strip
        if (service.getImageUrl() != null && !service.getImageUrl().isEmpty()) {
            holder.imgService.setVisibility(View.VISIBLE);
            holder.vPlaceholder.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext())
                    .load(service.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.imgService);
        } else {
            holder.imgService.setVisibility(View.GONE);
            holder.vPlaceholder.setVisibility(View.VISIBLE);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(service, position);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(service, position);
        });
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    public void removeItem(int position) {
        serviceList.remove(position);
        notifyItemRemoved(position);
    }

    public void updateItem(int position, Service updated) {
        serviceList.set(position, updated);
        notifyItemChanged(position);
    }

    public static class ServiceViewHolder extends RecyclerView.ViewHolder {
        ImageView imgService;
        View vPlaceholder;
        TextView tvName, tvCategory, tvPrice;
        Button btnEdit, btnDelete;

        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            imgService   = itemView.findViewById(R.id.imgService);
            vPlaceholder = itemView.findViewById(R.id.vImagePlaceholder);
            tvName       = itemView.findViewById(R.id.tvServiceName);
            tvCategory   = itemView.findViewById(R.id.tvServiceCategory);
            tvPrice      = itemView.findViewById(R.id.tvServicePrice);
            btnEdit      = itemView.findViewById(R.id.btnEditService);
            btnDelete    = itemView.findViewById(R.id.btnDeleteService);
        }
    }
}
