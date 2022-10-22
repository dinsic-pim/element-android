/*
 * Copyright (c) 2022 New Vector Ltd
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

<<<<<<<< HEAD:vector/src/test/java/im/vector/app/test/fakes/FakeTchapGetPlatformTask.kt
package im.vector.app.test.fakes

import fr.gouv.tchap.features.platform.GetPlatformResult
import fr.gouv.tchap.features.platform.TchapGetPlatformTask
import io.mockk.coEvery
import io.mockk.mockk

class FakeTchapGetPlatformTask {

    val instance = mockk<TchapGetPlatformTask>()

    fun givenGetPlatformResult(getPlatformResult: GetPlatformResult) {
        coEvery { instance.execute(any()) } returns getPlatformResult
    }
========
package im.vector.app.features.settings.legals

import android.content.Context

interface FlavorLegals {
    fun hasThirdPartyNotices(): Boolean
    fun navigateToThirdPartyNotices(context: Context)
>>>>>>>> v1.4.36:vector/src/main/java/im/vector/app/features/settings/legals/FlavorLegals.kt
}
