/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.roomprofile.settings

import androidx.core.net.toFile
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import fr.gouv.tchap.android.sdk.api.session.events.model.TchapEventType
import fr.gouv.tchap.android.sdk.api.session.room.model.RoomAccessRules
import fr.gouv.tchap.android.sdk.api.session.room.model.RoomAccessRulesContent
import fr.gouv.tchap.core.utils.RoomUtils
import fr.gouv.tchap.core.utils.TchapRoomType
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.room.model.RoomAvatarContent
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomGuestAccessContent
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibilityContent
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.mapOptional
import org.matrix.android.sdk.flow.unwrap

class RoomSettingsViewModel @AssistedInject constructor(@Assisted initialState: RoomSettingsViewState,
                                                        private val vectorPreferences: VectorPreferences,
                                                        private val session: Session) :
        VectorViewModel<RoomSettingsViewState, RoomSettingsAction, RoomSettingsViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomSettingsViewModel, RoomSettingsViewState> {
        override fun create(initialState: RoomSettingsViewState): RoomSettingsViewModel
    }

    companion object : MavericksViewModelFactory<RoomSettingsViewModel, RoomSettingsViewState> by hiltMavericksViewModelFactory()

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeRoomSummary()
        observeRoomHistoryVisibility()
        observeJoinRule()
        observeGuestAccess()
        observeRoomAvatar()
        observeState()

        val homeServerCapabilities = session.homeServerCapabilitiesService().getHomeServerCapabilities()
        val canUseRestricted = homeServerCapabilities
                .isFeatureSupported(HomeServerCapabilities.ROOM_CAP_RESTRICTED, room.roomVersionService().getRoomVersion())

        val restrictedSupport = homeServerCapabilities.isFeatureSupported(HomeServerCapabilities.ROOM_CAP_RESTRICTED)
        val couldUpgradeToRestricted = restrictedSupport == HomeServerCapabilities.RoomCapabilitySupport.SUPPORTED

        setState {
            copy(
                    supportsRestricted = canUseRestricted,
                    canUpgradeToRestricted = couldUpgradeToRestricted
            )
        }
    }

    private fun fetchRoomDirectoryVisibility() {
        setState {
            copy(
                    roomDirectoryVisibility = Loading()
            )
        }
        viewModelScope.launch {
            runCatching {
                session.roomDirectoryService().getRoomDirectoryVisibility(room.roomId)
            }.fold(
                    {
                        setState {
                            copy(
                                    roomDirectoryVisibility = Success(it)
                            )
                        }
                    },
                    {
                        setState {
                            copy(
                                    roomDirectoryVisibility = Fail(it)
                            )
                        }
                    }
            )
        }
    }

    private fun observeState() {
        onEach(
                RoomSettingsViewState::avatarAction,
                RoomSettingsViewState::newName,
                RoomSettingsViewState::newTopic,
                RoomSettingsViewState::newHistoryVisibility,
                RoomSettingsViewState::newRoomJoinRules,
                RoomSettingsViewState::roomSummary) { avatarAction,
                                                      newName,
                                                      newTopic,
                                                      newHistoryVisibility,
                                                      newJoinRule,
                                                      asyncSummary ->
            val summary = asyncSummary()
            setState {
                copy(
                        showSaveAction = avatarAction !is RoomSettingsViewState.AvatarAction.None ||
                                summary?.name != newName ||
                                summary?.topic != newTopic ||
                                (newHistoryVisibility != null && newHistoryVisibility != currentHistoryVisibility) ||
                                newJoinRule.hasChanged()
                )
            }
        }
    }

    private fun observeRoomSummary() {
        room.flow().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    val roomSummary = async.invoke()

                    if (roomSummary?.let { RoomUtils.getRoomType(it) } == TchapRoomType.UNKNOWN) fetchRoomDirectoryVisibility()

                    copy(
                            roomSummary = async,
                            newName = roomSummary?.name,
                            newTopic = roomSummary?.topic
                    )
                }

        val powerLevelsContentLive = PowerLevelsFlowFactory(room).createFlow()

        powerLevelsContentLive
                .onEach {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    val permissions = RoomSettingsViewState.ActionPermissions(
                            canChangeAvatar = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_AVATAR),
                            canChangeName = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_NAME),
                            canChangeTopic = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_TOPIC),
                            canChangeHistoryVisibility = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true,
                                    EventType.STATE_ROOM_HISTORY_VISIBILITY),
                            canChangeJoinRule = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true,
                                    EventType.STATE_ROOM_JOIN_RULES) &&
                                    powerLevelsHelper.isUserAllowedToSend(session.myUserId, true,
                                            EventType.STATE_ROOM_GUEST_ACCESS),
                            canChangeCanonicalAlias = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_CANONICAL_ALIAS),
                            canAddChildren = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true,
                                    EventType.STATE_SPACE_CHILD),
                            canRemoveFromRoomsDirectory = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_JOIN_RULES) &&
                                    powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_CANONICAL_ALIAS) &&
                                    powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_ENCRYPTION) &&
                                    powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_HISTORY_VISIBILITY),
                            canChangeRoomAccessRules = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, TchapEventType.STATE_ROOM_ACCESS_RULES)
                    )
                    setState {
                        copy(actionPermissions = permissions)
                    }
                }.launchIn(viewModelScope)
    }

    private fun observeRoomHistoryVisibility() {
        room.flow()
                .liveStateEvent(EventType.STATE_ROOM_HISTORY_VISIBILITY, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomHistoryVisibilityContent>() }
                .unwrap()
                .mapNotNull { it.historyVisibility }
                .setOnEach {
                    copy(currentHistoryVisibility = it)
                }
    }

    private fun observeJoinRule() {
        room.flow()
                .liveStateEvent(EventType.STATE_ROOM_JOIN_RULES, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomJoinRulesContent>() }
                .unwrap()
                .mapNotNull { it.joinRules }
                .setOnEach {
                    copy(currentRoomJoinRules = it)
                }
    }

    private fun observeGuestAccess() {
        room.flow()
                .liveStateEvent(EventType.STATE_ROOM_GUEST_ACCESS, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomGuestAccessContent>() }
                .unwrap()
                .mapNotNull { it.guestAccess }
                .setOnEach {
                    copy(currentGuestAccess = it)
                }
    }

    /**
     * We do not want to use the fallback avatar url, which can be the other user avatar, or the current user avatar.
     */
    private fun observeRoomAvatar() {
        room.flow()
                .liveStateEvent(EventType.STATE_ROOM_AVATAR, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomAvatarContent>() }
                .unwrap()
                .setOnEach {
                    copy(currentRoomAvatarUrl = it.avatarUrl)
                }
    }

    override fun handle(action: RoomSettingsAction) {
        when (action) {
            is RoomSettingsAction.SetAvatarAction          -> handleSetAvatarAction(action)
            is RoomSettingsAction.SetRoomName              -> setState { copy(newName = action.newName) }
            is RoomSettingsAction.SetRoomTopic             -> setState { copy(newTopic = action.newTopic) }
            is RoomSettingsAction.SetRoomHistoryVisibility -> setState { copy(newHistoryVisibility = action.visibility) }
            is RoomSettingsAction.SetRoomJoinRule          -> handleSetRoomJoinRule(action)
            is RoomSettingsAction.SetRoomGuestAccess       -> handleSetGuestAccess(action)
            RoomSettingsAction.RemoveFromRoomsDirectory    -> handleRemoveFromRoomsDirectory()
            RoomSettingsAction.AllowExternalUsersToJoin    -> handleAllowExternalUsersToJoin()
            is RoomSettingsAction.Save                     -> saveSettings()
            is RoomSettingsAction.Cancel                   -> cancel()
        }
    }

    private fun handleSetRoomJoinRule(action: RoomSettingsAction.SetRoomJoinRule) = withState { state ->
        setState {
            copy(newRoomJoinRules = RoomSettingsViewState.NewJoinRule(
                    newJoinRules = action.roomJoinRule.takeIf { it != state.currentRoomJoinRules },
                    newGuestAccess = state.newRoomJoinRules.newGuestAccess.takeIf { it != state.currentGuestAccess }
            ))
        }
    }

    private fun handleSetGuestAccess(action: RoomSettingsAction.SetRoomGuestAccess) = withState { state ->
        setState {
            copy(newRoomJoinRules = RoomSettingsViewState.NewJoinRule(
                    newJoinRules = state.newRoomJoinRules.newJoinRules.takeIf { it != state.currentRoomJoinRules },
                    newGuestAccess = action.guestAccess.takeIf { it != state.currentGuestAccess }
            ))
        }
    }

    private fun handleSetAvatarAction(action: RoomSettingsAction.SetAvatarAction) {
        setState {
            deletePendingAvatar(this)
            copy(avatarAction = action.avatarAction)
        }
    }

    /**
     * Several changes are required:
     * The new members can access only on invite
     * The encryption has to be enabled by default
     * The room will become private
     * The history visibility value is replaced with invited
     */
    private fun handleRemoveFromRoomsDirectory() {
        session.coroutineScope.launch {
            updateLoadingState(isLoading = true)
            try {
                // Update first the joinrule to INVITE.
                room.stateService().setJoinRuleInviteOnly()
                // Turn on the encryption in this room (if this is not already done).
                if (!room.roomCryptoService().isEncrypted()) room.roomCryptoService().enableEncryption()
                // Remove the room from the room directory.
                session.roomDirectoryService().setRoomDirectoryVisibility(room.roomId, RoomDirectoryVisibility.PRIVATE)
            } catch (failure: Throwable) {
                updateLoadingState(isLoading = false)
                _viewEvents.post(RoomSettingsViewEvents.Failure(failure))
            }
            // Update history visibility.
            tryOrNull { room.stateService().updateHistoryReadability(RoomHistoryVisibility.INVITED) }
            updateLoadingState(isLoading = false)
        }
    }

    private fun handleAllowExternalUsersToJoin() {
        session.coroutineScope.launch {
            updateLoadingState(isLoading = true)
            try {
                room.stateService().sendStateEvent(
                        eventType = TchapEventType.STATE_ROOM_ACCESS_RULES,
                        stateKey = "",
                        body = RoomAccessRulesContent(RoomAccessRules.UNRESTRICTED.value).toContent()
                )
            } catch (failure: Throwable) {
                updateLoadingState(isLoading = false)
                _viewEvents.post(RoomSettingsViewEvents.Failure(failure))
            }
            updateLoadingState(isLoading = false)
        }
    }

    private fun deletePendingAvatar(state: RoomSettingsViewState) {
        // Maybe delete the pending avatar
        (state.avatarAction as? RoomSettingsViewState.AvatarAction.UpdateAvatar)
                ?.let { tryOrNull { it.newAvatarUri.toFile().delete() } }
    }

    private fun cancel() {
        withState { deletePendingAvatar(it) }

        _viewEvents.post(RoomSettingsViewEvents.GoBack)
    }

    private fun saveSettings() = withState { state ->
        val operationList = mutableListOf<suspend () -> Unit>()

        val summary = state.roomSummary.invoke()

        when (val avatarAction = state.avatarAction) {
            RoomSettingsViewState.AvatarAction.None            -> Unit
            RoomSettingsViewState.AvatarAction.DeleteAvatar    -> {
                operationList.add { room.stateService().deleteAvatar() }
            }
            is RoomSettingsViewState.AvatarAction.UpdateAvatar -> {
                operationList.add { room.stateService().updateAvatar(avatarAction.newAvatarUri, avatarAction.newAvatarFileName) }
            }
        }
        if (summary?.name != state.newName) {
            operationList.add { room.stateService().updateName(state.newName ?: "") }
        }
        if (summary?.topic != state.newTopic) {
            operationList.add { room.stateService().updateTopic(state.newTopic ?: "") }
        }

        if (state.newHistoryVisibility != null) {
            operationList.add { room.stateService().updateHistoryReadability(state.newHistoryVisibility) }
        }

        if (state.newRoomJoinRules.hasChanged()) {
            operationList.add { room.stateService().updateJoinRule(state.newRoomJoinRules.newJoinRules, state.newRoomJoinRules.newGuestAccess) }
        }
        viewModelScope.launch {
            updateLoadingState(isLoading = true)
            try {
                for (operation in operationList) {
                    operation.invoke()
                }
                setState {
                    deletePendingAvatar(this)
                    copy(
                            avatarAction = RoomSettingsViewState.AvatarAction.None,
                            newHistoryVisibility = null,
                            newRoomJoinRules = RoomSettingsViewState.NewJoinRule()
                    )
                }
                _viewEvents.post(RoomSettingsViewEvents.Success)
            } catch (failure: Throwable) {
                _viewEvents.post(RoomSettingsViewEvents.Failure(failure))
            } finally {
                updateLoadingState(isLoading = false)
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        setState {
            copy(isLoading = isLoading)
        }
    }
}
