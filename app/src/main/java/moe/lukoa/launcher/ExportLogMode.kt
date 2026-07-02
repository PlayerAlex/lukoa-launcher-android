package moe.lukoa.launcher

enum class ExportLogMode(
    val includeTermux: Boolean,
    val includeApp: Boolean,
) {
    TermuxOnly(includeTermux = true, includeApp = false),
    AppOnly(includeTermux = false, includeApp = true),
    Both(includeTermux = true, includeApp = true),
}
