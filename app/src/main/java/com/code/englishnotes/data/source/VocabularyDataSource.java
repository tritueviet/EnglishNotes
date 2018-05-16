package com.code.englishnotes.data.source;

import android.support.annotation.NonNull;

import com.code.englishnotes.data.model.Vocabulary;
import com.google.common.base.Optional;

import java.util.List;

import io.reactivex.Flowable;

public interface VocabularyDataSource {

    Flowable<List<Vocabulary>> getVocabularys();

    Flowable<Optional<Vocabulary>> getVocabulary(@NonNull String vocabularyId);

    void saveVocabulary(@NonNull Vocabulary vocabulary);

    void completeVocabulary(@NonNull Vocabulary vocabulary);

    void completeVocabulary(@NonNull String vocabularyId);

    void activateVocabulary(@NonNull Vocabulary vocabulary);

    void activateVocabulary(@NonNull String vocabularyId);

    void clearCompletedVocabularys();

    void refreshVocabularys();

    void deleteAllVocabularys();

    void deleteVocabulary(@NonNull String vocabularyId);
}
