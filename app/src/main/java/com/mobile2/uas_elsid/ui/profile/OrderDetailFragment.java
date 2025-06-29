package com.mobile2.uas_elsid.ui.profile;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextPaint;
import android.text.Layout;
import android.text.StaticLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.adapter.OrderDetailAdapter;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.OrderResponse;
import com.mobile2.uas_elsid.api.response.PaymentStatusResponse;
import com.mobile2.uas_elsid.databinding.FragmentOrderDetailBinding;
import com.mobile2.uas_elsid.model.Order;
import com.mobile2.uas_elsid.model.OrderItem;
import com.mobile2.uas_elsid.utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailFragment extends Fragment {

    private FragmentOrderDetailBinding binding;
    private OrderDetailAdapter adapter;
    private SessionManager sessionManager;
    private int orderId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrderDetailBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());

        // Setup back button
        binding.backButton.setOnClickListener(v -> {
            Navigation.findNavController(v).popBackStack();
        });

        // Get order ID from arguments
        orderId = getArguments().getInt("order_id", -1);
        if (orderId == -1) {
            showError("Invalid order ID");
            return binding.getRoot();
        }

        setupRecyclerView();
        loadOrderDetails();
        setupReorderButton();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new OrderDetailAdapter(requireContext());
        binding.orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.orderItemsRecyclerView.setAdapter(adapter);
    }

    private void loadOrderDetails() {
        showLoading(true);
        ApiClient.getClient().getOrder(orderId).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                if (!isAdded() || binding == null) {
                    return;
                }

                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Order order = response.body().getData().getOrder();
                    if (order != null) {
                        updateOrderDetails(response.body());
                        updateTrackingStatus(order.getStatus());
                        checkPaymentStatus(order);
                    } else {
                        showError("Order data is missing");
                        Navigation.findNavController(requireView()).navigateUp();
                    }
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                showLoading(false);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void updateTrackingStatus(String status) {
        // Default all steps to inactive
        resetTrackingSteps();

        // Set status icon based on order status
        int statusIconRes = R.drawable.ic_order_processing;

        if (status == null) status = "pending";

        switch (status.toLowerCase()) {
            case "pending":
                activatePendingStep();
                statusIconRes = R.drawable.ic_order_pending;
                binding.statusIcon.setImageResource(statusIconRes);
                binding.statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.warning));
                break;

            case "processing":
                activateProcessingStep();
                statusIconRes = R.drawable.ic_order_processing;
                binding.statusIcon.setImageResource(statusIconRes);
                binding.statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
                break;

            case "picked_up":
            case "in_transit":
                activateInTransitStep();
                statusIconRes = R.drawable.ic_picked_up;
                binding.statusIcon.setImageResource(statusIconRes);
                binding.statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
                break;

            case "out_for_delivery":
                activateOutForDeliveryStep();
                statusIconRes = R.drawable.ic_out_for_delivery;
                binding.statusIcon.setImageResource(statusIconRes);
                binding.statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
                break;

            case "delivered":
                activateDeliveredStep();
                statusIconRes = R.drawable.ic_order_delivered;
                binding.statusIcon.setImageResource(statusIconRes);
                binding.statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success));
                break;

            case "cancelled":
                statusIconRes = R.drawable.ic_order_cancelled;
                binding.statusIcon.setImageResource(statusIconRes);
                binding.statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error));
                binding.trackingContainer.setVisibility(View.GONE);
                break;

            default:
                binding.trackingContainer.setVisibility(View.GONE);
                break;
        }
    }

    private void resetTrackingSteps() {
        // Pending step
        binding.pendingStep.setAlpha(0.5f);
        binding.pendingIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        binding.pendingLine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        // Processing step
        binding.processingStep.setAlpha(0.5f);
        binding.processingIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        binding.processingLine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        // In Transit step
        binding.inTransitStep.setAlpha(0.5f);
        binding.inTransitIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        binding.inTransitLine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        // Delivered step
        binding.deliveredStep.setAlpha(0.5f);
        binding.deliveredIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary));
    }

    private void activatePendingStep() {
        binding.pendingStep.setAlpha(1.0f);
        binding.pendingIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));

        binding.pendingDescription.setText("Your order has been received and is awaiting confirmation");
    }

    private void activateProcessingStep() {
        // Activate current and previous steps
        activatePendingStep();

        binding.processingStep.setAlpha(1.0f);
        binding.processingIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
        binding.pendingLine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary));

        binding.pendingDescription.setText("Your order has been confirmed");
        binding.processingDescription.setText("Your order is being prepared and packaged");
    }

    private void activateInTransitStep() {
        // Activate current and previous steps
        activateProcessingStep();

        binding.inTransitStep.setAlpha(1.0f);
        binding.inTransitIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
        binding.processingLine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary));

        binding.processingDescription.setText("Your order has been prepared and packaged");
        binding.inTransitDescription.setText("Your package is on its way to you");
    }

    private void activateOutForDeliveryStep() {
        // Activate current and previous steps
        activateInTransitStep();

        binding.inTransitDescription.setText("Your package is out for delivery today");
    }

    private void activateDeliveredStep() {
        // Activate current and previous steps
        activateInTransitStep();

        binding.deliveredStep.setAlpha(1.0f);
        binding.deliveredIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success));
        binding.inTransitLine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary));

        binding.inTransitDescription.setText("Your package has arrived in your area");
        binding.deliveredDescription.setText("Your package has been delivered successfully");
    }

    private void updateOrderDetails(OrderResponse response) {
        Order order = response.getData().getOrder();
        if (order == null) return;

        // Order ID and Status
        binding.orderIdText.setText(String.format("Order #%d", order.getId()));
        binding.orderStatusText.setText(order.getStatus() != null ? order.getStatus() : "Pending");
        binding.orderDateText.setText(formatDate(order.getCreatedAt()));

        // Payment Details
        String paymentMethod = "cod".equalsIgnoreCase(order.getPaymentMethod()) ? "Cash on Delivery" : "Bank Transfer";
        binding.paymentMethodText.setText(paymentMethod);
        binding.paymentStatusText.setText(order.getPaymentStatus() != null ? order.getPaymentStatus() : "Pending");

        // Set payment status color
        int statusColor = getPaymentStatusColor(order.getPaymentStatus());
        binding.paymentStatusText.setTextColor(statusColor);

        // Set payment method icon
        if ("cod".equalsIgnoreCase(order.getPaymentMethod())) {
            binding.paymentMethodIcon.setImageResource(R.drawable.ic_add);
        } else {
            binding.paymentMethodIcon.setImageResource(R.drawable.ic_payment);
        }

        // Show payment info for non-COD orders
        if (order.getPaymentMethod() != null && !order.getPaymentMethod().equalsIgnoreCase("cod")) {
            binding.onlinePaymentContainer.setVisibility(View.VISIBLE);

            // Show view payment button for certain payment statuses
            String paymentStatus = order.getPaymentStatus();
            boolean showPaymentButton = paymentStatus != null &&
                    (paymentStatus.equalsIgnoreCase("paid") ||
                            paymentStatus.equalsIgnoreCase("expired") ||
                            paymentStatus.equalsIgnoreCase("failed"));

            if (showPaymentButton && order.getPaymentUrl() != null) {
                binding.viewPaymentButton.setVisibility(View.VISIBLE);
                binding.viewPaymentButton.setOnClickListener(v -> {
                    // Toggle WebView visibility
                    if (binding.paymentWebview.getVisibility() == View.VISIBLE) {
                        binding.paymentWebview.setVisibility(View.GONE);
                        binding.viewPaymentButton.setText("View Payment Details");
                    } else {
                        binding.paymentWebview.setVisibility(View.VISIBLE);
                        binding.viewPaymentButton.setText("Hide Payment Details");

                        // Setup WebView
                        WebSettings webSettings = binding.paymentWebview.getSettings();
                        webSettings.setJavaScriptEnabled(true);
                        webSettings.setDomStorageEnabled(true);
                        webSettings.setLoadWithOverviewMode(true);
                        webSettings.setUseWideViewPort(true);
                        webSettings.setSupportZoom(true);
                        webSettings.setBuiltInZoomControls(true);
                        webSettings.setDisplayZoomControls(false);
                        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                        webSettings.setDefaultTextEncodingName("utf-8");
                        binding.paymentWebview.clearCache(true);

                        binding.paymentWebview.setWebViewClient(new WebViewClient());
                        binding.paymentWebview.loadUrl(order.getPaymentUrl());
                    }
                });
            } else {
                binding.viewPaymentButton.setVisibility(View.GONE);
            }
        } else {
            binding.onlinePaymentContainer.setVisibility(View.GONE);
            binding.paymentWebview.setVisibility(View.GONE);
        }

        // Shipping Details
        if (order.getUser() != null) {
            binding.recipientNameText.setText(order.getUser().getFullname());
        }

        String address = String.format("%s\n%s, %s %s",
                order.getShippingAddress() != null ? order.getShippingAddress() : "",
                order.getShippingCity() != null ? order.getShippingCity() : "",
                order.getShippingProvince() != null ? order.getShippingProvince() : "",
                order.getShippingPostalCode() != null ? order.getShippingPostalCode() : "");
        binding.shippingAddressText.setText(address);

        // Courier Details
        String courier = String.format("%s %s",
                order.getCourier() != null ? order.getCourier() : "",
                order.getCourierService() != null ? order.getCourierService() : "");
        binding.courierText.setText(courier);

        Integer estimatedDays = order.getEstimatedDays();
        if (estimatedDays != null && estimatedDays > 0) {
            binding.estimatedDeliveryText.setText(String.format("Estimated delivery: %d days", estimatedDays));
            binding.estimatedDeliveryText.setVisibility(View.VISIBLE);
        } else {
            binding.estimatedDeliveryText.setVisibility(View.GONE);
        }

        // Order Items
        if (order.getItems() != null) {
            adapter.setItems(order.getItems());
        }

        // Set discount info if available
        if (order.getCouponUsage() != null && order.getCouponUsage().getCoupon() != null) {
            adapter.setDiscountInfo(
                    order.getCouponUsage().getDiscountAmount(),
                    order.getCouponUsage().getCoupon().getCode()
            );
            binding.discountContainer.setVisibility(View.VISIBLE);
            binding.discountText.setText("-" + formatPrice(order.getCouponUsage().getDiscountAmount()));
        } else {
            binding.discountContainer.setVisibility(View.GONE);
        }

        // Payment Summary
        binding.subtotalText.setText(formatPrice(order.getSubtotal()));
        binding.shippingCostText.setText(formatPrice(order.getShippingCost()));
        binding.totalText.setText(formatPrice(order.getTotalAmount()));

        // Show payment info for all non-COD orders
        if (order.getPaymentUrl() != null && !"cod".equalsIgnoreCase(order.getPaymentMethod())) {
            binding.paymentWebview.setVisibility(View.VISIBLE);
            WebSettings webSettings = binding.paymentWebview.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setSupportZoom(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            webSettings.setDefaultTextEncodingName("utf-8");
            binding.paymentWebview.clearCache(true);

            binding.paymentWebview.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }
            });

            binding.paymentWebview.loadUrl(order.getPaymentUrl());
        } else {
            binding.paymentWebview.setVisibility(View.GONE);
        }

        // Show download invoice button for paid orders or COD
        boolean canDownloadInvoice = "cod".equalsIgnoreCase(order.getPaymentMethod()) ||
                "paid".equalsIgnoreCase(order.getPaymentStatus()) ||
                "settlement".equalsIgnoreCase(order.getPaymentStatus());

        if (canDownloadInvoice) {
            binding.reorderButton.setText("Download Invoice");
            binding.reorderButton.setIconResource(R.drawable.ic_download);
            binding.reorderButton.setOnClickListener(v -> downloadInvoice(order));
        } else {
            binding.reorderButton.setVisibility(View.GONE);
        }
    }

    private void downloadInvoice(Order order) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Colors
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int accentColor = Color.parseColor("#0066cc");
        int lightGray = Color.parseColor("#f5f7fa");
        int darkGray = Color.parseColor("#333333");
        int borderGray = Color.parseColor("#e0e0e0");
        int zebraRow = Color.parseColor("#f9f9f9");

        // Margins & layout
        int leftMargin = 40;
        int rightMargin = pageInfo.getPageWidth() - leftMargin;
        int contentWidth = rightMargin - leftMargin;
        int y = 40;

        // Header background
        Paint paintBg = new Paint();
        paintBg.setColor(lightGray);
        canvas.drawRect(leftMargin - 20, y - 20, rightMargin + 20, y + 80, paintBg);

        // Logo
        try {
            Drawable logoDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.logo);
            if (logoDrawable != null) {
                int logoW = 110, logoH = 32;
                Bitmap logoBitmap = Bitmap.createBitmap(logoW, logoH, Bitmap.Config.ARGB_8888);
                Canvas logoCanvas = new Canvas(logoBitmap);
                logoDrawable.setBounds(0, 0, logoW, logoH);
                logoDrawable.draw(logoCanvas);
                canvas.drawBitmap(logoBitmap, leftMargin, y, null);
            }
        } catch (Exception e) {}

        // Invoice title
        Paint paintTitle = new Paint();
        paintTitle.setColor(accentColor);
        paintTitle.setTextSize(28);
        paintTitle.setFakeBoldText(true);
        paintTitle.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("INVOICE", leftMargin, y + 55, paintTitle);

        // Order info (right)
        Paint paintInfo = new Paint();
        paintInfo.setColor(darkGray);
        paintInfo.setTextSize(13);
        paintInfo.setTextAlign(Paint.Align.RIGHT);
        paintInfo.setFakeBoldText(true);
        canvas.drawText("Invoice #" + order.getId(), rightMargin, y + 10, paintInfo);
        paintInfo.setFakeBoldText(false);
        paintInfo.setTextSize(12);
        canvas.drawText("Date: " + formatDate(order.getCreatedAt()), rightMargin, y + 30, paintInfo);
        y += 80;

        // Section: Shipping & Billing
        Paint paintSection = new Paint();
        paintSection.setColor(accentColor);
        paintSection.setTextSize(16);
        paintSection.setFakeBoldText(true);
        paintSection.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Shipping Details", leftMargin, y, paintSection);
        canvas.drawText("Billing Details", leftMargin + contentWidth / 2 + 20, y, paintSection);
        y += 22;

        Paint paintText = new Paint();
        paintText.setColor(darkGray);
        paintText.setTextSize(12);
        paintText.setTextAlign(Paint.Align.LEFT);
        int lineHeight = 17;
        int shippingX = leftMargin;
        int billingX = leftMargin + contentWidth / 2 + 20;
        int yShipping = y;
        int yBilling = y;
        // Shipping
        String shippingName = order.getUser() != null ? order.getUser().getFullname() : "-";
        String shippingAddr = (order.getShippingAddress() != null ? order.getShippingAddress() : "-") + ", " +
                (order.getShippingCity() != null ? order.getShippingCity() : "-") + ", " +
                (order.getShippingProvince() != null ? order.getShippingProvince() : "-") + " " +
                (order.getShippingPostalCode() != null ? order.getShippingPostalCode() : "-");
        String courier = (order.getCourier() != null ? order.getCourier().toUpperCase() : "-") +
                " " + (order.getCourierService() != null ? order.getCourierService() : "-");
        canvas.drawText(shippingName, shippingX, yShipping, paintText); yShipping += lineHeight;
        canvas.drawText(shippingAddr, shippingX, yShipping, paintText); yShipping += lineHeight;
        canvas.drawText("Courier: " + courier, shippingX, yShipping, paintText);
        // Billing
        if (order.getUser() != null) {
            canvas.drawText(order.getUser().getFullname(), billingX, yBilling, paintText); yBilling += lineHeight;
            canvas.drawText(order.getUser().getEmail(), billingX, yBilling, paintText); yBilling += lineHeight;
            if (order.getUser().getPhone() != null) {
                canvas.drawText(order.getUser().getPhone(), billingX, yBilling, paintText);
            }
        }
        y += 3 * lineHeight + 18;

        // Section: Order Items
        paintSection.setTextSize(15);
        paintSection.setColor(accentColor);
        canvas.drawText("Order Items", leftMargin, y, paintSection);
        y += 18;

        // Table header
        int itemWidth = (int)(contentWidth * 0.40);
        int qtyWidth = (int)(contentWidth * 0.13);
        int priceWidth = (int)(contentWidth * 0.20);
        int totalWidth = (int)(contentWidth * 0.27);
        int tableHeight = 28;
        Paint paintTableHeader = new Paint();
        paintTableHeader.setColor(accentColor);
        paintTableHeader.setTextSize(13);
        paintTableHeader.setFakeBoldText(true);
        paintTableHeader.setTextAlign(Paint.Align.LEFT);
        Paint paintTableBg = new Paint();
        paintTableBg.setColor(lightGray);
        canvas.drawRect(leftMargin, y, rightMargin, y + tableHeight, paintTableBg);
        canvas.drawText("Item", leftMargin + 10, y + 19, paintTableHeader);
        paintTableHeader.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Qty", leftMargin + itemWidth + qtyWidth / 2, y + 19, paintTableHeader);
        paintTableHeader.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Price", leftMargin + itemWidth + qtyWidth + priceWidth - 10, y + 19, paintTableHeader);
        canvas.drawText("Total", rightMargin - 10, y + 19, paintTableHeader);
        y += tableHeight;

        // Table items
        Paint paintRow = new Paint();
        paintRow.setTextSize(12);
        paintRow.setColor(darkGray);
        int rowNum = 0;
        for (OrderItem item : order.getItems()) {
            // Zebra row
            if (rowNum % 2 == 1) {
                Paint zebra = new Paint();
                zebra.setColor(zebraRow);
                canvas.drawRect(leftMargin, y, rightMargin, y + tableHeight, zebra);
            }
            String itemName = "-";
            if (item.getProduct() != null && item.getProduct().getTitle() != null) {
                itemName = item.getProduct().getTitle();
            }
            if (item.getVariant() != null && item.getVariant().getVariantName() != null) {
                itemName += " (" + item.getVariant().getVariantName() + ")";
            }
            int price = item.getVariant() != null ? item.getVariant().getPrice() : (item.getProduct() != null ? item.getProduct().getPrice() : 0);
            int discount = item.getVariant() != null ? item.getVariant().getDiscount() : (item.getProduct() != null ? item.getProduct().getDiscount() : 0);
            int discountedPrice = price - (price * discount / 100);
            int total = discountedPrice * item.getQuantity();
            // Item name (wrap if needed)
            TextPaint textPaint = new TextPaint(paintRow);
            textPaint.setAntiAlias(true);
            int itemNamePadding = 10;
            int itemCellWidth = itemWidth - 2 * itemNamePadding;
            StaticLayout textLayout = StaticLayout.Builder.obtain(
                    itemName, 0, itemName.length(),
                    textPaint, itemCellWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0, 1.1f)
                    .setIncludePad(false)
                    .build();
            int textHeight = textLayout.getHeight();
            int cellHeight = Math.max(textHeight + 2 * itemNamePadding, tableHeight);
            // Draw cell borders
            Paint borderPaint = new Paint();
            borderPaint.setColor(borderGray);
            borderPaint.setStrokeWidth(1);
            canvas.drawLine(leftMargin, y, rightMargin, y, borderPaint);
            // Draw item name
            canvas.save();
            canvas.translate(leftMargin + itemNamePadding, y + itemNamePadding);
            textLayout.draw(canvas);
            canvas.restore();
            // Qty
            paintRow.setTextAlign(Paint.Align.CENTER);
            float centerY = y + (cellHeight / 2f) - ((paintRow.descent() + paintRow.ascent()) / 2f);
            canvas.drawText(String.valueOf(item.getQuantity()), leftMargin + itemWidth + qtyWidth / 2, centerY, paintRow);
            // Price
            paintRow.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(formatPrice(discountedPrice), leftMargin + itemWidth + qtyWidth + priceWidth - 15, centerY, paintRow);
            // Total
            canvas.drawText(formatPrice(total), rightMargin - 15, centerY, paintRow);
            y += cellHeight;
            rowNum++;
        }
        // Table bottom border
        Paint borderPaint = new Paint();
        borderPaint.setColor(borderGray);
        borderPaint.setStrokeWidth(1);
        canvas.drawLine(leftMargin, y, rightMargin, y, borderPaint);
        y += 18;

        // Payment Method & Summary
        int summaryWidth = (int)(contentWidth * 0.42);
        int summaryLeft = rightMargin - summaryWidth;
        int summaryY = y;
        Paint paintSummaryBg = new Paint();
        paintSummaryBg.setColor(lightGray);
        canvas.drawRect(summaryLeft - 10, summaryY - 10, rightMargin + 10, summaryY + 90, paintSummaryBg);
        Paint paintSummary = new Paint();
        paintSummary.setTextSize(13);
        paintSummary.setColor(darkGray);
        paintSummary.setTextAlign(Paint.Align.LEFT);
        // Payment method
        canvas.drawText("Payment Method:", summaryLeft, summaryY + 10, paintSummary);
        String paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod() : "-";
        paintSummary.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(("cod".equalsIgnoreCase(paymentMethod) ? "Cash on Delivery" : paymentMethod.toUpperCase()), rightMargin - 10, summaryY + 10, paintSummary);
        // Subtotal
        paintSummary.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Subtotal:", summaryLeft, summaryY + 30, paintSummary);
        paintSummary.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(formatPrice(order.getSubtotal()), rightMargin - 10, summaryY + 30, paintSummary);
        // Shipping
        paintSummary.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Shipping Cost:", summaryLeft, summaryY + 50, paintSummary);
        paintSummary.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(formatPrice(order.getShippingCost()), rightMargin - 10, summaryY + 50, paintSummary);
        // Discount
        if (order.getCouponUsage() != null && order.getCouponUsage().getCoupon() != null) {
            paintSummary.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Discount (" + order.getCouponUsage().getCoupon().getCode() + "):", summaryLeft, summaryY + 70, paintSummary);
            paintSummary.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("-" + formatPrice(order.getCouponUsage().getDiscountAmount()), rightMargin - 10, summaryY + 70, paintSummary);
        }
        // Total (bold)
        Paint paintTotal = new Paint(paintSummary);
        paintTotal.setFakeBoldText(true);
        paintTotal.setTextSize(15);
        paintTotal.setColor(accentColor);
        paintTotal.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Total:", summaryLeft, summaryY + 90, paintTotal);
        paintTotal.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(formatPrice(order.getTotalAmount()), rightMargin - 10, summaryY + 90, paintTotal);
        y = summaryY + 110;

        // Footer
        Paint paintFooter = new Paint();
        paintFooter.setColor(primaryColor);
        paintFooter.setTextSize(13);
        paintFooter.setFakeBoldText(true);
        paintFooter.setTextAlign(Paint.Align.CENTER);
        int centerX = leftMargin + (contentWidth / 2);
        canvas.drawText("ELS.ID Computer Semarang", centerX, pageInfo.getPageHeight() - 80, paintFooter);
        paintFooter.setFakeBoldText(false);
        paintFooter.setTextSize(10);
        paintFooter.setColor(darkGray);
        canvas.drawText("Jl. MH Thamrin No.45, Miroto, Kec. Semarang Tengah, Kota Semarang, Jawa Tengah 50134", centerX, pageInfo.getPageHeight() - 65, paintFooter);
        canvas.drawText("Telp: 024-3500-500 / 0899-033-5000 | Email: semarang@els.co.id", centerX, pageInfo.getPageHeight() - 53, paintFooter);
        canvas.drawText("Jam Operasional: Senin – Minggu: 08:30 – 21:00", centerX, pageInfo.getPageHeight() - 41, paintFooter);
        paintFooter.setTextSize(10);
        paintFooter.setColor(accentColor);
        canvas.drawText("Terima kasih telah berbelanja di ELS.ID Computer Semarang!", centerX, pageInfo.getPageHeight() - 25, paintFooter);

        document.finishPage(page);
        // Save and open PDF
        String fileName = "Invoice_" + order.getId() + ".pdf";
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            document.writeTo(fos);
            document.close();
            openPdf(file);
        } catch (IOException e) {
            document.close();
            showError("Failed to save invoice: " + e.getMessage());
        }
    }

    private void openPdf(File file) {
        Uri uri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".provider",
                file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(binding.getRoot(), "Please install a PDF reader",
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    private void checkPaymentStatus(Order order) {
        if (order != null && order.getPaymentMethod() != null &&
                !order.getPaymentMethod().equals("cod") &&
                order.getPaymentUrl() != null) {

            ApiClient.getClient().checkPaymentStatus(order.getId(), sessionManager.getUserId())
                    .enqueue(new Callback<PaymentStatusResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<PaymentStatusResponse> call,
                                               @NonNull Response<PaymentStatusResponse> response) {
                            if (response.isSuccessful() && response.body() != null &&
                                    response.body().getData() != null) {
                                updatePaymentDetails(response.body().getData().toMap());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<PaymentStatusResponse> call, @NonNull Throwable t) {
                            showError("Failed to check payment status");
                        }
                    });
        }
    }

    private void updatePaymentDetails(Map<String, Object> paymentData) {
        if (paymentData != null) {
            binding.onlinePaymentContainer.setVisibility(View.VISIBLE);

            String transactionId = (String) paymentData.get("transaction_id");
            binding.transactionIdText.setText("Transaction ID: " + transactionId);

            String paymentTime = (String) paymentData.get("settlement_time");
            if (paymentTime != null) {
                binding.paymentTimeText.setText("Paid on " + formatDate(paymentTime));
            }

            String paymentUrl = (String) paymentData.get("payment_url");
            if (paymentUrl != null && !paymentData.get("payment_status").equals("paid")) {
                // Setup WebView
                binding.paymentWebview.setVisibility(View.VISIBLE);

                // Enable JavaScript
                WebSettings webSettings = binding.paymentWebview.getSettings();
                webSettings.setJavaScriptEnabled(true);

                // Set WebViewClient to handle redirects within the WebView
                binding.paymentWebview.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        // Check if payment is completed
                        if (url.contains("payment_status=success") ||
                                url.contains("transaction_status=settlement")) {
                            // Refresh order details
                            loadOrderDetails();
                        }
                    }
                });

                // Load Midtrans URL
                binding.paymentWebview.loadUrl(paymentUrl);
            } else {
                binding.paymentWebview.setVisibility(View.GONE);
            }
        }
    }

    private int getPaymentStatusColor(String status) {
        if (status == null) return ContextCompat.getColor(requireContext(), R.color.text_primary);

        switch (status.toLowerCase()) {
            case "paid":
                return ContextCompat.getColor(requireContext(), R.color.success);
            case "pending":
                return ContextCompat.getColor(requireContext(), R.color.warning);
            case "expired":
            case "failed":
                return ContextCompat.getColor(requireContext(), R.color.error);
            default:
                return ContextCompat.getColor(requireContext(), R.color.text_primary);
        }
    }

    private void setupReorderButton() {
        binding.reorderButton.setOnClickListener(v -> {
            Map<String, Object> reorderData = new HashMap<>();
            reorderData.put("order_id", orderId);
            reorderData.put("user_id", sessionManager.getUserId());

            binding.reorderButton.setEnabled(false);
            ApiClient.getClient().reorderItems(reorderData).enqueue(new Callback<OrderResponse>() {
                @Override
                public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                    binding.reorderButton.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_navigation_order_detail_to_navigation_detail_pesanan);
                    } else {
                        showError("Failed to reorder items");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                    binding.reorderButton.setEnabled(true);
                    showError("Network error: " + t.getMessage());
                }
            });
        });
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US);
            Date date = inputFormat.parse(dateStr);
            return date != null ? outputFormat.format(date) : dateStr;
        } catch (ParseException e) {
            return dateStr;
        }
    }

    private String formatPrice(int price) {
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = rupiahFormat.format(price);
        return formatted.substring(0, formatted.length() - 3);
    }

    private void handleErrorResponse(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                showError("Server error: " + errorBody);
            } else {
                showError("Failed to load order details (Status " + response.code() + ")");
            }
        } catch (Exception e) {
            showError("Failed to load order details");
        }
        Navigation.findNavController(requireView()).navigateUp();
    }

    private void showLoading(boolean isLoading) {
        // TODO: Implement loading indicator
    }

    private void showError(String message) {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
