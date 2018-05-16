package com.code.englishnotes.data.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

import java.util.UUID;

public final class Vocabulary {
    @NonNull
    private final String mId;

    @Nullable
    private final String mTitle;

    @Nullable
    private final String mDescription;

    @Nullable
    private final String mType;

    @Nullable
    private final String mPronounce;

    private final boolean mCompleted;

    public Vocabulary(@Nullable String mTitle, @Nullable String mDescription) {
        this(UUID.randomUUID().toString(), mTitle, mDescription, null, null, false);
    }

    public Vocabulary(@Nullable String mTitle, @Nullable String mDescription, @Nullable String mId) {
        this(mId, mTitle, mDescription, null, null, false);
    }

    public Vocabulary(@Nullable String mTitle, @Nullable String mDescription, @Nullable String mId, boolean mCompleted) {
        this(mId, mTitle, mDescription, null, null, mCompleted);
    }

    public Vocabulary(@Nullable String mTitle, @Nullable String mDescription, boolean mCompleted) {
        this(UUID.randomUUID().toString(), mTitle, mDescription, null, null, mCompleted);
    }

    public Vocabulary(@NonNull String mId, @Nullable String mTitle, @Nullable String mDescription, @Nullable String mType, @Nullable String mPronounce, boolean mCompleted) {
        this.mId = mId;
        this.mTitle = mTitle;
        this.mDescription = mDescription;
        this.mType = mType;
        this.mPronounce = mPronounce;
        this.mCompleted = mCompleted;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    @Nullable
    public String getTitle() {
        return mTitle;
    }

    @Nullable
    public String getDescription() {
        return mDescription;
    }

    @Nullable
    public String getType() {
        return mType;
    }

    @Nullable
    public String getPronounce() {
        return mPronounce;
    }

    public boolean isCompleted() {
        return mCompleted;
    }

    public boolean isActive() {
        return !mCompleted;
    }

    public boolean isEmpty() {
        return Strings.isNullOrEmpty(mTitle) &&
                Strings.isNullOrEmpty(mDescription);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vocabulary vocabulary = (Vocabulary) o;
        return Objects.equal(mId, vocabulary.mId) &&
                Objects.equal(mTitle, vocabulary.mTitle) &&
                Objects.equal(mDescription, vocabulary.mDescription) &&
                Objects.equal(mPronounce, vocabulary.mPronounce) &&
                Objects.equal(mType, vocabulary.mType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mId, mTitle, mDescription, mPronounce, mType);
    }

    @Override
    public String toString() {
        return "Vocabulary " + mTitle;
    }
}
