package com.bookshelf.shelf.model;

import java.util.Arrays;
import java.util.List;

public enum ShelfType {

    READ("Read", true),
    CURRENTLY_READING("Currently Reading", true),
    OWNED("Owned", true),
    WISH_LIST("Wish List", true),
    CUSTOM("Custom", false);

    private final String displayName;
    private final boolean isDefault;

    ShelfType(String displayName, boolean isDefault) {
        this.displayName = displayName;
        this.isDefault = isDefault;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public static List<ShelfType> getDefaults() {
        return Arrays.stream(values())
                .filter(ShelfType::isDefault)
                .toList();
    }
}
