package com.github.michalbednarski.intentslab.browser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

/**
 * Helper class for spliting components into categories in {@link Fetcher}
 */
abstract class Categorizer<C> {
    private HashMap<String, ArrayList<Fetcher.Component>> mMap = new HashMap<String, ArrayList<Fetcher.Component>>();

    abstract void add(C component);
    abstract String getTitleForComponent(C component);

    void addToCategory(String category, C component) {
        Fetcher.Component cmp = new Fetcher.Component();
        cmp.title = getTitleForComponent(component);
        cmp.componentInfo = component;
        if (mMap.containsKey(category)) {
            mMap.get(category).add(cmp);
        } else {
            ArrayList<Fetcher.Component> newList = new ArrayList<Fetcher.Component>();
            newList.add(cmp);
            mMap.put(category, newList);
        }
    }

    Fetcher.Category[] getResult() {
        Set<String> keySet = mMap.keySet();
        String[] categoryNames = keySet.toArray(new String[keySet.size()]);
        Arrays.sort(categoryNames);
        Fetcher.Category[] result = new Fetcher.Category[categoryNames.length];
        for (int i = 0, j = categoryNames.length; i < j; i++) {
            String categoryName = categoryNames[i];
            ArrayList<Fetcher.Component> componentsList = mMap.get(categoryName);
            Fetcher.Category category = new Fetcher.Category();
            category.title = categoryName;
            category.components = componentsList.toArray(new Fetcher.Component[componentsList.size()]);
            result[i] = category;
        }

        return result;
    }
}
