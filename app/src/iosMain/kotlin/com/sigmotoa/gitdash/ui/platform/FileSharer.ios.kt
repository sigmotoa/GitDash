package com.sigmotoa.gitdash.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberFileSharer(): FileSharer = remember {
    FileSharer { bytes, fileName, _ ->
        if (bytes.isEmpty()) return@FileSharer
        val path = (NSTemporaryDirectory() as NSString).stringByAppendingPathComponent(fileName)
        val url = NSURL.fileURLWithPath(path)
        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        data.writeToURL(url, atomically = true)

        val activityController = UIActivityViewController(
            activityItems = listOf(url),
            applicationActivities = null,
        )
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(activityController, animated = true, completion = null)
    }
}
