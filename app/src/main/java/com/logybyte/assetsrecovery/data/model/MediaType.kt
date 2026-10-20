package com.logybyte.assetsrecovery.data.model

enum class MediaType {
    IMAGE,
    VIDEO
}

enum class MediaStorageType {
    ACTIVE_GALLERY,
    MEDIA_TRASH,
    HIDDEN_ORPHANED,
    APP_CACHE
}

enum class RecoveryStatus {
    NONE,
    RECOVERED,
    FAILED,
    SKIPPED
}

enum class FilterType {
    ALL,
    RECOVERABLE_ONLY,
    TRASHED,
    HIDDEN,
    ACTIVE
}

enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    SIZE_DESC,
    SIZE_ASC,
    NAME_ASC,
    NAME_DESC
}
