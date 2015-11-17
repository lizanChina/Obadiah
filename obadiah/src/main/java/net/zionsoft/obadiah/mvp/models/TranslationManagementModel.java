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

import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.okhttp.ResponseBody;

import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.translations.TranslationHelper;
import net.zionsoft.obadiah.model.translations.TranslationInfo;
import net.zionsoft.obadiah.model.translations.Translations;
import net.zionsoft.obadiah.network.BackendChapter;
import net.zionsoft.obadiah.network.BackendInterface;
import net.zionsoft.obadiah.network.BackendTranslationInfo;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okio.BufferedSource;
import okio.Okio;
import retrofit.Response;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class TranslationManagementModel {
    private final DatabaseHelper databaseHelper;
    private final BackendInterface backendInterface;
    private final JsonAdapter<BackendTranslationInfo> translationInfoJsonAdapter;
    private final JsonAdapter<BackendChapter> chapterJsonAdapter;

    public TranslationManagementModel(DatabaseHelper databaseHelper, Moshi moshi, BackendInterface backendInterface) {
        this.databaseHelper = databaseHelper;
        this.backendInterface = backendInterface;
        this.translationInfoJsonAdapter = moshi.adapter(BackendTranslationInfo.class);
        this.chapterJsonAdapter = moshi.adapter(BackendChapter.class);
    }

    public Observable<Translations> loadTranslations(boolean forceRefresh) {
        Observable<List<TranslationInfo>> translations;
        if (forceRefresh) {
            translations = loadFromNetwork();
        } else {
            translations = Observable.concat(loadFromLocal(), loadFromNetwork())
                    .first(new Func1<List<TranslationInfo>, Boolean>() {
                        @Override
                        public Boolean call(List<TranslationInfo> translations) {
                            return translations.size() > 0;
                        }
                    });
        }
        translations = translations.map(new Func1<List<TranslationInfo>, List<TranslationInfo>>() {
            @Override
            public List<TranslationInfo> call(List<TranslationInfo> translations) {
                return TranslationHelper.sortByLocale(translations);
            }
        });
        final Observable<List<String>> downloaded = Observable.create(
                new Observable.OnSubscribe<List<String>>() {
                    @Override
                    public void call(Subscriber<? super List<String>> subscriber) {
                        SQLiteDatabase db = null;
                        try {
                            db = databaseHelper.openDatabase();
                            subscriber.onNext(TranslationHelper.getDownloadedTranslationShortNames(db));
                            subscriber.onCompleted();
                        } catch (Exception e) {
                            subscriber.onError(e);
                        } finally {
                            if (db != null) {
                                databaseHelper.closeDatabase();
                            }
                        }
                    }
                });
        return Observable.zip(translations, downloaded, new Func2<List<TranslationInfo>, List<String>, Translations>() {
            @Override
            public Translations call(List<TranslationInfo> translations, List<String> downloaded) {
                return new Translations.Builder()
                        .translations(translations)
                        .downloaded(downloaded)
                        .build();
            }
        });
    }

    private Observable<List<TranslationInfo>> loadFromNetwork() {
        return backendInterface.fetchTranslations()
                .doOnNext(new Action1<List<TranslationInfo>>() {
                    @Override
                    public void call(List<TranslationInfo> translations) {
                        SQLiteDatabase db = null;
                        try {
                            db = databaseHelper.openDatabase();
                            db.beginTransaction();
                            TranslationHelper.saveTranslations(db, translations);
                            db.setTransactionSuccessful();
                        } finally {
                            if (db != null) {
                                if (db.inTransaction()) {
                                    db.endTransaction();
                                }
                                databaseHelper.closeDatabase();
                            }
                        }
                    }
                })
                // workaround for Retrofit / okhttp issue (of sorts)
                // https://github.com/square/retrofit/issues/1046
                // https://github.com/square/okhttp/issues/1592
                .unsubscribeOn(Schedulers.io());
    }

    private Observable<List<TranslationInfo>> loadFromLocal() {
        return Observable.create(new Observable.OnSubscribe<List<TranslationInfo>>() {
            @Override
            public void call(Subscriber<? super List<TranslationInfo>> subscriber) {
                SQLiteDatabase db = null;
                try {
                    db = databaseHelper.openDatabase();
                    subscriber.onNext(TranslationHelper.getTranslations(db));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                } finally {
                    if (db != null) {
                        databaseHelper.closeDatabase();
                    }
                }
            }
        });
    }

    public Observable<Void> removeTranslation(final TranslationInfo translation) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    SQLiteDatabase db = null;
                    try {
                        db = databaseHelper.openDatabase();
                        db.beginTransaction();
                        TranslationHelper.removeTranslation(db, translation.shortName);
                        db.setTransactionSuccessful();
                    } finally {
                        if (db != null) {
                            if (db.inTransaction()) {
                                db.endTransaction();
                            }
                            databaseHelper.closeDatabase();
                        }
                    }
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<Integer> fetchTranslation(final TranslationInfo translation) {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                final long timestamp = SystemClock.elapsedRealtime();
                boolean success = false;

                SQLiteDatabase db = null;
                ZipInputStream is = null;
                try {
                    final Response<ResponseBody> response
                            = backendInterface.fetchTranslation(translation.blobKey).execute();
                    if (response.code() != 200) {
                        throw new IOException("Unsupported HTTP status code - " + response.code());
                    }

                    db = databaseHelper.openDatabase();
                    db.beginTransaction();
                    TranslationHelper.createTranslationTable(db, translation.shortName);

                    is = new ZipInputStream(response.body().byteStream());
                    ZipEntry entry;
                    int downloaded = 0;
                    int progress = -1;
                    while ((entry = is.getNextEntry()) != null) {
                        final String entryName = entry.getName();
                        BufferedSource bufferedSource = Okio.buffer(Okio.source(is));
                        if (entryName.equals("books.json")) {
                            TranslationHelper.saveBookNames(db,
                                    translationInfoJsonAdapter.fromJson(bufferedSource));
                        } else {
                            final String[] parts = entryName.substring(0, entryName.length() - 5).split("-");
                            final int book = Integer.parseInt(parts[0]);
                            final int chapter = Integer.parseInt(parts[1]);
                            TranslationHelper.saveVerses(db, translation.shortName, book, chapter,
                                    chapterJsonAdapter.fromJson(bufferedSource).verses);
                        }

                        // only emits if the progress is actually changed
                        // should I move this logic to a filter?
                        final int currentProgress = ++downloaded / 12;
                        if (currentProgress > progress) {
                            progress = currentProgress;
                            // the progress should be less or equal than 100
                            // in this case, with Integer.valueOf() no small object will be created
                            //noinspection UnnecessaryBoxing
                            subscriber.onNext(Integer.valueOf(progress));
                        }
                    }
                    db.setTransactionSuccessful();
                    subscriber.onCompleted();
                    success = true;
                } catch (Exception e) {
                    subscriber.onError(e);
                } finally {
                    Analytics.trackTranslationDownload(translation.shortName, success, SystemClock.elapsedRealtime() - timestamp);

                    if (db != null) {
                        if (db.inTransaction()) {
                            db.endTransaction();
                        }
                        databaseHelper.closeDatabase();
                    }

                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            // not much we can do here
                        }
                    }
                }
            }
        });
    }
}