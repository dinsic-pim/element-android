/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
 *
 */

package fr.gouv.tchap.android.sdk.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Enum for [RoomJoinRulesContent] : https://gitlab.matrix.org/new-vector/internal/-/wikis/DINSIC#imvectorroomaccess_rules
 */
@JsonClass(generateAdapter = false)
enum class RoomAccessRules(val value: String) {
    @Json(name = "direct") DIRECT("direct"),
    @Json(name = "restricted") RESTRICTED("restricted"),
    @Json(name = "unrestricted") UNRESTRICTED("unrestricted")
}
