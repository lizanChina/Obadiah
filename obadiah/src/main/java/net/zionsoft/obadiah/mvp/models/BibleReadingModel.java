/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2015 ZionSoft
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.zionsoft.obadiah.mvp.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.translations.TranslationInfo;

import java.util.List;

import rx.Observable;
import rx.Subscriber;

public class BibleReadingModel {
    private final Bible bible;
    private final SharedPreferences preferences;

    public BibleReadingModel(Context context, Bible bible) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        this.bible = bible;
    }

    public String loadCurrentTranslation() {
        return preferences.getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
    }

    public void saveCurrentTranslation(TranslationInfo translation) {
        preferences.edit().putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, translation.shortName).apply();
        Analytics.trackTranslationSelection(translation.shortName);
    }

    public boolean hasDownloadedTranslation() {
        return !TextUtils.isEmpty(getCurrentTranslation());
    }

    @Nullable
    public String getCurrentTranslation() {
        return preferences.getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
    }

    public void setCurrentTranslation(String translation) {
        preferences.edit().putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, translation).apply();
    }

    public void storeReadingProgress(int book, int chapter, int verse) {
        preferences.edit()
                .putInt(Constants.PREF_KEY_LAST_READ_BOOK, book)
                .putInt(Constants.PREF_KEY_LAST_READ_CHAPTER, chapter)
                .putInt(Constants.PREF_KEY_LAST_READ_VERSE, verse)
                .apply();
    }

    public Observable<List<String>> loadTranslations() {
        return Observable.create(new Observable.OnSubscribe<List<String>>() {
            @Override
            public void call(Subscriber<? super List<String>> subscriber) {
                try {
                    subscriber.onNext(bible.loadTranslations());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<List<String>> loadBookNames(final String translation) {
        return Observable.create(new Observable.OnSubscribe<List<String>>() {
            @Override
            public void call(Subscriber<? super List<String>> subscriber) {
                try {
                    subscriber.onNext(bible.loadBookNames(translation));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }
}
