package com.example.campusx.ui.feed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.campusx.R;
import com.example.campusx.model.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
    private List<Item> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Item> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView itemImage;
        private TextView itemTitle;
        private TextView itemPrice;
        private TextView ownerName;
        private TextView ratingText;
        private TextView itemLocation;
        private TextView listingMode;
        private TextView buyBadge;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.item_image);
            itemTitle = itemView.findViewById(R.id.item_title);
            itemPrice = itemView.findViewById(R.id.item_price);
            ownerName = itemView.findViewById(R.id.owner_name);
            ratingText = itemView.findViewById(R.id.rating_text);
            itemLocation = itemView.findViewById(R.id.item_location);
            listingMode = itemView.findViewById(R.id.listing_mode);
            buyBadge = itemView.findViewById(R.id.buy_badge);
        }

        public void bind(Item item, OnItemClickListener listener) {
            itemTitle.setText(item.getTitle());
            itemPrice.setText(itemView.getContext().getString(R.string.per_day, item.getPricePerDay()));
            ownerName.setText("Listed by " + item.getOwnerName());
            ratingText.setText(String.format(Locale.getDefault(), "%.1f", item.getOwnerRating()));
            itemLocation.setText(itemView.getContext().getString(R.string.nearby_label) + " · " + item.getPickupLocation());
            listingMode.setText(R.string.rent_badge);
            buyBadge.setVisibility(View.VISIBLE);

            // Load image with Glide
            if (item.getImages() != null && !item.getImages().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getImages().get(0))
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(itemImage);
            } else {
                itemImage.setImageResource(R.drawable.ic_launcher_background);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
    }
}
