package com.mana.launcher

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ManaDiagnosticFileProviderTest {
    @Test fun diagnosticExportIsReadableThroughScopedContentUri() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "mana-diagnostics").apply { mkdirs() }
        val file = File(directory, "instrumentation-check.md").apply {
            writeText("# MANA diagnostic test", Charsets.UTF_8)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        assertEquals("content", uri.scheme)
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        assertNotNull(content)
        assertEquals("# MANA diagnostic test", content)
        file.delete()
    }
}
