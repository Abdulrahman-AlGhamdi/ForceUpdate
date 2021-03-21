package com.android.forceupdate.common

import java.io.File

sealed class Event {
    class OnDownloadProgress(val progress: Int) : Event()
    class OnDownloadCompleting(val localFile: File) : Event()
    object OnCanceled : Event()
}