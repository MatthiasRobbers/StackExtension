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
import java.util.zip.GZIPInputStream;

public class StackExtension extends DashClockExtension {
    private static final String TAG = "StackExtension";

    public static final String PREF_SITE = "pref_site";
    public static final String PREF_USER_ID = "pref_user_id";
    public static final String PREF_DISPLAY = "pref_display";

    private static final int DISPLAY_REPUTATION = 0;
    private static final int DISPLAY_DAY_REPUTATION = 1;
    private static final int DISPLAY_REPUTATION_CHANGE = 2;

    private Sites mSites;
    private String mSite;
    private String mUserId;
    private String mUrl;
    private int mIcon;
    private int mDisplay;

    private String mReputation;

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        setUpdateWhenScreenOn(true);
        mSites = new Sites(this);
    }

    @Override
    protected void onUpdateData(int reason) {
        // Get preference value.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mSite = sp.getString(PREF_SITE, null);
        mUserId = sp.getString(PREF_USER_ID, null);

        String display = sp.getString(PREF_DISPLAY, null);
        if (display.equals(getString(R.string.display_reputation))) {
            mDisplay = DISPLAY_REPUTATION;
        } else if (display.equals(getString(R.string.display_day_reputation))) {
            mDisplay = DISPLAY_DAY_REPUTATION;
        } else if (display.equals(getString(R.string.display_reputation_change))) {
            mDisplay = DISPLAY_REPUTATION_CHANGE;
        }

        mUrl = mSites.getUrlFromApiParameter(mSite) + "/users/" + mUserId;
        mIcon = mSites.getIcon(mSite);

        if (mSite.equals("") || mUserId.equals("")) {
            Log.i(TAG, "Data missing");
            return;
        }
        DefaultHttpClient client = new DefaultHttpClient();

        String apiUri = "http://api.stackexchange.com/2.1/users/" + mUserId;
        if (mDisplay == DISPLAY_REPUTATION) {
            apiUri += "?";
        } else if (mDisplay == DISPLAY_DAY_REPUTATION) {
            apiUri += "/reputation?fromdate=1363219200&todate=1363305600&";
        } else if (mDisplay == DISPLAY_REPUTATION_CHANGE) {
            apiUri += "/reputation?fromdate=1363219200&todate=1363305600&";
        }
        apiUri += "site=" + mSite;

        HttpGet get = new HttpGet(apiUri);
        get.addHeader("Accept-Encoding", "gzip");

        Log.i(TAG, apiUri);
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

        if (mReputation == null) {
            return;
        }

        // Publish the extension data update.
        publishUpdate(new ExtensionData()
                .visible(true)
                .icon(mIcon)
                .status(mReputation)
                .expandedTitle(mReputation + " Reputation")
                .expandedBody(mSites.getNameFromApiParameter(mSite))
                .clickIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(mUrl))));
    }

    private void parseJson(String json) {
        try {
            switch (mDisplay) {
                case DISPLAY_REPUTATION:
                    JSONObject user = new JSONObject(json).getJSONArray("items").getJSONObject(0);
                    mReputation = user.getString("reputation");
                    break;
                case DISPLAY_DAY_REPUTATION:
                    JSONArray items = new JSONObject(json).getJSONArray("items");
                    int reputation = 0;
                    for (int i = 0; i < items.length(); i++) {
                        reputation += items.getJSONObject(i).getInt("reputation_change");
                    }
                    mReputation = String.valueOf(reputation);
                    break;
                case DISPLAY_REPUTATION_CHANGE:
                    mReputation = getString(R.string.status_none);
                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
