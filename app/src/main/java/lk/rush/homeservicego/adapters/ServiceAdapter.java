package lk.rush.homeservicego.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import lk.rush.homeservicego.R;
import lk.rush.homeservicego.models.Service;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    private List<Service> serviceList;

    public interface OnServiceClickListener {
        void onServiceClick(Service service);
    }

    private OnServiceClickListener listener;

    public ServiceAdapter(List<Service> serviceList, OnServiceClickListener listener) {
        this.serviceList = serviceList;
        this.listener    = listener;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        Service service = serviceList.get(position);

        holder.tvName.setText(service.getName());
        holder.tvDescription.setText(service.getDescription());
        holder.tvPrice.setText("Rs. " + (long) service.getPrice());

        // Show image banner if service has one, hide otherwise
        if (service.getImageUrl() != null && !service.getImageUrl().isEmpty()) {
            holder.imgBanner.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(service.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.imgBanner);
        } else {
            holder.imgBanner.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onServiceClick(service);
        });
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    public static class ServiceViewHolder extends RecyclerView.ViewHolder {

        ImageView imgBanner;
        TextView tvName, tvDescription, tvPrice;

        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBanner     = itemView.findViewById(R.id.imgServiceBanner);
            tvName        = itemView.findViewById(R.id.tvServiceName);
            tvDescription = itemView.findViewById(R.id.tvServiceDescription);
            tvPrice       = itemView.findViewById(R.id.tvServicePrice);
        }
    }
}
