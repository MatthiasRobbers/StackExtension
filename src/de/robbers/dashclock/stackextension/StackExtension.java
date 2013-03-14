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
    public static final String PREF_ACCOUNT_ID = "pref_account_id";

    private String site;
    private String accountId;
    private String url;
    private int icon;
    private String reputation;

    @Override
    protected void onUpdateData(int reason) {
        Sites sites = new Sites(this);
        // Get preference value.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        site = sp.getString(PREF_SITE, getString(R.string.pref_site_default));
        accountId = sp.getString(PREF_ACCOUNT_ID,
                getString(R.string.pref_account_id_default));
        url = sites.getUrlFromApiParameter(site) + "/users/" + accountId;
        icon = sites.getIcon(site);

        if (site.equals("") || accountId.equals("")) {
            Log.i(TAG, "Data missing");
            return;
        }
        DefaultHttpClient client = new DefaultHttpClient();

        String apiUri = "http://api.stackexchange.com/2.1/users/" + accountId + "?site="
                + site;
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
            JSONObject user = new JSONObject(result).getJSONArray("items").getJSONObject(0);
            reputation = user.getString("reputation");
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (reputation == null) {
            return;
        }

        // Publish the extension data update.
        publishUpdate(new ExtensionData()
                .visible(true)
                .icon(icon)
                .status(reputation)
                .expandedTitle(reputation + " Reputation")
                .expandedBody(site)
                .clickIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));
    }
}
