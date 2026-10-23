package com.example.fruitshop.activities;

import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.fruitshop.R;
import com.example.fruitshop.models.OrderStatus;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class OrderUi {
    private OrderUi() {}

    static String date(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN")).format(new Date(millis));
    }

    static void status(TextView view, OrderStatus status) {
        int label = status == OrderStatus.PENDING ? R.string.status_pending
                : status == OrderStatus.CONFIRMED ? R.string.status_confirmed : R.string.status_completed;
        int surface = status == OrderStatus.PENDING ? R.color.order_pending_background
                : status == OrderStatus.CONFIRMED ? R.color.order_confirmed_background : R.color.order_completed_background;
        int ink = status == OrderStatus.PENDING ? R.color.order_pending_text
                : status == OrderStatus.CONFIRMED ? R.color.primary : R.color.muted;
        view.setText(label);
        view.setTextColor(ContextCompat.getColor(view.getContext(), ink));
        GradientDrawable background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(view.getContext(), surface));
        background.setCornerRadius(8 * view.getResources().getDisplayMetrics().density);
        view.setBackground(background);
    }

    static OrderStatus next(OrderStatus status) {
        return status == OrderStatus.PENDING ? OrderStatus.CONFIRMED
                : status == OrderStatus.CONFIRMED ? OrderStatus.COMPLETED : null;
    }
}
