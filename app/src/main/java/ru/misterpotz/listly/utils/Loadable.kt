package ru.misterpotz.listly.utils

/**
 * Универсальная обёртка над состоянием загрузки.
 *
 * В общей структуре это вспомогательная UI-модель.
 * Reducer хранит её в State, а экран по ней решает, показывать загрузку, ошибку или контент.
 */
sealed class Loadable<T>(
    open val content: T?,
    open val error: Throwable? = null,
) {
    /** Состояние, в котором полезные данные уже получены. */
    data class Content<T>(override val content: T?) : Loadable<T>(content)

    /** Состояние ошибки с опциональным предыдущим контентом. */
    data class Error<T>(override val content: T? = null, override val error: Throwable?) :
        Loadable<T>(content, error)

    /** Состояние загрузки с опциональным уже известным контентом. */
    data class Loading<T>(override val content: T? = null) : Loadable<T>(content, null)

    val isContent
        get() = this is Content

    /** Возвращает контент или падает, если reducer/UI ошибся в предположении о состоянии. */
    fun requireContent() = requireNotNull(content)
    val isError
        get() = this is Error
    val isLoading
        get() = this is Loading
}

/** Быстрый helper: переводит данные или ошибку в Loadable. */
inline fun <reified T> T?.toLoadable(): Loadable<T> {
    return when {
        this is Throwable -> Loadable.Error(error = this)
        this == null -> Loadable.Loading(content = null)
        else -> Loadable.Content(this)
    }
}

/** Перегрузка для случаев, когда у нас уже есть Throwable. */
fun <T> Throwable?.toLoadable(): Loadable<T> {
    return Loadable.Error(null, this)
}
