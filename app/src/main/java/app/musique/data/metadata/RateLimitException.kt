package app.musique.data.metadata

class RateLimitException(
    message: String,
    val retryAfterSeconds: Long? = null
) : Exception(message)

