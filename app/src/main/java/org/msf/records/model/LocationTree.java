package org.msf.records.model;

import java.util.TreeMap;
import java.util.TreeSet;

import org.msf.records.net.model.Location;

/**
 * A LocationTree represents a tree of Locations, with each level of the tree sorted by the given
 * locale.
 */
public class LocationTree implements Comparable<LocationTree> {
    private final String DEFAULT_LOCALE = "en";

    private TreeMap<String, LocationTree> mChildren;
    private Location mLocation;
    private String mSortLocale = DEFAULT_LOCALE;

    public LocationTree(Location location) {
        mLocation = location;
        mChildren = new TreeMap<String, LocationTree>();
    }

    public Location getLocation() {
        return mLocation;
    }

    public void setSortLocale(String sortLocale) {
        mSortLocale = sortLocale;
    }

    public TreeMap<String, LocationTree> getChildren() {
        return mChildren;
    }

    public TreeSet<LocationTree> getLocationsForDepth(int depth) {
        TreeSet<LocationTree> locations = new TreeSet<LocationTree>();
        if (depth == 0) {
            locations.addAll(getChildren().values());
            return locations;
        }

        for (String uuid : mChildren.keySet()) {
            locations.addAll(getLocationsForDepth(depth - 1));
        }

        return locations;
    }

    @Override
    public String toString() {
        if (!mLocation.names.containsKey(mSortLocale)) {
            return "";
        }

        return mLocation.names.get(mSortLocale);
    }

    @Override
    public int compareTo(LocationTree another) {
        if (!mLocation.names.containsKey(mSortLocale) ||
                !another.getLocation().names.containsKey(mSortLocale)) {
            return 0;
        }

        return mLocation.names.get(mSortLocale).compareTo(
                another.getLocation().names.get(mSortLocale));
    }
}
