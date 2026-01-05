package com.controlbro.claimy.model;

public enum TownFlag {
    ALLOW_ALLY_BUILD(true),
    ALLOW_ALLY_DOORS(true),
    ALLOW_ALLY_VILLAGERS(true),
    ALLOW_ALLY_CONTAINERS(true),
    ALLOW_ALLY_REDSTONE(true),
    GRAVITY_BLOCKS(false);

    private final boolean defaultValue;

    TownFlag(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }
}
