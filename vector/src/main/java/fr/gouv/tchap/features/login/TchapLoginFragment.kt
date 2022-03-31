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

package fr.gouv.tchap.features.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import fr.gouv.tchap.android.sdk.internal.services.threepidplatformdiscover.model.Platform
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.isEmail
import im.vector.app.databinding.FragmentTchapLoginBinding
import im.vector.app.features.login.AbstractLoginFragment
import im.vector.app.features.login.LoginAction
import im.vector.app.features.login.LoginViewEvents
import im.vector.app.features.login.LoginViewState
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.failure.isInvalidPassword
import javax.inject.Inject

/**
 * In this screen:
 * - the user is asked for email and password to sign in to a homeserver.
 * - He also can reset his password
 */
class TchapLoginFragment @Inject constructor() : AbstractLoginFragment<FragmentTchapLoginBinding>() {

    private lateinit var login: String
    private lateinit var password: String

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTchapLoginBinding {
        return FragmentTchapLoginBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.groupToolbar)
        views.groupToolbar.setTitle(R.string.tchap_connection_title)

        setupForgottenPasswordButton()

        loginViewModel.observeViewEvents {
            when (it) {
                LoginViewEvents.OnLoginFlowRetrieved ->
                    loginViewModel.handle(LoginAction.LoginOrRegister(login, password, getString(R.string.login_default_session_public_name)))
                is LoginViewEvents.OnHomeServerRetrieved -> {
                    val homeServerUrl = resources.getString(R.string.server_url_prefix) + it.hs
                    loginViewModel.handle(LoginAction.UpdateHomeServer(homeServerUrl))
                }
                else                                 ->
                    // This is handled by the Activity
                    Unit
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

    private fun setupForgottenPasswordButton() {
        views.forgetPasswordButton.setOnClickListener { forgetPasswordClicked() }
    }

    private fun submit() {
        cleanupUi()

        login = views.tchapLoginField.text.toString()
        password = views.tchapPasswordField.text.toString()

        // This can be called by the IME action, so deal with empty cases
        var error = 0
        if (login.isEmpty() || !login.isEmail()) {
            views.tchapLoginFieldTil.error = getString(R.string.auth_invalid_email)
            error++
        }
        if (password.isEmpty()) {
            views.tchapPasswordFieldTil.error = getString(R.string.error_empty_field_your_password)
            error++
        }

        if (error == 0) {
            loginViewModel.handle(LoginAction.RetrieveHomeServer(login))
        }
    }

    private fun cleanupUi() {
//        views.loginSubmit.hideKeyboard()
        views.tchapLoginFieldTil.error = null
        views.tchapPasswordFieldTil.error = null
    }

    private fun updateHomeServer(platform: Platform) {
        loginViewModel.handle(LoginAction.UpdateHomeServer(getString(R.string.server_url_prefix) + platform.hs))
    }

    private fun forgetPasswordClicked() {
        loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnForgetPasswordClicked))
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetLogin)
    }

    override fun onError(throwable: Throwable) {
        views.tchapLoginFieldTil.error = errorFormatter.toHumanReadable(throwable)
    }

    override fun updateWithState(state: LoginViewState) {
        when (state.asyncLoginAction) {
            is Loading -> Unit
            is Fail    -> {
                val error = state.asyncLoginAction.error
                if (error is Failure.ServerError &&
                        error.error.code == MatrixError.M_FORBIDDEN &&
                        error.error.message.isEmpty()) {
                    // Login with email, but email unknown
                    views.tchapLoginFieldTil.error = getString(R.string.auth_invalid_login_param)
                } else {
                    if (error.isInvalidPassword() && spaceInPassword()) {
                        views.tchapPasswordFieldTil.error = getString(R.string.auth_invalid_login_param_space_in_password)
                    } else {
                        views.tchapPasswordFieldTil.error = errorFormatter.toHumanReadable(error)
                    }
                }
            }
            // Success is handled by the LoginActivity
            is Success -> Unit
        }
    }

    /**
     * Detect if password ends or starts with spaces
     */
    private fun spaceInPassword() = views.tchapPasswordField.text.toString().let { it.trim() != it }
}
