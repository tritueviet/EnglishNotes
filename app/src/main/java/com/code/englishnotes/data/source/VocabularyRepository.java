package com.code.englishnotes.data.source;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.code.englishnotes.data.model.Vocabulary;
import com.google.common.base.Optional;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;

import static com.google.common.base.Preconditions.checkNotNull;

public class VocabularyRepository implements VocabularyDataSource{

    @Nullable
    private static VocabularyRepository INSTANCE = null;

    @NonNull
    private final VocabularyDataSource mVocabularyRemoteDataSource;

    @NonNull
    private final VocabularyDataSource mVocabularyLocalDataSource;

    /**
     * This variable has package local visibility so it can be accessed from tests.
     */
    @VisibleForTesting
    @Nullable
    Map<String, Vocabulary> mCachedVocabulary;

    /**
     * Marks the cache as invalid, to force an update the next time data is requested. This variable
     * has package local visibility so it can be accessed from tests.
     */
    @VisibleForTesting
    boolean mCacheIsDirty = false;

    private VocabularyRepository(@NonNull VocabularyDataSource vocabularyRemoteDataSource,
                            @NonNull VocabularyDataSource vocabularyLocalDataSource) {
        mVocabularyRemoteDataSource = checkNotNull(vocabularyRemoteDataSource);
        mVocabularyLocalDataSource = checkNotNull(vocabularyLocalDataSource);
    }

    public static VocabularyRepository getInstance(@NonNull VocabularyDataSource vocabularyRemoteDataSource,
                                              @NonNull VocabularyDataSource vocabularyLocalDataSource) {
        if (INSTANCE == null) {
            INSTANCE = new VocabularyRepository(vocabularyRemoteDataSource, vocabularyLocalDataSource);
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }

    @Override
    public Flowable<List<Vocabulary>> getVocabularys() {
        if (mCachedVocabulary != null && !mCacheIsDirty) {
            return Flowable.fromIterable(mCachedVocabulary.values()).toList().toFlowable();
        } else if (mCachedVocabulary == null) {
            mCachedVocabulary = new LinkedHashMap<>();
        }

        Flowable<List<Vocabulary>> remoteTasks = getAndSaveRemoteVocabulary();

        if (mCacheIsDirty) {
            return remoteTasks;
        } else {
            // Query the local storage if available. If not, query the network.
            Flowable<List<Vocabulary>> localTasks = getAndCacheLocalVocabulary();
            return Flowable.concat(localTasks, remoteTasks)
                    .filter(tasks -> !tasks.isEmpty())
                    .firstOrError()
                    .toFlowable();
        }
    }
    private Flowable<List<Vocabulary>> getAndCacheLocalVocabulary() {
        return mVocabularyLocalDataSource.getVocabularys()
                .flatMap(tasks -> Flowable.fromIterable(tasks)
                        .doOnNext(task -> mCachedVocabulary.put(task.getId(), task))
                        .toList()
                        .toFlowable());
    }

    private Flowable<List<Vocabulary>> getAndSaveRemoteVocabulary() {
        return mVocabularyRemoteDataSource
                .getVocabularys()
                .flatMap(tasks -> Flowable.fromIterable(tasks).doOnNext(task -> {
                    mVocabularyLocalDataSource.saveVocabulary(task);
                    mCachedVocabulary.put(task.getId(), task);
                }).toList().toFlowable())
                .doOnComplete(() -> mCacheIsDirty = false);
    }
    @Override
    public void saveVocabulary(@NonNull Vocabulary vocabulary) {
        checkNotNull(vocabulary);
        mVocabularyRemoteDataSource.saveVocabulary(vocabulary);
        mVocabularyLocalDataSource.saveVocabulary(vocabulary);

        // Do in memory cache update to keep the app UI up to date
        if (mCachedVocabulary == null) {
            mCachedVocabulary = new LinkedHashMap<>();
        }
        mCachedVocabulary.put(vocabulary.getId(), vocabulary);
    }

    @Override
    public void completeVocabulary(@NonNull Vocabulary vocabulary) {
        checkNotNull(vocabulary);
        mVocabularyRemoteDataSource.completeVocabulary(vocabulary);
        mVocabularyLocalDataSource.completeVocabulary(vocabulary);

        Vocabulary completedTask = new Vocabulary(vocabulary.getTitle(), vocabulary.getDescription(), vocabulary.getId(), true);

        // Do in memory cache update to keep the app UI up to date
        if (mCachedVocabulary == null) {
            mCachedVocabulary = new LinkedHashMap<>();
        }
        mCachedVocabulary.put(vocabulary.getId(), completedTask);
    }

    @Override
    public void completeVocabulary(@NonNull String vocabularyId) {
        checkNotNull(vocabularyId);
        Vocabulary taskWithId = getVocabularyWithId(vocabularyId);
        if (taskWithId != null) {
            completeVocabulary(taskWithId);
        }
    }

    @Override
    public void activateVocabulary(@NonNull Vocabulary vocabulary) {
        checkNotNull(vocabulary);
        mVocabularyRemoteDataSource.activateVocabulary(vocabulary);
        mVocabularyLocalDataSource.activateVocabulary(vocabulary);

        Vocabulary activeTask = new Vocabulary(vocabulary.getTitle(), vocabulary.getDescription(), vocabulary.getId());

        // Do in memory cache update to keep the app UI up to date
        if (mCachedVocabulary == null) {
            mCachedVocabulary = new LinkedHashMap<>();
        }
        mCachedVocabulary.put(vocabulary.getId(), activeTask);
    }

    @Override
    public void activateVocabulary(@NonNull String vocabularyId) {
        checkNotNull(vocabularyId);
        Vocabulary taskWithId = getVocabularyWithId(vocabularyId);
        if (taskWithId != null) {
            activateVocabulary(taskWithId);
        }
    }

    @Override
    public void clearCompletedVocabularys() {
        mVocabularyRemoteDataSource.clearCompletedVocabularys();
        mVocabularyLocalDataSource.clearCompletedVocabularys();

        // Do in memory cache update to keep the app UI up to date
        if (mCachedVocabulary == null) {
            mCachedVocabulary = new LinkedHashMap<>();
        }
        Iterator<Map.Entry<String, Vocabulary>> it = mCachedVocabulary.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Vocabulary> entry = it.next();
            if (entry.getValue().isCompleted()) {
                it.remove();
            }
        }
    }

    @Override
    public Flowable<Optional<Vocabulary>> getVocabulary(@NonNull final String vocabularyId) {
        checkNotNull(vocabularyId);

        final Vocabulary cachedTask = getVocabularyWithId(vocabularyId);

        // Respond immediately with cache if available
        if (cachedTask != null) {
            return Flowable.just(Optional.of(cachedTask));
        }

        // Load from server/persisted if needed.

        // Do in memory cache update to keep the app UI up to date
        if (mCachedVocabulary == null) {
            mCachedVocabulary = new LinkedHashMap<>();
        }

        // Is the task in the local data source? If not, query the network.
        Flowable<Optional<Vocabulary>> localTask = getVocabularyWithIdFromLocalRepository(vocabularyId);
        Flowable<Optional<Vocabulary>> remoteTask = mVocabularyLocalDataSource
                .getVocabulary(vocabularyId)
                .doOnNext(taskOptional -> {
                    if (taskOptional.isPresent()) {
                        Vocabulary task = taskOptional.get();
                        mVocabularyLocalDataSource.saveVocabulary(task);
                        mCachedVocabulary.put(task.getId(), task);
                    }
                });

        return Flowable.concat(localTask, remoteTask)
                .firstElement()
                .toFlowable();
    }

    @Override
    public void refreshVocabularys() {
        mCacheIsDirty = true;
    }

    @Override
    public void deleteAllVocabularys() {
        mVocabularyRemoteDataSource.deleteAllVocabularys();
        mVocabularyLocalDataSource.deleteAllVocabularys();

        if (mCachedVocabulary == null) {
            mCachedVocabulary = new LinkedHashMap<>();
        }
        mCachedVocabulary.clear();
    }

    @Override
    public void deleteVocabulary(@NonNull String vocabularyId) {
        mVocabularyRemoteDataSource.deleteVocabulary(checkNotNull(vocabularyId));
        mVocabularyLocalDataSource.deleteVocabulary(checkNotNull(vocabularyId));

        mCachedVocabulary.remove(vocabularyId);
    }

    @Nullable
    private Vocabulary getVocabularyWithId(@NonNull String id) {
        checkNotNull(id);
        if (mCachedVocabulary == null || mCachedVocabulary.isEmpty()) {
            return null;
        } else {
            return mCachedVocabulary.get(id);
        }
    }
    @NonNull
    Flowable<Optional<Vocabulary>> getVocabularyWithIdFromLocalRepository(@NonNull final String vocabularyId) {
        return mVocabularyLocalDataSource
                .getVocabulary(vocabularyId)
                .doOnNext(taskOptional -> {
                    if (taskOptional.isPresent()) {
                        mCachedVocabulary.put(vocabularyId, taskOptional.get());
                    }
                })
                .firstElement().toFlowable();
    }
}
