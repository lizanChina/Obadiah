/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2016 ZionSoft
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

package net.zionsoft.obadiah.biblereading.verse;

import android.support.annotation.NonNull;

import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.datamodel.BookmarkModel;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.domain.VerseWithParallelTranslations;
import net.zionsoft.obadiah.mvp.BasePresenter;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class VersePagerPresenter extends BasePresenter<VersePagerView> {
    private final BibleReadingModel bibleReadingModel;
    private final BookmarkModel bookmarkModel;
    private CompositeSubscription subscription;

    public VersePagerPresenter(BibleReadingModel bibleReadingModel, BookmarkModel bookmarkModel, Settings settings) {
        super(settings);
        this.bibleReadingModel = bibleReadingModel;
        this.bookmarkModel = bookmarkModel;
    }

    @Override
    protected void onViewTaken() {
        super.onViewTaken();

        getSubscription().add(Observable.merge(
                bibleReadingModel.observeCurrentTranslation()
                        .map(new Func1<String, Void>() {
                            @Override
                            public Void call(String s) {
                                return null;
                            }
                        }),
                bibleReadingModel.observeParallelTranslation())
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(Void param) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onTranslationUpdated();
                        }
                    }
                }));

        getSubscription().add(bibleReadingModel.observeCurrentReadingProgress()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Verse.Index>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(Verse.Index index) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onReadingProgressUpdated(index);
                        }
                    }
                }));
    }

    @NonNull
    private CompositeSubscription getSubscription() {
        if (subscription == null) {
            subscription = new CompositeSubscription();
        }
        return subscription;
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
    }

    String loadCurrentTranslation() {
        return bibleReadingModel.loadCurrentTranslation();
    }

    int loadCurrentBook() {
        return bibleReadingModel.loadCurrentBook();
    }

    int loadCurrentChapter() {
        return bibleReadingModel.loadCurrentChapter();
    }

    int loadCurrentVerse() {
        return bibleReadingModel.loadCurrentVerse();
    }

    void saveReadingProgress(int book, int chapter, int verse) {
        bibleReadingModel.saveReadingProgress(new Verse.Index(book, chapter, verse));
    }

    void loadVerses(int book, int chapter) {
        if (bibleReadingModel.hasParallelTranslation()) {
            loadVersesWithParallelTranslations(book, chapter);
        } else {
            loadVersesOnly(book, chapter);
        }
    }

    private void loadVersesOnly(final int book, final int chapter) {
        getSubscription().add(bibleReadingModel.loadVerses(loadCurrentTranslation(), book, chapter)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Verse>>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onVersesLoadFailed(book, chapter);
                        }
                    }

                    @Override
                    public void onNext(List<Verse> verses) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onVersesLoaded(verses);
                        }
                    }
                }));
    }

    private void loadVersesWithParallelTranslations(final int book, final int chapter) {
        getSubscription().add(bibleReadingModel.loadVersesWithParallelTranslations(book, chapter)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<VerseWithParallelTranslations>>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onVersesLoadFailed(book, chapter);
                        }
                    }

                    @Override
                    public void onNext(List<VerseWithParallelTranslations> verses) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onVersesWithParallelTranslationsLoaded(verses);
                        }
                    }
                }));
    }

    void addFavorite(Verse.Index verseIndex) {
        getSubscription().add(bookmarkModel.addFavorite(verseIndex)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        // TODO
                    }

                    @Override
                    public void onError(Throwable e) {
                        // TODO
                    }

                    @Override
                    public void onNext(Void v) {
                        // should not reach here
                    }
                }));
    }

    void removeFavorite(Verse.Index verseIndex) {
        getSubscription().add(bookmarkModel.removeFavorite(verseIndex)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        // TODO
                    }

                    @Override
                    public void onError(Throwable e) {
                        // TODO
                    }

                    @Override
                    public void onNext(Void v) {
                        // should not reach here
                    }
                }));
    }
}
