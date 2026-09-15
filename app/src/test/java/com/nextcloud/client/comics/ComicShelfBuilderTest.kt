/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.comics

import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComicShelfBuilderTest {

    private val tree = mutableMapOf<String, List<OCFile>>()
    private val progress = mutableMapOf<String, Int>()

    private fun image(path: String): OCFile = OCFile(path).apply { mimeType = "image/jpeg" }

    private fun folder(path: String): OCFile = OCFile(path).apply { mimeType = "DIR" }

    private fun builder() = ComicShelfBuilder(
        folderContent = { tree[it.remotePath] ?: emptyList() },
        lastReadPage = { progress[it.remotePath] ?: 0 }
    )

    @Test
    fun `folders with images are comics and groupers are skipped`() {
        val library = folder("/Comics/")
        tree["/Comics/"] = listOf(folder("/Comics/Marvel/"), folder("/Comics/Zorro/"))
        tree["/Comics/Marvel/"] = listOf(folder("/Comics/Marvel/Hulk 2/"), folder("/Comics/Marvel/Hulk 10/"))
        tree["/Comics/Marvel/Hulk 2/"] = listOf(
            image("/Comics/Marvel/Hulk 2/2.jpg"),
            image("/Comics/Marvel/Hulk 2/1.jpg")
        )
        tree["/Comics/Marvel/Hulk 10/"] = listOf(image("/Comics/Marvel/Hulk 10/1.jpg"))
        tree["/Comics/Zorro/"] = listOf(image("/Comics/Zorro/a.jpg"))

        val comics = builder().build(library)

        assertEquals(listOf("Hulk 2", "Hulk 10", "Zorro"), comics.map { it.title })
        assertEquals("1.jpg", comics.first().cover.fileName)
        assertEquals(2, comics.first().pageCount)
    }

    @Test
    fun `images directly inside the library form a comic named after it`() {
        val library = folder("/Comics/")
        tree["/Comics/"] = listOf(image("/Comics/page.jpg"), folder("/Comics/Empty/"))

        val comics = builder().build(library)

        assertEquals(listOf("Comics"), comics.map { it.title })
    }

    @Test
    fun `stored progress is clamped to the existing pages`() {
        val library = folder("/Comics/")
        tree["/Comics/"] = listOf(folder("/Comics/Short/"))
        tree["/Comics/Short/"] = listOf(image("/Comics/Short/1.jpg"), image("/Comics/Short/2.jpg"))
        progress["/Comics/Short/"] = 40

        val comic = builder().build(library).single()

        assertEquals(1, comic.lastReadPage)
        assertTrue(comic.isFinished)
        assertTrue(comic.isStarted)
    }

    @Test
    fun `encrypted folders are never entered`() {
        val library = folder("/Comics/")
        tree["/Comics/"] = listOf(folder("/Comics/Secret/").apply { isEncrypted = true })
        tree["/Comics/Secret/"] = listOf(image("/Comics/Secret/1.jpg"))

        assertTrue(builder().build(library).isEmpty())
    }

    @Test
    fun `one folder level lists groups before comics and counts the comics below each group`() {
        val library = folder("/Comics/")
        tree["/Comics/"] = listOf(folder("/Comics/Zorro/"), folder("/Comics/Marvel/"), folder("/Comics/Empty/"))
        tree["/Comics/Marvel/"] = listOf(folder("/Comics/Marvel/Hulk 2/"), folder("/Comics/Marvel/Hulk 10/"))
        tree["/Comics/Marvel/Hulk 2/"] = listOf(image("/Comics/Marvel/Hulk 2/1.jpg"))
        tree["/Comics/Marvel/Hulk 10/"] = listOf(image("/Comics/Marvel/Hulk 10/1.jpg"))
        tree["/Comics/Zorro/"] = listOf(image("/Comics/Zorro/a.jpg"))

        val items = builder().buildLevel(library)

        assertEquals(listOf("Marvel", "Zorro"), items.map { it.folder.fileName })
        val group = items.first() as ComicGroup
        assertEquals(2, group.comicCount)
        assertEquals("/Comics/Marvel/Hulk 2/1.jpg", group.cover.remotePath)
        assertTrue(items.last() is Comic)
    }
}
