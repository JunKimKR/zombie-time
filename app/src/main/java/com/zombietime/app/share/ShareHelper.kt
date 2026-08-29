package com.zombietime.app.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ShareHelper {

    private const val IG_PACKAGE = "com.instagram.android"

    /**
     * 인스타그램 스토리로 바로 공유.
     * 인스타가 없거나 스토리 인텐트를 못 받으면 일반 공유 시트로 넘어간다.
     */
    fun shareToInstagramStory(ctx: Context, uri: Uri) {
        val story = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(uri, "image/png")
            putExtra("interactive_asset_uri", uri)
            putExtra("source_application", ctx.packageName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage(IG_PACKAGE)
        }

        try {
            ctx.grantUriPermission(IG_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
        }

        val canHandle = try {
            ctx.packageManager.resolveActivity(story, 0) != null
        } catch (e: Exception) {
            false
        }

        if (canHandle) {
            try {
                ctx.startActivity(story)
                return
            } catch (e: ActivityNotFoundException) {
            } catch (e: SecurityException) {
            }
        }
        shareChooser(ctx, uri, "인스타그램이 없어서 공유 시트를 열었어요")
    }

    /** 일반 공유 시트 (인스타 피드/스토리, 카톡 등 아무 데나) */
    fun shareChooser(ctx: Context, uri: Uri, toast: String? = null) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "오늘의 좀비 리포트 🧟 #좀비타임")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "오늘의 브리핑 공유")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            if (toast != null) Toast.makeText(ctx, toast, Toast.LENGTH_SHORT).show()
            ctx.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(ctx, "공유할 수 있는 앱이 없어요", Toast.LENGTH_SHORT).show()
        }
    }
}
