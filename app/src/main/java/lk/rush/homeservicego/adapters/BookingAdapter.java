package lk.rush.homeservicego.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import lk.rush.homeservicego.R;
import lk.rush.homeservicego.models.Booking;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookingList;

    public BookingAdapter(List<Booking> bookingList) {
        this.bookingList = bookingList;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        holder.tvServiceName.setText(booking.getServiceName());
        holder.tvDate.setText(booking.getBookingDate());
        holder.tvTime.setText(booking.getBookingTime());
        holder.tvStatus.setText(booking.getStatus());

        // Show location row if we have one
        if (booking.getLocationName() != null && !booking.getLocationName().isEmpty()) {
            holder.layoutLocation.setVisibility(View.VISIBLE);
            holder.tvLocation.setText(booking.getLocationName());
        } else {
            holder.layoutLocation.setVisibility(View.GONE);
        }

        // Colour the status badge AND the top status strip to match booking status
        int statusColor;
        switch (booking.getStatus().toLowerCase()) {
            case "confirmed":
                statusColor = Color.parseColor("#4CAF50"); // green
                break;
            case "completed":
                statusColor = Color.parseColor("#9E9E9E"); // grey
                break;
            default:                                        // pending
                statusColor = Color.parseColor("#FF9800"); // orange
                break;
        }
        holder.tvStatus.getBackground().setTint(statusColor);
        holder.vStatusStrip.setBackgroundColor(statusColor);
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        View vStatusStrip;
        View layoutLocation;
        TextView tvServiceName, tvDate, tvTime, tvStatus, tvLocation;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            vStatusStrip  = itemView.findViewById(R.id.vStatusStrip);
            tvServiceName = itemView.findViewById(R.id.tvBookingServiceName);
            tvDate        = itemView.findViewById(R.id.tvBookingDate);
            tvTime        = itemView.findViewById(R.id.tvBookingTime);
            tvStatus      = itemView.findViewById(R.id.tvStatus);
            layoutLocation = itemView.findViewById(R.id.layoutLocation);
            tvLocation    = itemView.findViewById(R.id.tvBookingLocation);
        }
    }
}
