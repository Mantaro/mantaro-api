/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantaroapi.entities;

public class PokemonData {
    private String name;
    private String url;
    private String[] names;

    public PokemonData(String name, String url, String[] names) {
        this.name = name;
        this.url = url;
        this.names = names;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String[] getNames() {
        return names;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setNames(String[] names) {
        this.names = names;
    }
}
