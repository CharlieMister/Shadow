package me.lucko.shadow

import java.lang.reflect.Method
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * A [TargetResolver] for fields to match common "getter" and "setter" method patterns.
 */
internal class FuzzyFieldTargetResolver private constructor() : TargetResolver {
    override fun lookupField(
        shadowMethod: Method,
        shadowClass: Class<out Shadow>,
        targetClass: Class<*>
    ): Optional<String> {
        val methodName = shadowMethod.getName()
        var matcher: Matcher = GETTER_PATTERN.matcher(methodName)
        if (matcher.matches()) {
            return Optional.of(
                methodName.substring(3, 4).lowercase(Locale.getDefault()) + methodName.substring(4)
            )
        }

        matcher = GETTER_IS_PATTERN.matcher(methodName)
        if (matcher.matches()) {
            return Optional.of(
                methodName.substring(2, 3).lowercase(Locale.getDefault()) + methodName.substring(3)
            )
        }

        matcher = SETTER_PATTERN.matcher(methodName)
        if (matcher.matches()) {
            return Optional.of(
                methodName.substring(3, 4).lowercase(Locale.getDefault()) + methodName.substring(4)
            )
        }

        return Optional.empty()
    }

    companion object {
        val INSTANCE: FuzzyFieldTargetResolver = FuzzyFieldTargetResolver()

        private val GETTER_PATTERN: Pattern = Pattern.compile("(get)[A-Z].*")
        private val GETTER_IS_PATTERN: Pattern = Pattern.compile("(is)[A-Z].*")
        private val SETTER_PATTERN: Pattern = Pattern.compile("(set)[A-Z].*")
    }
}
