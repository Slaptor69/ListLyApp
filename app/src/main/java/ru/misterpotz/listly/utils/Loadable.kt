package ru.misterpotz.listly.utils
sealed class Loadable<T>(
    open val content: T?,
    open val error: Throwable? = null,
) {
    data class Content<T>(override val content: T?) : Loadable<T>(content)
    data class Error<T>(override val content: T? = null, override val error: Throwable?) :
        Loadable<T>(content, error)
    data class Loading<T>(override val content: T? = null) : Loadable<T>(content, null)

    val isContent
        get() = this is Content
    fun requireContent() = requireNotNull(content)
    val isError
        get() = this is Error
    val isLoading
        get() = this is Loading
}
inline fun <reified T> T?.toLoadable(): Loadable<T> {
    return when {
        this is Throwable -> Loadable.Error(error = this)
        this == null -> Loadable.Loading(content = null)
        else -> Loadable.Content(this)
    }
}
fun <T> Throwable?.toLoadable(): Loadable<T> {
    return Loadable.Error(null, this)
}
