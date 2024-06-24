/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.gouv.tchap.features.roomprofile.settings.linkaccess.detail

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.session.Session

class TchapRoomLinkAccessBottomSheetViewModel @AssistedInject constructor(
        @Assisted initialState: TchapRoomLinkAccessBottomSheetState,
        session: Session
) : VectorViewModel<TchapRoomLinkAccessBottomSheetState, EmptyAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: TchapRoomLinkAccessBottomSheetState): TchapRoomLinkAccessBottomSheetViewModel
    }

    companion object : MavericksViewModelFactory<TchapRoomLinkAccessBottomSheetViewModel, TchapRoomLinkAccessBottomSheetState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: TchapRoomLinkAccessBottomSheetState): TchapRoomLinkAccessBottomSheetViewModel? {
            val fragment: TchapRoomLinkAccessBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.roomAliasBottomSheetViewModelFactory.create(state)
        }
    }

    init {
        setState {
            val permalink = if (MatrixPatterns.isRoomAlias(roomIdOrAlias)) {
                session.permalinkService().createPermalink(roomIdOrAlias)
            } else {
                session.permalinkService().createRoomPermalink(roomIdOrAlias)
            }
            copy(
                    matrixToLink = permalink
            )
        }
    }

    override fun handle(action: EmptyAction) {
        // No op
    }
}
