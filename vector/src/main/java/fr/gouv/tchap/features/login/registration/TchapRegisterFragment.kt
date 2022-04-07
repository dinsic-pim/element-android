/*
 * Copyright 2021 New Vector Ltd
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

package fr.gouv.tchap.features.login.registration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.gouv.tchap.core.utils.TchapUtils
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.isEmail
import im.vector.app.databinding.FragmentTchapRegisterBinding
import im.vector.app.features.login.AbstractLoginFragment
import im.vector.app.features.login.LoginAction
import im.vector.app.features.login.LoginViewEvents
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.api.auth.registration.Stage
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.failure.is401
import javax.inject.Inject

/**
 * In this screen:
 * In signin mode:
 * - the user is asked for login (or email) and password to sign in to a homeserver.
 * - He also can reset his password
 * In signup mode:
 * - the user is asked for login and password
 */
class TchapRegisterFragment @Inject constructor() : AbstractLoginFragment<FragmentTchapRegisterBinding>() {

    private lateinit var login: String
    private lateinit var password: String

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTchapRegisterBinding {
        return FragmentTchapRegisterBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.toolbar)
        views.toolbar.setTitle(R.string.tchap_register_title)

        loginViewModel.observeViewEvents { loginViewEvents ->
            when (loginViewEvents) {
                LoginViewEvents.OnLoginFlowRetrieved      ->
                    loginViewModel.handle(LoginAction.LoginOrRegister(login, password, getString(R.string.login_default_session_public_name)))
                is LoginViewEvents.RegistrationFlowResult -> {
                    // Result from registration request when the account password is set.
                    // Email stage is mandatory at this time and another stage should not happen.
                    val emailStage = loginViewEvents.flowResult.missingStages.firstOrNull { it.mandatory && it is Stage.Email }

                    if (emailStage != null) {
                        loginViewModel.handle(LoginAction.AddThreePid(RegisterThreePid.Email(login)))
                    } else {
                        MaterialAlertDialogBuilder(requireActivity())
                                .setTitle(R.string.dialog_title_error)
                                .setMessage(R.string.tchap_login_error_unable_register)
                                .setPositiveButton(R.string.ok, null)
                                .show()
                        Unit
                    }
                }
                is LoginViewEvents.OnHomeServerRetrieved  -> updateHomeServer(loginViewEvents.hs)
                else                                      -> Unit // This is handled by the Activity
            }.exhaustive
        }
    }

    override fun getMenuRes() = R.menu.tchap_menu_next

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_next -> {
                submit()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun submit() {
        cleanupUi()

        login = views.tchapRegisterEmail.text.toString()
        password = views.tchapRegisterPassword.text.toString()

        // This can be called by the IME action, so deal with empty cases
        var error = 0
        if (login.isEmpty() || !login.isEmail()) {
            views.tchapRegisterEmail.error = getString(R.string.auth_invalid_email)
            error++
        }
        if (password.isEmpty()) {
            views.tchapRegisterPassword.error = getString(R.string.error_empty_field_choose_password)
            error++
        }

        if (password != views.tchapRegisterPasswordConfirm.text.toString()) {
            views.tchapRegisterPasswordConfirm.error = getString(R.string.tchap_auth_password_dont_match)
            error++
        }

        if (error == 0) {
            loginViewModel.handle(LoginAction.RetrieveHomeServer(login))
        }
    }

    private fun cleanupUi() {
//        views.loginSubmit.hideKeyboard()
        views.tchapRegisterEmail.error = null
        views.tchapRegisterPassword.error = null
    }

    private fun updateHomeServer(hs: String) {
        if (TchapUtils.isExternalTchapServer(hs)) {
            MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.tchap_register_warning_for_external_title)
                    .setCancelable(false)
                    .setMessage(R.string.tchap_register_warning_for_external)
                    .setPositiveButton(R.string.tchap_register_warning_for_external_proceed) { _, _ ->
                        loginViewModel.handle(LoginAction.UpdateHomeServer(getString(R.string.server_url_prefix) + hs))
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
        } else {
            loginViewModel.handle(LoginAction.UpdateHomeServer(getString(R.string.server_url_prefix) + hs))
        }
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetLogin)
    }

    override fun onError(throwable: Throwable) {
        val passwordErrors = setOf(MatrixError.M_WEAK_PASSWORD, MatrixError.M_PASSWORD_TOO_SHORT, MatrixError.M_PASSWORD_NO_UPPERCASE,
                MatrixError.M_PASSWORD_NO_DIGIT, MatrixError.M_PASSWORD_NO_SYMBOL, MatrixError.M_PASSWORD_NO_LOWERCASE, MatrixError.M_PASSWORD_IN_DICTIONARY)

        if (throwable is Failure.ServerError &&
                throwable.error.code in passwordErrors) {
            // Show password error in the password field
            views.tchapRegisterPassword.error = errorFormatter.toHumanReadable(throwable)
        } else if (throwable.is401()) {
            // This is normal use case, we go to the mail waiting screen
            loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnSendEmailSuccess(loginViewModel.currentThreePid ?: "")))
        } else {
            views.tchapRegisterEmail.error = errorFormatter.toHumanReadable(throwable)
        }
    }
}
