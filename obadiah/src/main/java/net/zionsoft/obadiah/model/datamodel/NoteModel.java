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

package net.zionsoft.obadiah.model.datamodel;

import android.database.sqlite.SQLiteDatabase;

import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.database.NoteTableHelper;
import net.zionsoft.obadiah.model.domain.Note;
import net.zionsoft.obadiah.model.domain.VerseIndex;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.AsyncEmitter;
import rx.Observable;
import rx.functions.Action1;

@Singleton
public class NoteModel {
    private final DatabaseHelper databaseHelper;

    @Inject
    public NoteModel(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public Observable<Note> updateNote(final VerseIndex verseIndex, final String note) {
        return Observable.fromAsync(new Action1<AsyncEmitter<Note>>() {
            @Override
            public void call(AsyncEmitter<Note> emitter) {
                try {
                    final SQLiteDatabase db = databaseHelper.getDatabase();
                    final boolean newNote = !NoteTableHelper.hasNote(db, verseIndex);

                    final Note n = Note.create(verseIndex, note, System.currentTimeMillis());
                    NoteTableHelper.saveNote(db, n);

                    if (newNote) {
                        Analytics.trackEvent(Analytics.CATEGORY_NOTES, Analytics.NOTES_ACTION_ADDED);
                    }

                    emitter.onNext(n);
                    emitter.onCompleted();
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        }, AsyncEmitter.BackpressureMode.ERROR);
    }

    public Observable<Void> removeNote(final VerseIndex verseIndex) {
        return Observable.fromAsync(new Action1<AsyncEmitter<Void>>() {
            @Override
            public void call(AsyncEmitter<Void> emitter) {
                try {
                    NoteTableHelper.removeNote(databaseHelper.getDatabase(), verseIndex);
                    Analytics.trackEvent(Analytics.CATEGORY_NOTES, Analytics.NOTES_ACTION_REMOVED);
                    emitter.onCompleted();
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        }, AsyncEmitter.BackpressureMode.ERROR);
    }

    public Observable<List<Note>> loadNotes(final int book, final int chapter) {
        return Observable.fromAsync(new Action1<AsyncEmitter<List<Note>>>() {
            @Override
            public void call(AsyncEmitter<List<Note>> emitter) {
                try {
                    emitter.onNext(NoteTableHelper.getNotes(databaseHelper.getDatabase(), book, chapter));
                    emitter.onCompleted();
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        }, AsyncEmitter.BackpressureMode.ERROR);
    }

    public Observable<List<Note>> loadNotes() {
        return Observable.fromAsync(new Action1<AsyncEmitter<List<Note>>>() {
            @Override
            public void call(AsyncEmitter<List<Note>> emitter) {
                try {
                    emitter.onNext(NoteTableHelper.getNotes(databaseHelper.getDatabase()));
                    emitter.onCompleted();
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        }, AsyncEmitter.BackpressureMode.ERROR);
    }
}
