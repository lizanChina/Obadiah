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

package net.zionsoft.obadiah.mvp.presenters;

import net.zionsoft.obadiah.mvp.models.BibleReadingModel;
import net.zionsoft.obadiah.mvp.models.ReadingProgressModel;
import net.zionsoft.obadiah.mvp.views.BibleReadingView;

import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class BibleReadingPresenter extends MVPPresenter<BibleReadingView> {
    private final BibleReadingModel bibleReadingModel;
    private final ReadingProgressModel readingProgressModel;

    private CompositeSubscription subscription;

    public BibleReadingPresenter(BibleReadingModel bibleReadingModel, ReadingProgressModel readingProgressModel) {
        this.bibleReadingModel = bibleReadingModel;
        this.readingProgressModel = readingProgressModel;
    }

    @Override
    protected void onViewTaken() {
        super.onViewTaken();
        subscription = new CompositeSubscription();
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
    }

    public void loadTranslations() {
        if (bibleReadingModel.hasDownloadedTranslation()) {
            subscription.add(bibleReadingModel.loadTranslations()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<String>>() {
                        @Override
                        public void onCompleted() {
                            // do nothing
                        }

                        @Override
                        public void onError(Throwable e) {
                            final BibleReadingView v = getView();
                            if (v != null) {
                                v.onTranslationsLoadFailed();
                            }
                        }

                        @Override
                        public void onNext(List<String> translations) {
                            final BibleReadingView v = getView();
                            if (v != null) {
                                if (translations.size() > 0) {
                                    v.onTranslationsLoaded(translations);
                                } else {
                                    // Would it ever reach here?
                                    v.informNoTranslationDownloaded();
                                }
                            }
                        }
                    }));
        } else {
            final BibleReadingView v = getView();
            if (v != null) {
                v.informNoTranslationDownloaded();
            }
        }
    }

    public void setCurrentTranslation(String translation) {
        bibleReadingModel.setCurrentTranslation(translation);
    }

    public void storeReadingProgress(int book, int chapter, int verse) {
        bibleReadingModel.storeReadingProgress(book, chapter, verse);
    }
}
