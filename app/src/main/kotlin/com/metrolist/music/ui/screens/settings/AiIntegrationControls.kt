/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.ai.AiModelOption
import com.metrolist.music.ai.AiProvider
import com.metrolist.music.ai.AiRequestParameters
import com.metrolist.music.ai.AiRequestParametersKey
import com.metrolist.music.ai.AiServiceConfig
import com.metrolist.music.ai.AiTextService
import com.metrolist.music.ai.archiveAiProvider
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.component.IrideFolioSecondaryButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.TextFieldDialog
import com.metrolist.music.ui.component.irideFolioGestures
import com.metrolist.music.ui.component.rememberIrideFolioFlingBehavior
import com.metrolist.music.ui.theme.FolioShape
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** ArchiveTune's model discovery, API test and request editor adapted to Iride Folio settings. */
@Composable
internal fun AiIntegrationControls(
    provider: String,
    apiKey: String,
    endpoint: String,
    model: String,
    onSelectModel: (String) -> Unit,
) {
    val archiveProvider = archiveAiProvider(provider) ?: return
    if (archiveProvider == AiProvider.NONE) return
    var parameters by rememberPreference(AiRequestParametersKey, "")
    val config = AiServiceConfig(archiveProvider, apiKey, endpoint, model, parameters)
    val scope = rememberCoroutineScope()
    var models by remember(archiveProvider, apiKey, endpoint) { mutableStateOf<List<AiModelOption>>(emptyList()) }
    var showModels by remember(config) { mutableStateOf(false) }
    var showParameters by remember(config) { mutableStateOf(false) }
    var isFetching by remember(config) { mutableStateOf(false) }
    var isTesting by remember(config) { mutableStateOf(false) }
    var connected by remember(config) { mutableStateOf<Boolean?>(null) }
    var error by remember(config) { mutableStateOf<String?>(null) }
    var showError by remember(config) { mutableStateOf(false) }
    var fetchJob by remember(config) { mutableStateOf<Job?>(null) }
    var testJob by remember(config) { mutableStateOf<Job?>(null) }
    val testFailed = stringResource(R.string.ai_api_test_failed)
    val fetchFailed = stringResource(R.string.ai_model_fetch_failed)
    val emptyModels = stringResource(R.string.ai_models_empty)

    DisposableEffect(config) {
        onDispose {
            fetchJob?.cancel()
            testJob?.cancel()
        }
    }

    fun displayError(throwable: Exception, fallback: String): String =
        (throwable.localizedMessage?.takeIf(String::isNotBlank) ?: fallback)
            .let { if (apiKey.isBlank()) it else it.replace(apiKey, "••••") }

    if (showParameters) {
        TextFieldDialog(
            title = { Text(stringResource(R.string.ai_request_parameters)) },
            initialTextFieldValue = TextFieldValue(parameters),
            singleLine = false,
            maxLines = 10,
            isInputValid = { runCatching { AiRequestParameters.normalize(it) }.isSuccess },
            onDone = { parameters = AiRequestParameters.normalize(it) },
            onDismiss = { showParameters = false },
            extraContent = {
                Text(
                    stringResource(R.string.ai_request_parameters_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textSecondary,
                )
                Text(
                    stringResource(R.string.ai_request_parameters_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            },
        )
    }

    if (showModels) {
        AiModelPicker(
            models = models,
            selectedModel = model,
            onSelect = {
                showModels = false
                onSelectModel(it)
            },
            onDismiss = { showModels = false },
        )
    }

    if (showError && error != null) {
        val errorScrollState = rememberScrollState()
        DefaultDialog(
            onDismiss = { showError = false },
            title = { Text(stringResource(R.string.ai_api_test_failed)) },
            buttons = {
                IrideFolioPrimaryButton(onClick = { showError = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            SelectionContainer(
                Modifier.weight(1f, fill = false).heightIn(max = 240.dp)
                    .irideFolioGestures(errorScrollState)
                    .verticalScroll(errorScrollState, overscrollEffect = null),
            ) {
                Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    Material3SettingsGroup(
        items = buildList {
            if (archiveProvider != AiProvider.CUSTOM) {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.sync),
                        title = { Text(stringResource(R.string.ai_fetch_models)) },
                        description = {
                            Text(if (isFetching) stringResource(R.string.ai_fetching_models) else model)
                        },
                        enabled = apiKey.isNotBlank() && !isFetching,
                        trailingContent = {
                            if (isFetching) CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.textPrimary,
                                strokeWidth = 2.dp,
                            )
                        },
                        onClick = {
                            isFetching = true
                            error = null
                            fetchJob = scope.launch {
                                try {
                                    models = AiTextService.fetchModels(config)
                                    if (models.isEmpty()) error = emptyModels else showModels = true
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (failure: Exception) {
                                    error = displayError(failure, fetchFailed)
                                } finally {
                                    isFetching = false
                                }
                            }
                        },
                    ),
                )
            }
            if (archiveProvider != AiProvider.GEMINI) {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text(stringResource(R.string.ai_request_parameters)) },
                        description = {
                            Text(
                                parameters.ifBlank { stringResource(R.string.ai_request_parameters_description) },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = { showParameters = true },
                    ),
                )
            }
            add(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.sync),
                    title = { Text(stringResource(R.string.ai_test_api)) },
                    description = {
                        Text(
                            when {
                                isTesting -> stringResource(R.string.ai_api_testing)
                                connected == true -> stringResource(R.string.ai_api_connected)
                                connected == false -> stringResource(R.string.ai_api_test_failed)
                                else -> stringResource(R.string.ai_api_not_tested)
                            },
                        )
                        error?.let { details ->
                            Text(
                                details.take(140),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clickable { showError = true },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    enabled = config.canCallApi && model.isNotBlank() && !isTesting,
                    trailingContent = {
                        if (isTesting) CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.textPrimary,
                            strokeWidth = 2.dp,
                        )
                    },
                    onClick = {
                        isTesting = true
                        error = null
                        testJob = scope.launch {
                            try {
                                AiTextService.test(config)
                                connected = true
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                connected = false
                                error = displayError(failure, testFailed)
                                showError = true
                            } finally {
                                isTesting = false
                            }
                        }
                    },
                ),
            )
        },
    )
    Spacer(Modifier.height(27.dp))
}

@Composable
private fun AiModelPicker(
    models: List<AiModelOption>,
    selectedModel: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val filtered = remember(models, query) {
        models.filter { it.id.contains(query.trim(), ignoreCase = true) || it.displayName.contains(query.trim(), ignoreCase = true) }
    }
    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.ai_model)) },
        buttons = {
            IrideFolioPrimaryButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    ) {
        Column(Modifier.fillMaxWidth().weight(1f, fill = false)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text(stringResource(R.string.ai_model_search)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = IBMPlexMonoFontFamily),
                shape = FolioShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.textPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.textPrimary,
                    cursorColor = MaterialTheme.colorScheme.textPrimary,
                    focusedBorderColor = MaterialTheme.colorScheme.textPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.strokeHairline,
                    focusedLabelColor = MaterialTheme.colorScheme.textSecondary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.textSecondary,
                ),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).irideFolioGestures(listState),
                state = listState,
                overscrollEffect = null,
                flingBehavior = rememberIrideFolioFlingBehavior(),
            ) {
                items(filtered, key = { it.id }) { option ->
                    Column(
                        Modifier.fillMaxWidth().clickable { onSelect(option.id) }.padding(vertical = 12.dp),
                    ) {
                        Text(
                            option.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = IBMPlexMonoFontFamily),
                            color = MaterialTheme.colorScheme.textPrimary,
                            fontWeight = if (option.id == selectedModel) androidx.compose.ui.text.font.FontWeight.Bold else null,
                        )
                        if (option.displayName != option.id) Text(
                            option.id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textSecondary,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.strokeHairline)
                }
            }
        }
    }
}

@Composable
internal fun AiSecretKeyDialog(
    initialValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }
    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.ai_api_key)) },
        buttons = {
            IrideFolioSecondaryButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
            IrideFolioPrimaryButton(onClick = { onSave(value) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = IBMPlexMonoFontFamily),
            shape = FolioShape(6.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.textPrimary,
                unfocusedTextColor = MaterialTheme.colorScheme.textPrimary,
                cursorColor = MaterialTheme.colorScheme.textPrimary,
                focusedBorderColor = MaterialTheme.colorScheme.textPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.strokeHairline,
            ),
        )
    }
}
