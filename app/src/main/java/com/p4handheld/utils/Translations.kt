package com.p4handheld.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

object Translations {

    operator fun get(context: Context, resourceId: Int): String {
        return TranslationManager.getInstance(context)[resourceId]
    }

    @Composable
    operator fun get(resourceId: Int): String {
        val context = LocalContext.current
        return TranslationManager.getInstance(context)[resourceId]
    }

    fun format(context: Context, resourceId: Int, vararg formatArgs: Any): String {
        val translatedString = TranslationManager.getInstance(context)[resourceId]
        return if (formatArgs.isNotEmpty()) {
            String.format(translatedString, *formatArgs)
        } else {
            translatedString
        }
    }

    @Composable
    fun format(resourceId: Int, vararg formatArgs: Any): String {
        val context = LocalContext.current
        return format(context, resourceId, *formatArgs)
    }
}
