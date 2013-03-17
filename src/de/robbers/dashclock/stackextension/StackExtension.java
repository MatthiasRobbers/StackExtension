/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.robbers.dashclock.stackextension;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

public class StackExtension extends DashClockExtension {
    private static final String TAG = "StackExtension";

    public static final String PREF_SITE = "pref_site";
    public static final String PREF_USER_ID = "pref_user_id";
    public static final String PREF_DISPLAY = "pref_display";

    private static final int DISPLAY_REPUTATION = 0;
    private static final int DISPLAY_DAY_REPUTATION = 1;

    private static final int EXPANDED_BODY_POSTS = 2;

    // from SharedPreferences
    private String mSite;
    private String mUserId;
    private int mDisplay;

    // used in publishUpdate
    private boolean mVisible = true;
    private String mStatus;
    private String mUrl;
    private int mIcon;
    private String mExpandedTitle;
    private String mExpandedBody;

    private Sites mSites;
    private int mReputation;
    private boolean mError;

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        setUpdateWhenScreenOn(true);
        mSites = new Sites(this);
    }

    @Override
    protected void onUpdateData(int reason) {
        Log.i(TAG, "onUpdateData");

        loadPreferences();

        if (mSite.equals("") || mUserId.equals("")) {
            Log.i(TAG, "Data missing");
            return;
        }

        switch (mDisplay) {
            case DISPLAY_REPUTATION:
                performUserRequest();
                performReputationRequest();
                break;
            case DISPLAY_DAY_REPUTATION:
                performDayReputationRequest();
                break;
        }

        publishUpdate();
    }

    private void loadPreferences() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mSite = sp.getString(PREF_SITE, null);
        mUserId = sp.getString(PREF_USER_ID, null);

        String display = sp.getString(PREF_DISPLAY, null);
        if (display.equals(getString(R.string.display_reputation))) {
            mDisplay = DISPLAY_REPUTATION;
        } else if (display.equals(getString(R.string.display_day_reputation))) {
            mDisplay = DISPLAY_DAY_REPUTATION;
        }

        mUrl = mSites.getUrlFromApiParameter(mSite) + "/users/" + mUserId;
        mIcon = mSites.getIcon(mSite);
    }

    private String performHttpRequest(String uri) {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(uri);
        get.addHeader("Accept-Encoding", "gzip");
        try {
            // get JSON from Stack Exchange API
            HttpResponse response = client.execute(get);
            InputStream inputStream = response.getEntity().getContent();
            GZIPInputStream zis = new GZIPInputStream(new BufferedInputStream(inputStream));
            InputStreamReader reader = new InputStreamReader(zis);
            BufferedReader in = new BufferedReader(reader);
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                builder.append(line);
            }
            String json = builder.toString();
            zis.close();
            return json;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void performUserRequest() {
        String uri = "http://api.stackexchange.com/2.1/users/" + mUserId
                + "?filter=!23InChbQ8Zub*hOrntVVM&site=" + mSite;
        String json = performHttpRequest(uri);
        parseUserResponse(json);
    }

    private void performReputationRequest() {
        // today
        Calendar date = Calendar.getInstance();
        date.setTimeZone(TimeZone.getTimeZone("UTC"));
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        // 7 days ago
        date.add(Calendar.DAY_OF_MONTH, -7);
        long fromDate = date.getTimeInMillis() / 1000;

        // tomorrow
        date.add(Calendar.DAY_OF_MONTH, 8);
        long toDate = date.getTimeInMillis() / 1000;

        String uri = "http://api.stackexchange.com/2.1/users/" + mUserId
                + "/reputation?fromdate=" + fromDate + "&todate=" + toDate
                + "&filter=!)qoIx37Y_u8lL30-SFjg" + "&site=" + mSite;
        String json = performHttpRequest(uri);
        parseReputationResponse(json);
    }

    private void performDayReputationRequest() {
        // today
        Calendar date = Calendar.getInstance();
        date.setTimeZone(TimeZone.getTimeZone("UTC"));
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        long fromDate = date.getTimeInMillis() / 1000;

        // tomorrow
        date.add(Calendar.DAY_OF_MONTH, 1);
        long toDate = date.getTimeInMillis() / 1000;

        String uri = "http://api.stackexchange.com/2.1/users/" + mUserId
                + "/reputation?fromdate=" + fromDate + "&todate=" + toDate
                + "&filter=!)qoIx37Y_u8lL30-SFjg" + "&site=" + mSite;
        String json = performHttpRequest(uri);
        parseDayReputationResponse(json);
    }

    private void parseUserResponse(String json) {
        if (json == null) {
            return;
        }
        Log.i(TAG, json);
        try {
            JSONObject user = new JSONObject(json).getJSONArray("items").getJSONObject(0);
            mReputation = user.getInt("reputation");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void parseReputationResponse(String json) {
        if (json == null) {
            return;
        }
        Log.i(TAG, json);
        mVisible = true;
        mExpandedBody = "";
        try {
            JSONArray items = new JSONObject(json).getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject reputation = items.getJSONObject(i);
                int reputationChange = reputation.getInt("reputation_change");
                String title = String.valueOf(Html.fromHtml(reputation.getString("title")));
                mExpandedBody += buildExpandedBodyLine(reputationChange, title, i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(mExpandedBody)) {
            mExpandedBody = getString(R.string.no_recent_reputation_changes);
        }
    }

    private void parseDayReputationResponse(String json) {
        if (json == null) {
            return;
        }
        Log.i(TAG, json);
        mExpandedBody = "";
        try {
            JSONArray items = new JSONObject(json).getJSONArray("items");
            mVisible = items.length() == 0 ? false : true;
            mReputation = 0;
            for (int i = 0; i < items.length(); i++) {
                JSONObject reputation = items.getJSONObject(i);
                int reputationChange = reputation.getInt("reputation_change");
                mReputation += reputationChange;
                String title = String.valueOf(Html.fromHtml(reputation.getString("title")));
                mExpandedBody += buildExpandedBodyLine(reputationChange, title, i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String buildExpandedBodyLine(int reputationChange, String title, int lineNumber) {
        String line = "";
        if (lineNumber < EXPANDED_BODY_POSTS) {
            line += lineNumber > 0 ? "\n" : "";
            line += reputationChange > 0 ? "+" + reputationChange
                    : reputationChange;
            line += " \u2014 " + title;
        }
        return line;
    }

    private void publishUpdate() {
        mStatus = String.valueOf(mReputation);
        mExpandedTitle = mReputation + " Reputation" + " \u2014 "
                + mSites.getNameFromApiParameter(mSite);
        mExpandedBody = mExpandedBody == null ? "" : mExpandedBody;

        // Publish the extension data update.
        publishUpdate(new ExtensionData()
                .visible(mVisible)
                .icon(mIcon)
                .status(mStatus)
                .expandedTitle(mExpandedTitle)
                .expandedBody(mExpandedBody)
                .clickIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(mUrl))));
    }
}
