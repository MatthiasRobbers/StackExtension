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

    // from SharedPreferences
    private String mSite;
    private String mUserId;
    private int mDisplay;

    // used in publishUpdate
    private String mStatus;
    private String mUrl;
    private int mIcon;
    private boolean mVisible = true;

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
        // Get preference value.
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

        if (mSite.equals("") || mUserId.equals("")) {
            Log.i(TAG, "Data missing");
            return;
        }
        DefaultHttpClient client = new DefaultHttpClient();

        HttpGet get = new HttpGet(buildApiUri());
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
            String result = builder.toString();
            zis.close();

            // get data from JSON
            parseJson(result);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mStatus = String.valueOf(mReputation);

        // Publish the extension data update.
        publishUpdate(new ExtensionData()
                .visible(mVisible)
                .icon(mIcon)
                .status(mStatus)
                .expandedTitle(mReputation + " Reputation")
                .expandedBody(mSites.getNameFromApiParameter(mSite))
                .clickIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(mUrl))));
    }

    private String buildApiUri() {
        String apiUri = "http://api.stackexchange.com/2.1/users/" + mUserId;
        if (mDisplay == DISPLAY_REPUTATION) {
            apiUri += "?";
        } else if (mDisplay == DISPLAY_DAY_REPUTATION) {
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
            apiUri += "/reputation?fromdate=" + fromDate + "&todate=" + toDate + "&";
        }
        apiUri += "site=" + mSite;
        return apiUri;
    }

    private void parseJson(String json) {
        Log.i(TAG, json);
        try {
            switch (mDisplay) {
                case DISPLAY_REPUTATION:
                    JSONObject user = new JSONObject(json).getJSONArray("items").getJSONObject(0);
                    mVisible = true;
                    mReputation = user.getInt("reputation");
                    return;
                case DISPLAY_DAY_REPUTATION:
                    JSONArray items = new JSONObject(json).getJSONArray("items");
                    mVisible = items.length() == 0 ? false : true;
                    mReputation = 0;
                    for (int i = 0; i < items.length(); i++) {
                        mReputation += items.getJSONObject(i).getInt("reputation_change");
                    }
                    return;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
