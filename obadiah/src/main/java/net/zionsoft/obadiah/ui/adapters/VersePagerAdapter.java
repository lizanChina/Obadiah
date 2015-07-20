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

package net.zionsoft.obadiah.ui.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.Verse;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class VersePagerAdapter extends PagerAdapter {
    public static interface Listener {
        public void onVersesSelectionChanged(boolean hasSelected);
    }

    static class Page {
        boolean inUse;
        int position;
        VerseListAdapter verseListAdapter;

        View rootView;

        @Bind(R.id.loading_spinner)
        View loadingSpinner;

        @Bind(R.id.verse_list_view)
        ListView verseListView;

        Page(View view) {
            ButterKnife.bind(this, view);
            rootView = view;
        }
    }

    @Inject
    Bible bible;

    private final Context context;
    private final Listener listener;
    private final LayoutInflater inflater;
    private final List<Page> pages;

    private String translationShortName;
    private int currentBook = -1;
    private int currentChapter;
    private int currentVerse;

    public VersePagerAdapter(Context context, Listener listener) {
        super();
        App.get(context).getInjectionComponent().inject(this);

        this.context = context;
        this.listener = listener;
        inflater = LayoutInflater.from(context);
        pages = new LinkedList<>();
    }

    @Override
    public int getCount() {
        return currentBook < 0 || translationShortName == null ? 0 : Bible.getChapterCount(currentBook);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Page page = null;
        for (Page p : pages) {
            if (!p.inUse) {
                page = p;
                break;
            }
        }

        if (page == null) {
            page = new Page(inflater.inflate(R.layout.item_verse_pager, container, false));
            final VerseListAdapter verseListAdapter = new VerseListAdapter(context);
            page.verseListAdapter = verseListAdapter;
            page.verseListView.setAdapter(verseListAdapter);
            page.verseListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    verseListAdapter.select(position);
                    verseListAdapter.notifyDataSetChanged();

                    listener.onVersesSelectionChanged(verseListAdapter.hasSelectedVerses());
                }
            });
            pages.add(page);
        }

        container.addView(page.rootView, 0);
        page.inUse = true;
        page.position = position;

        page.loadingSpinner.setVisibility(View.VISIBLE);
        page.verseListView.setVisibility(View.GONE);
        loadVerses(position, page);

        return page;
    }

    private void loadVerses(final int position, final Page page) {
        bible.loadVerses(translationShortName, currentBook, position, new Bible.OnVersesLoadedListener() {
                    @Override
                    public void onVersesLoaded(List<Verse> verses) {
                        if (verses == null || verses.size() == 0) {
                            DialogHelper.showDialog(context, false, R.string.dialog_retry,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            loadVerses(position, page);
                                        }
                                    }, null
                            );
                            return;
                        }

                        if (page.position == position) {
                            AnimationHelper.fadeOut(page.loadingSpinner);
                            AnimationHelper.fadeIn(page.verseListView);

                            page.verseListAdapter.setVerses(verses);
                            page.verseListAdapter.notifyDataSetChanged();

                            if (currentVerse > 0 && currentChapter == position) {
                                page.verseListView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        page.verseListView.setSelection(currentVerse);
                                        currentVerse = 0;
                                    }
                                });
                            } else {
                                page.verseListView.setSelectionAfterHeaderView();
                            }
                        }
                    }
                }
        );
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        for (Page page : pages) {
            if (page.position == position) {
                page.inUse = false;
                container.removeView(page.rootView);
                return;
            }
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((Page) object).rootView;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    public void setTranslationShortName(String translationShortName) {
        this.translationShortName = translationShortName;
    }

    public void setSelected(int currentBook, int currentChapter, int currentVerse) {
        this.currentBook = currentBook;
        this.currentChapter = currentChapter;
        this.currentVerse = currentVerse;
    }

    public int getCurrentVerse(int chapter) {
        for (Page page : pages) {
            if (page.position == chapter)
                return page.verseListView.getFirstVisiblePosition();
        }
        return 0;
    }

    public List<Verse> getSelectedVerses(int chapter) {
        for (Page page : pages) {
            if (page.position == chapter)
                return page.verseListAdapter.getSelectedVerses();
        }
        return null;
    }

    public void deselectVerses() {
        for (Page page : pages) {
            page.verseListAdapter.deselectVerses();
            page.verseListAdapter.notifyDataSetChanged();
        }
    }
}
