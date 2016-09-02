package com.yahoo.squidb.test;

public enum TestEnum {
    APPLE, BANANA, CHERRY;

    @Override
    public String toString() {
        return "I am a TestEnum with name " + name();
    }
}
