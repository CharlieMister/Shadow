package me.lucko.shadow

/**
 * Simple nullability markers matching the original shadow annotations.
 *
 * They are only used for documentation/compatibility and have no runtime effect.
 */
@kotlin.annotation.Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE
)
@Retention(AnnotationRetention.SOURCE)
annotation class NonNull

@kotlin.annotation.Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE
)
@Retention(AnnotationRetention.SOURCE)
annotation class Nullable
