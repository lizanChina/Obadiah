/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2017 ZionSoft
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

package net.zionsoft.obadiah.bookmarks;

import android.support.v4.util.Pair;

import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.datamodel.BookmarkModel;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.mvp.BasePresenter;
import net.zionsoft.obadiah.utils.RxHelper;

import java.util.ArrayList;
import java.util.List;

import rx.SingleSubscriber;
import rx.Subscription;
import rx.functions.Func1;

class BookmarksPresenter extends BasePresenter<BookmarksView> {
    @SuppressWarnings("WeakerAccess")
    final BibleReadingModel bibleReadingModel;
    private final BookmarkModel bookmarkModel;

    @SuppressWarnings("WeakerAccess")
    Subscription subscription;

    BookmarksPresenter(BibleReadingModel bibleReadingModel, BookmarkModel bookmarkModel, Settings settings) {
        super(settings);
        this.bibleReadingModel = bibleReadingModel;
        this.bookmarkModel = bookmarkModel;
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
    }

    void loadBookmarks() {
        subscription = bookmarkModel.loadBookmarks()
                .map(new Func1<List<Bookmark>, Pair<List<Bookmark>, List<Verse>>>() {
                    @Override
                    public Pair<List<Bookmark>, List<Verse>> call(List<Bookmark> bookmarks) {
                        final int count = bookmarks.size();
                        final List<Verse> verses = new ArrayList<>(count);
                        final String translation = bibleReadingModel.loadCurrentTranslation();
                        for (int i = 0; i < count; ++i) {
                            final VerseIndex verseIndex = bookmarks.get(i).verseIndex();
                            verses.add(bibleReadingModel.loadVerse(translation, verseIndex.book(),
                                    verseIndex.chapter(), verseIndex.verse()).toBlocking().value());
                        }
                        return new Pair<>(bookmarks, verses);
                    }
                }).compose(RxHelper.<Pair<List<Bookmark>, List<Verse>>>applySchedulersForSingle())
                .subscribe(new SingleSubscriber<Pair<List<Bookmark>, List<Verse>>>() {
                    @Override
                    public void onSuccess(Pair<List<Bookmark>, List<Verse>> bookmarks) {
                        subscription = null;
                        final BookmarksView v = getView();
                        if (v != null) {
                            v.onBookmarksLoaded(bookmarks.first, bookmarks.second);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        subscription = null;
                        final BookmarksView v = getView();
                        if (v != null) {
                            v.onBookmarksLoadFailed();
                        }
                    }
                });
    }

    void saveReadingProgress(VerseIndex index) {
        bibleReadingModel.saveReadingProgress(index);
    }
}
