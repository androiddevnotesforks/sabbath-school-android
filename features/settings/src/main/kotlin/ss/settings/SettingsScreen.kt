/*
 * Copyright (c) 2023. Adventech <info@adventech.io>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ss.settings

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import app.ss.design.compose.extensions.list.ListEntity
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import kotlinx.parcelize.Parcelize
import ss.circuit.helpers.factory.SettingsPresenterFactory
import ss.circuit.helpers.factory.SettingsUiFactory
import ss.settings.ui.SettingsUiScreen
import javax.inject.Inject
import javax.inject.Singleton

@Parcelize
object SettingsScreen : Screen, Parcelable {

    internal sealed interface Event : CircuitUiEvent {
        object NavBack : Event
        object OverlayDismiss : Event
        object AccountDeleteConfirmed : Event
        data class SetReminderTime(val hour: Int, val minute: Int): Event
    }

    @Immutable
    internal data class State(
        val entities: List<ListEntity>,
        val overlay: Overlay?,
        val eventSick: (Event) -> Unit,
    ) : CircuitUiState

    internal sealed interface Overlay {
        @Immutable
        data class SelectReminderTime(val hour: Int, val minute: Int): Overlay
        object ConfirmDeleteAccount: Overlay
    }
}

@Singleton
internal class SettingsUiFactoryImpl @Inject constructor() : SettingsUiFactory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is SettingsScreen -> ui<SettingsScreen.State> { state, modifier ->
                SettingsUiScreen(
                    state,
                    modifier
                )
            }
            else -> null
        }
    }
}

@Singleton
internal class SettingsPresenterFactoryImpl @Inject constructor(
    private val presenter: SettingsPresenter.Factory,
) : SettingsPresenterFactory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            is SettingsScreen -> presenter.create(navigator)
            else -> null
        }
    }
}
