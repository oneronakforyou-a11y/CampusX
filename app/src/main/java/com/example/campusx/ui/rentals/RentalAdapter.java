package com.example.campusx.ui.rentals;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.campusx.R;
import com.example.campusx.model.Booking;
import com.example.campusx.model.BookingStatus;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RentalAdapter extends RecyclerView.Adapter<RentalAdapter.RentalViewHolder> {
    private List<Booking> bookings = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private String currentUserId;
    private boolean showRequestActions;
    private ActionListener actionListener;

    public interface ActionListener {
        void onAccept(Booking booking);
        void onDecline(Booking booking);
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
        notifyDataSetChanged();
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setShowRequestActions(boolean showRequestActions) {
        this.showRequestActions = showRequestActions;
        notifyDataSetChanged();
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public RentalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rental_card, parent, false);
        return new RentalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RentalViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        holder.bind(booking, dateFormat, currentUserId, showRequestActions, actionListener);
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class RentalViewHolder extends RecyclerView.ViewHolder {
        private ImageView itemImage;
        private TextView itemTitle, bookingDates, bookingParty, totalPrice, statusBadge, roleBadge, otpText;
        private View actionRow;
        private MaterialButton acceptButton, declineButton;

        public RentalViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.item_image);
            itemTitle = itemView.findViewById(R.id.item_title);
            bookingDates = itemView.findViewById(R.id.booking_dates);
            bookingParty = itemView.findViewById(R.id.booking_party);
            totalPrice = itemView.findViewById(R.id.total_price);
            statusBadge = itemView.findViewById(R.id.status_badge);
            roleBadge = itemView.findViewById(R.id.role_badge);
            otpText = itemView.findViewById(R.id.otp_text);
            actionRow = itemView.findViewById(R.id.action_row);
            acceptButton = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
        }

        public void bind(Booking booking, SimpleDateFormat dateFormat, String currentUserId,
                         boolean showRequestActions, ActionListener actionListener) {
            itemTitle.setText(booking.getItemTitle());

            String startDateStr = dateFormat.format(new Date(booking.getStartDate()));
            String endDateStr = dateFormat.format(new Date(booking.getEndDate()));
            bookingDates.setText(startDateStr + " - " + endDateStr);

            totalPrice.setText(String.format(Locale.getDefault(), "₹%.0f total", booking.getTotalPrice()));
            statusBadge.setText(booking.getStatus().getDisplayName());

            boolean isSeller = currentUserId != null && currentUserId.equals(booking.getListerId());
            roleBadge.setText(isSeller ? R.string.booking_role_selling : R.string.booking_role_buying);
            bookingParty.setText(isSeller
                    ? itemView.getContext().getString(R.string.booking_renter_format, safeName(booking.getRenterName()))
                    : itemView.getContext().getString(R.string.booking_seller_format, safeName(booking.getListerName())));

            // Show OTP for confirmed bookings
            if (booking.getStatus() == BookingStatus.CONFIRMED ||
                booking.getStatus() == BookingStatus.ACTIVE) {
                otpText.setText("Handoff PIN: " + booking.getOtp());
                otpText.setVisibility(View.VISIBLE);
            } else {
                otpText.setVisibility(View.GONE);
            }

            boolean canAct = showRequestActions
                    && isSeller
                    && booking.getStatus() == BookingStatus.PENDING
                    && actionListener != null;
            actionRow.setVisibility(canAct ? View.VISIBLE : View.GONE);
            if (canAct) {
                acceptButton.setOnClickListener(v -> actionListener.onAccept(booking));
                declineButton.setOnClickListener(v -> actionListener.onDecline(booking));
            } else {
                acceptButton.setOnClickListener(null);
                declineButton.setOnClickListener(null);
            }

            // Load image
            Glide.with(itemView.getContext())
                    .load(booking.getItemImage())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(itemImage);
        }

        private String safeName(String name) {
            return name == null || name.trim().isEmpty() ? "CampusX user" : name;
        }
    }
}
