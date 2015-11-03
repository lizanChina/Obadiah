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

package net.zionsoft.obadiah.injection.components.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.injection.InjectionComponent;
import net.zionsoft.obadiah.injection.components.Component;
import net.zionsoft.obadiah.injection.components.HasComponent;

public abstract class ComponentFragment<C extends Component> extends Fragment implements HasComponent<C> {
    private C component;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (component == null) {
            component = createComponent(App.get(context).getInjectionComponent());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public C getComponent() {
        return component;
    }

    protected abstract C createComponent(InjectionComponent injectionComponent);
}