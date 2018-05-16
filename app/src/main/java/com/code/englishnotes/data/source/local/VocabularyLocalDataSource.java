package com.code.englishnotes.data.source.local;

import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.code.englishnotes.data.model.Vocabulary;
import com.code.englishnotes.data.source.VocabularyDataSource;
import com.code.englishnotes.utils.schedulers.BaseSchedulerProvider;
import com.google.common.base.Optional;
import com.squareup.sqlbrite3.BriteDatabase;
import com.squareup.sqlbrite3.SqlBrite;

import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public class VocabularyLocalDataSource implements VocabularyDataSource {

    @Nullable
    private static VocabularyLocalDataSource INSTANCE;

    @NonNull
    private final BriteDatabase mDatabaseHelper;

    @NonNull
    private Function<Cursor, Vocabulary> mVocabularyMapperFunction;

    public VocabularyLocalDataSource(@NonNull Context context,
                                     @NonNull BaseSchedulerProvider schedulerProvider) {
        checkNotNull(context, "context cannot be null");
        checkNotNull(schedulerProvider, "scheduleProvider cannot be null");
        VocabularyDbHelper dbHelper = new VocabularyDbHelper(context);
        SqlBrite sqlBrite = new SqlBrite.Builder().build();
        mDatabaseHelper = sqlBrite.wrapDatabaseHelper((SupportSQLiteOpenHelper) dbHelper, schedulerProvider.io());
        mVocabularyMapperFunction = this::getVocabulary;
    }

    @NonNull
    private Vocabulary getVocabulary(@NonNull Cursor c) {
        String itemId = c.getString(c.getColumnIndexOrThrow(VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_ENTRY_ID));
        String title = c.getString(c.getColumnIndexOrThrow(VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_TITLE));
        String description = c.getString(c.getColumnIndexOrThrow(VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_DESCRIPTION));
        String type = c.getString(c.getColumnIndexOrThrow(VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_TYPE));
        String pronounce = c.getString(c.getColumnIndexOrThrow(VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_PRONOUNCE));
        boolean completed = c.getInt(c.getColumnIndexOrThrow(VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_COMPLETED)) == 1;
        return new Vocabulary(itemId, title, description, type, pronounce, completed);
    }

    public static VocabularyLocalDataSource getInstance(
            @NonNull Context context,
            @NonNull BaseSchedulerProvider schedulerProvider) {
        if (INSTANCE == null) {
            INSTANCE = new VocabularyLocalDataSource(context, schedulerProvider);
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }

    @Override
    public Flowable<List<Vocabulary>> getVocabularys() {
        String[] projection = {
                VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_ENTRY_ID,
                VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_TITLE,
                VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_DESCRIPTION,
                VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_TYPE,
                VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_PRONOUNCE,
                VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_COMPLETED
        };
        String sql = String.format("SELECT %s FROM %s", TextUtils.join(",", projection), VocabularyPersistenceContract.VocabularyEntry.TABLE_NAME);
        return mDatabaseHelper.createQuery(VocabularyPersistenceContract.VocabularyEntry.TABLE_NAME, sql)
                .mapToList(mVocabularyMapperFunction)
                .toFlowable(BackpressureStrategy.BUFFER);
    }

    @Override
    public Flowable<Optional<Vocabulary>> getVocabulary(@NonNull String vocabularyId) {
        String[] projection = {
                VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_ENTRY_ID,
                VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_TITLE,
                VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_DESCRIPTION,
                VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_TYPE,
                VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_PRONOUNCE,
                VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_COMPLETED
        };
        String sql = String.format("SELECT %s FROM %s WHERE %s LIKE ?",
                TextUtils.join(",", projection), VocabularyPersistenceContract.VocabularyEntry.TABLE_NAME, VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_ENTRY_ID);
        return mDatabaseHelper.createQuery(VocabularyPersistenceContract.VocabularyEntry.TABLE_NAME, sql, vocabularyId)
                .mapToOneOrDefault(cursor -> Optional.of(mVocabularyMapperFunction.apply(cursor)), Optional.<Vocabulary>absent())
                .toFlowable(BackpressureStrategy.BUFFER);
    }

    @Override
    public void saveVocabulary(@NonNull Vocabulary vocabulary) {
        checkNotNull(vocabulary);
        ContentValues values = new ContentValues();
        values.put(VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_ENTRY_ID, vocabulary.getId());
        values.put(VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_TITLE, vocabulary.getTitle());
        values.put(VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_DESCRIPTION, vocabulary.getDescription());
        values.put(VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_TYPE, vocabulary.getType());
        values.put(VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_PRONOUNCE, vocabulary.getPronounce());
        values.put(VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_COMPLETED, vocabulary.isCompleted());
        mDatabaseHelper.insert(VocabularyPersistenceContract.VocabularyEntry.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
    }

    @Override
    public void completeVocabulary(@NonNull Vocabulary vocabulary) {
        completeVocabulary(vocabulary.getId());
    }

    @Override
    public void completeVocabulary(@NonNull String vocabularyId) {
        ContentValues values = new ContentValues();
        values.put(VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_COMPLETED, true);

        String selection = VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
        String[] selectionArgs = {vocabularyId};
        mDatabaseHelper.update(VocabularyPersistenceContract.VocabularyEntry.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values, selection, selectionArgs);
    }

    @Override
    public void activateVocabulary(@NonNull Vocabulary vocabulary) {
        activateVocabulary(vocabulary.getId());
    }

    @Override
    public void activateVocabulary(@NonNull String vocabularyId) {
        ContentValues values = new ContentValues();
        values.put(VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_COMPLETED, false);

        String selection = VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
        String[] selectionArgs = {vocabularyId};
        mDatabaseHelper.update(VocabularyPersistenceContract.VocabularyEntry.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE , values, selection, selectionArgs);
    }

    @Override
    public void clearCompletedVocabularys() {
        String selection = VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_COMPLETED + " LIKE ?";
        String[] selectionArgs = {"1"};
        mDatabaseHelper.delete(VocabularyPersistenceContract.VocabularyEntry.TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public void refreshVocabularys() {

    }

    @Override
    public void deleteAllVocabularys() {
        mDatabaseHelper.delete(VocabularyPersistenceContract.VocabularyEntry.TABLE_NAME, null);
    }

    @Override
    public void deleteVocabulary(@NonNull String vocabularyId) {
        String selection = VocabularyPersistenceContract.VocabularyEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
        String[] selectionArgs = {vocabularyId};
        mDatabaseHelper.delete(VocabularyPersistenceContract.VocabularyEntry.TABLE_NAME, selection, selectionArgs);
    }
}
