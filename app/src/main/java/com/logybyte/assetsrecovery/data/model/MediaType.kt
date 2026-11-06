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
<<<<<<< HEAD

enum class RecoveryConfidence {
    HIGH,
    MEDIUM,
    LOW,
    CORRUPTED
}
=======
>>>>>>> 3d5e19e31eb8fcb5e39ab175672a81e2e22ffe53
