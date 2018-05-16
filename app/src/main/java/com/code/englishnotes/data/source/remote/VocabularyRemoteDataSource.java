/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.code.englishnotes.data.source.remote;

import android.support.annotation.NonNull;

import com.code.englishnotes.data.model.Vocabulary;
import com.code.englishnotes.data.source.VocabularyDataSource;
import com.google.common.base.Optional;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;

/**
 * Implementation of the data source that adds a latency simulating network.
 */
public class VocabularyRemoteDataSource implements VocabularyDataSource {

    private static VocabularyRemoteDataSource INSTANCE;

    private static final int SERVICE_LATENCY_IN_MILLIS = 5000;

    private final static Map<String, Vocabulary> TASKS_SERVICE_DATA;

    static {
        TASKS_SERVICE_DATA = new LinkedHashMap<>(2);
        addTask("Build tower in Pisa", "Ground looks good, no foundation work required.");
        addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!");
    }

    public static VocabularyRemoteDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VocabularyRemoteDataSource();
        }
        return INSTANCE;
    }

    // Prevent direct instantiation.
    private VocabularyRemoteDataSource() {
    }

    private static void addTask(String title, String description) {
        Vocabulary newTask = new Vocabulary(title, description);
        TASKS_SERVICE_DATA.put(newTask.getId(), newTask);
    }

    @Override
    public Flowable<List<Vocabulary>> getVocabularys() {
        return Flowable
                .fromIterable(TASKS_SERVICE_DATA.values())
                .delay(SERVICE_LATENCY_IN_MILLIS, TimeUnit.MILLISECONDS)
                .toList()
                .toFlowable();
    }

    @Override
    public Flowable<Optional<Vocabulary>> getVocabulary(@NonNull String taskId) {
        final Vocabulary task = TASKS_SERVICE_DATA.get(taskId);
        if (task != null) {
            return Flowable.just(Optional.of(task)).delay(SERVICE_LATENCY_IN_MILLIS, TimeUnit.MILLISECONDS);
        } else {
            return Flowable.empty();
        }
    }

    @Override
    public void saveVocabulary(@NonNull Vocabulary vocabulary) {
        TASKS_SERVICE_DATA.put(vocabulary.getId(), vocabulary);
    }

    @Override
    public void completeVocabulary(@NonNull Vocabulary task) {
        Vocabulary completedTask = new Vocabulary(task.getTitle(), task.getDescription(), task.getId(), true);
        TASKS_SERVICE_DATA.put(task.getId(), completedTask);
    }

    @Override
    public void completeVocabulary(@NonNull String vocabularyId) {
        // Not required for the remote data source because the {@link TasksRepository} handles
        // converting from a {@code taskId} to a {@link task} using its cached data.
    }

    @Override
    public void activateVocabulary(@NonNull Vocabulary task) {
        Vocabulary activeTask = new Vocabulary(task.getTitle(), task.getDescription(), task.getId());
        TASKS_SERVICE_DATA.put(task.getId(), activeTask);
    }

    @Override
    public void activateVocabulary(@NonNull String taskId) {
        // Not required for the remote data source because the {@link TasksRepository} handles
        // converting from a {@code taskId} to a {@link task} using its cached data.
    }

    @Override
    public void clearCompletedVocabularys() {
        Iterator<Map.Entry<String, Vocabulary>> it = TASKS_SERVICE_DATA.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Vocabulary> entry = it.next();
            if (entry.getValue().isCompleted()) {
                it.remove();
            }
        }
    }

    @Override
    public void refreshVocabularys() {
        // Not required because the {@link TasksRepository} handles the logic of refreshing the
        // tasks from all the available data sources.
    }

    @Override
    public void deleteAllVocabularys() {
        TASKS_SERVICE_DATA.clear();
    }

    @Override
    public void deleteVocabulary(@NonNull String taskId) {
        TASKS_SERVICE_DATA.remove(taskId);
    }
}
