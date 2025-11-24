package com.return0.notes.domain.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SearchFilters implements Serializable {
    private final Map<String, String> filters;

    public SearchFilters() {
        this.filters = new HashMap<>();
    }

    public void setFilter(String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            filters.put(key, value.trim());
        } else {
            filters.remove(key);
        }
    }

    public String getFilter(String key) {
        return filters.get(key);
    }

    public Map<String, String> getFilters() {
        return new HashMap<>(filters);
    }

    public boolean hasFilters() {
        return !filters.isEmpty();
    }

    public void clear() {
        filters.clear();
    }
}




