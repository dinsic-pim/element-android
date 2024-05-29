/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.settings

import android.os.Bundle
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import fr.gouv.tchap.core.utils.TchapUtils
import im.vector.app.R
import im.vector.app.core.preference.VectorPreference
import im.vector.app.core.utils.FirstThrottler
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.features.VectorFeatures
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.displayname.getBestName
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

@AndroidEntryPoint
class VectorSettingsRootFragment :
        VectorSettingsBaseFragment() {

    @Inject lateinit var vectorFeatures: VectorFeatures

    override var titleRes: Int = R.string.title_activity_settings
    override val preferenceXmlRes = R.xml.vector_settings_root

    private val firstThrottler = FirstThrottler(1000)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.Settings
    }

    override fun bindPref() {
        tintIcons()

        // TCHAP Manage new FAQ entry
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_HELP_PREFERENCE_KEY)!!
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (firstThrottler.canHandle() is FirstThrottler.CanHandlerResult.Yes) {
                openUrlInChromeCustomTab(requireContext(), null, VectorSettingsUrls.HELP)
            }
            false
        }

        // TCHAP Manage labs entry.
        val myUserDisplayName = session.getUserOrDefault(session.myUserId).toMatrixItem().getBestName()
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_LABS_PREFERENCE_KEY)!!
                .isVisible = vectorFeatures.tchapIsLabsVisible(TchapUtils.getDomainFromDisplayName(myUserDisplayName))
    }

    private fun tintIcons() {
        for (i in 0 until preferenceScreen.preferenceCount) {
            (preferenceScreen.getPreference(i) as? VectorPreference)?.let { it.tintIcon = true }
        }
    }
}
