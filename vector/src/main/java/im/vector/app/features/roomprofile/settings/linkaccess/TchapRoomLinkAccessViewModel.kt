/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.roomprofile.settings.linkaccess

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomCanonicalAliasContent
import org.matrix.android.sdk.rx.mapOptional
import org.matrix.android.sdk.rx.rx
import org.matrix.android.sdk.rx.unwrap

class TchapRoomLinkAccessViewModel @AssistedInject constructor(
        @Assisted initialState: TchapRoomLinkAccessState,
        session: Session
) : VectorViewModel<TchapRoomLinkAccessState, TchapRoomLinkAccessAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: TchapRoomLinkAccessState): TchapRoomLinkAccessViewModel
    }

    companion object : MvRxViewModelFactory<TchapRoomLinkAccessViewModel, TchapRoomLinkAccessState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: TchapRoomLinkAccessState): TchapRoomLinkAccessViewModel {
            val fragment: TchapRoomLinkAccessFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeRoomSummary()
        observeRoomCanonicalAlias()
    }

    private fun observeRoomCanonicalAlias() {
        room.rx()
                .liveStateEvent(EventType.STATE_ROOM_CANONICAL_ALIAS, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomCanonicalAliasContent>() }
                .unwrap()
                .subscribe {
                    setState {
                        copy(canonicalAlias = it.canonicalAlias)
                    }
                }
                .disposeOnClear()
    }

    override fun handle(action: TchapRoomLinkAccessAction) {
        when (action) {
            is TchapRoomLinkAccessAction.SetIsEnabled -> handleSetIsEnabled(action)
        }.exhaustive
    }

    private fun observeRoomSummary() {
        room.rx().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    copy(
                            roomSummary = async
                    )
                }
    }

    private fun handleSetIsEnabled(action: TchapRoomLinkAccessAction.SetIsEnabled) {
        setState { copy(isLinkAccessEnabled = action.isEnabled) }
    }
}
