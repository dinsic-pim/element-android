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

package fr.gouv.tchap.features.home.contact.list

import im.vector.app.core.platform.VectorViewEvents

/**
 * Transient events for invite users to room screen
 */
sealed class TchapContactListViewEvents : VectorViewEvents {
    object OpenSearch : TchapContactListViewEvents()
    object CancelSearch : TchapContactListViewEvents()
    data class InviteIgnoredForDiscoveredUser(val email: String) : TchapContactListViewEvents()
    data class InviteIgnoredForUnauthorizedEmail(val email: String) : TchapContactListViewEvents()
    data class InviteIgnoredForExistingRoom(val email: String) : TchapContactListViewEvents()
    object InviteNoTchapUserByEmail : TchapContactListViewEvents()
}