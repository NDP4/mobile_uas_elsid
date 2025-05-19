package com.mobile2.uas_elsid.utils;

import com.mobile2.uas_elsid.R;

public class CategoryUtils {
    public static int getCategoryIcon(String categoryName) {
            // Normalisasi:
            // 1. Ubah ke lowercase
            // 2. Hapus semua spasi, strip, garis bawah
            // 3. Trim whitespace
            String normalized = categoryName.toLowerCase()
                    .replaceAll("\\s+", "")
                    .replace("-", "")
                    .replace("_", "")
                    .trim();

            switch (normalized) {
                case "casingkomputer":
                    return R.drawable.ic_casing;
                case "fan&cooler":
                    return R.drawable.ic_fan;
                case "motherboardamd":
                case "motherboardintel":
                    return R.drawable.ic_motherboard;
                case "powersupply":
                    return R.drawable.ic_power_supply;
                case "processoramd":
                case "processorintel":
                    return R.drawable.ic_processor;
                case "ram":
                    return R.drawable.ic_ram;
                case "vgacard":
                    return R.drawable.ic_vga;
                default:
                    return R.drawable.ic_all;
            }
        }
}