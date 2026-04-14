package com.example.timecard.util

object PathSanitizer {
    /**
     * Sanitizes a name or filename by removing all path separators and ".." sequences.
     * This ensures the resulting string refers only to a single file or directory level.
     */
    fun sanitize(input: String): String {
        return input.replace("\\", "/")
            .replace("../", "")
            .replace("/", "")
            .replace("..", "")
    }

    /**
     * Sanitizes a relative path by removing ".." sequences and ensuring it doesn't start with a separator.
     * Allows internal separators.
     */
    fun sanitizePath(path: String): String {
        var sanitized = path.replace("\\", "/")
            .replace("../", "")
            .replace("..", "")
        while (sanitized.startsWith("/")) {
            sanitized = sanitized.substring(1)
        }
        return sanitized
    }
}
