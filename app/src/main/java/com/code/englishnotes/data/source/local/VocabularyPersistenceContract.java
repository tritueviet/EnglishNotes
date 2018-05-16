package com.code.englishnotes.data.source.local;

import android.provider.BaseColumns;

public class VocabularyPersistenceContract {
    private VocabularyPersistenceContract() {
    }

    public static abstract class VocabularyEntry implements BaseColumns {
        public static final String TABLE_NAME = "vocabulary";
        public static final String COLUMN_NAME_ENTRY_ID = "entryid";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_PRONOUNCE = "pronounce";
        public static final String COLUMN_NAME_COMPLETED = "completed";
    }
}
