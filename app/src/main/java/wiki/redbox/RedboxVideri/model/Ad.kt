package wiki.redbox.RedboxVideri.model

data class Ad(
    val id: Int,
    val path: String,
    val durationMs: Int,
    val isVideo: Boolean
)