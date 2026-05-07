/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.sticker

import im.vector.app.features.home.room.detail.RoomDetailViewEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class StickerPickerActionHandler @Inject constructor(private val session: Session) {

    suspend fun handle(): RoomDetailViewEvents = withContext(Dispatchers.Default) {
        // SVOi: всегда открываем собственный пикер с встроенными+пользовательскими стикерами
        RoomDetailViewEvents.OpenSvoiStickerPicker(session.myUserId)
    }
}
