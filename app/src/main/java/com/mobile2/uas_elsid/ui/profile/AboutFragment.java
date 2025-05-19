package com.mobile2.uas_elsid.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import androidx.fragment.app.Fragment;
import com.mobile2.uas_elsid.R;


public class AboutFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        WebView mapWebView = view.findViewById(R.id.mapWebView);
        mapWebView.getSettings().setJavaScriptEnabled(true);
        mapWebView.loadData(
                "<iframe src=\"https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3960.2180930386126!2d110.4175539!3d-6.9835695!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x2e708b9cdfbd420f%3A0x6d53495409a62e74!2sELS%20Computer%20Semarang%20(ELS.ID)!5e0!3m2!1sid!2sid!4v1745045213880!5m2!1sid!2sid\" width=\"100%\" height=\"100%\" style=\"border:0;\" allowfullscreen=\"\" loading=\"lazy\" referrerpolicy=\"no-referrer-when-downgrade\"></iframe>",
                "text/html",
                "utf-8"
        );

        return view;
    }
}