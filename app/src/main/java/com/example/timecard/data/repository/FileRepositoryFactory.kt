package com.example.timecard.data.repository

import android.content.Context
import android.net.Uri
import java.io.File

object FileRepositoryFactory {
    fun create(context: Context, uri: Uri): FileRepository {
        return if ("file" == uri.scheme) {
            DirectFileRepository(File(uri.path!!))
        } else {
            SafFileRepository(context, uri)
        }
    }
}
