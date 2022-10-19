/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.store.db.mapper

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.internal.crypto.store.db.model.MyDeviceLastSeenInfoEntity

private const val A_DEVICE_ID = "device-id"
private const val AN_IP_ADDRESS = "ip-address"
private const val A_TIMESTAMP = 123L
private const val A_DISPLAY_NAME = "display-name"

class MyDeviceLastSeenInfoEntityMapperTest {

    private val myDeviceLastSeenInfoEntityMapper = MyDeviceLastSeenInfoEntityMapper()

    @Test
    fun `given an entity when mapping to model then all fields are correctly mapped`() {
        val entity = MyDeviceLastSeenInfoEntity(
                deviceId = A_DEVICE_ID,
                lastSeenIp = AN_IP_ADDRESS,
                lastSeenTs = A_TIMESTAMP,
                displayName = A_DISPLAY_NAME
        )
        val expectedDeviceInfo = DeviceInfo(
                deviceId = A_DEVICE_ID,
                lastSeenIp = AN_IP_ADDRESS,
                lastSeenTs = A_TIMESTAMP,
                displayName = A_DISPLAY_NAME
        )

        val deviceInfo = myDeviceLastSeenInfoEntityMapper.map(entity)

        deviceInfo shouldBeEqualTo expectedDeviceInfo
    }
}
