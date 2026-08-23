package com.metrolist.music.ui.screens.settings
import com.metrolist.music.ui.component.IrideSwitch

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.DynamicThemeKey
import com.metrolist.music.constants.EnableDynamicIconKey
import com.metrolist.music.constants.EnableHighRefreshRateKey
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.AlbumTopGradientKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.PureBlackMiniPlayerKey
import com.metrolist.music.constants.SelectedThemeColorKey
import com.metrolist.music.constants.IrideAnimationsKey
import com.metrolist.music.constants.CompactTopNavigationBarKey
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.SettingsBackTopBar
import com.metrolist.music.ui.theme.DefaultThemeColor
import com.metrolist.music.ui.theme.IrideTheme
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.utils.IconUtils
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch

data class ThemePalette(
    val nameRes: Int,
    val seedColor: Color
)

val PaletteColors = listOf(
    ThemePalette(R.string.palette_dynamic, Color.Transparent), // Sentinel for System/Dynamic colors
    ThemePalette(R.string.palette_crimson, Color(0xFFEC5464)), // Slightly shifted from DefaultThemeColor (0xFFED5564) to avoid conflict
    ThemePalette(R.string.palette_rose, Color(0xFFD81B60)),
    ThemePalette(R.string.palette_purple, Color(0xFF8E24AA)),
    ThemePalette(R.string.palette_deep_purple, Color(0xFF5E35B1)),
    ThemePalette(R.string.palette_indigo, Color(0xFF3949AB)),
    ThemePalette(R.string.palette_blue, Color(0xFF1E88E5)),
    ThemePalette(R.string.palette_sky_blue, Color(0xFF039BE5)),
    ThemePalette(R.string.palette_cyan, Color(0xFF00ACC1)),
    ThemePalette(R.string.palette_teal, Color(0xFF00897B)),
    ThemePalette(R.string.palette_green, Color(0xFF43A047)),
    ThemePalette(R.string.palette_light_green, Color(0xFF7CB342)),
    ThemePalette(R.string.palette_lime, Color(0xFFC0CA33)),
    ThemePalette(R.string.palette_yellow, Color(0xFFFDD835)),
    ThemePalette(R.string.palette_amber, Color(0xFFFFB300)),
    ThemePalette(R.string.palette_orange, Color(0xFFFB8C00)),
    ThemePalette(R.string.palette_deep_orange, Color(0xFFF4511E)),
    ThemePalette(R.string.palette_brown, Color(0xFF6D4C41)),
    ThemePalette(R.string.palette_grey, Color(0xFF757575)),
    ThemePalette(R.string.palette_blue_grey, Color(0xFF546E7A)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    navController: NavController,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, DarkMode.ON)
    val (pureBlack, onPureBlackChangeRaw) = rememberPreference(PureBlackKey, defaultValue = false)
    val (_, onPureBlackMiniPlayerChange) = rememberPreference(
        PureBlackMiniPlayerKey,
        defaultValue = false
    )
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(DynamicThemeKey, defaultValue = true)
    val (enableDynamicIcon, onEnableDynamicIconChange) =
        rememberPreference(EnableDynamicIconKey, defaultValue = true)
    val (enableHighRefreshRate, onEnableHighRefreshRateChange) =
        rememberPreference(EnableHighRefreshRateKey, defaultValue = true)

    val coroutineScope = rememberCoroutineScope()

    val onPureBlackChange: (Boolean) -> Unit = { enabled ->
        onPureBlackChangeRaw(enabled)
        onPureBlackMiniPlayerChange(enabled)
    }
    val (mainTopGradient, onMainTopGradientChange) = rememberPreference(MainTopGradientKey, defaultValue = true)
    val (albumTopGradient, onAlbumTopGradientChange) = rememberPreference(AlbumTopGradientKey, defaultValue = true)
    val (selectedThemeColorInt, onSelectedThemeColorChange) = rememberPreference(
        SelectedThemeColorKey,
        DefaultThemeColor.toArgb()
    )

    val selectedThemeColor = Color(selectedThemeColorInt)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val handleColorSelection: (Color) -> Unit = { color ->
        onSelectedThemeColorChange(color.toArgb())
        val isDynamicColor = color == DefaultThemeColor
        onDynamicThemeChange(isDynamicColor)
    }

    fun handleIconChange(enabled: Boolean) {
        onEnableDynamicIconChange(enabled)
        IconUtils.setIcon(activity, enabled)
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Icon updated, restart to apply",
                actionLabel = "Restart"
            )
            if (result == SnackbarResult.ActionPerformed) {
                val packageManager = activity.packageManager
                val intent = packageManager.getLaunchIntentForPackage(activity.packageName)
                val componentName = intent?.component
                val mainIntent = Intent.makeRestartActivityTask(componentName)
                activity.startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }
        }
    }

    val isUsingCustomColor = selectedThemeColorInt != DefaultThemeColor.toArgb()

    if (isLandscape) {
        LandscapeThemeLayout(
            innerPadding = PaddingValues(0.dp),
            darkMode = darkMode,
            onDarkModeChange = onDarkModeChange,
            pureBlack = pureBlack,
            onPureBlackChange = onPureBlackChange,
            selectedThemeColor = selectedThemeColor,
            onSelectedThemeColorChange = handleColorSelection,
            enableDynamicIcon = enableDynamicIcon,
            onEnableDynamicIconChange = { handleIconChange(it) },
            enableHighRefreshRate = enableHighRefreshRate,
            onEnableHighRefreshRateChange = onEnableHighRefreshRateChange,
            dynamicTheme = dynamicTheme,
            onDynamicThemeChange = onDynamicThemeChange,
            isUsingCustomColor = isUsingCustomColor,
            mainTopGradient = mainTopGradient,
            onMainTopGradientChange = onMainTopGradientChange,
            albumTopGradient = albumTopGradient,
            onAlbumTopGradientChange = onAlbumTopGradientChange
        )
    } else {
        PortraitThemeLayout(
            innerPadding = PaddingValues(0.dp),
            darkMode = darkMode,
            onDarkModeChange = onDarkModeChange,
            pureBlack = pureBlack,
            onPureBlackChange = onPureBlackChange,
            selectedThemeColor = selectedThemeColor,
            onSelectedThemeColorChange = handleColorSelection,
            enableDynamicIcon = enableDynamicIcon,
            onEnableDynamicIconChange = { handleIconChange(it) },
            enableHighRefreshRate = enableHighRefreshRate,
            onEnableHighRefreshRateChange = onEnableHighRefreshRateChange,
            dynamicTheme = dynamicTheme,
            onDynamicThemeChange = onDynamicThemeChange,
            isUsingCustomColor = isUsingCustomColor,
            mainTopGradient = mainTopGradient,
            onMainTopGradientChange = onMainTopGradientChange,
            albumTopGradient = albumTopGradient,
            onAlbumTopGradientChange = onAlbumTopGradientChange
        )
    }

    SettingsBackTopBar(
        title = stringResource(R.string.settings_theme),
        navController = navController,
    )
}

@Composable
fun PortraitThemeLayout(
    innerPadding: PaddingValues,
    darkMode: DarkMode,
    onDarkModeChange: (DarkMode) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    selectedThemeColor: Color,
    onSelectedThemeColorChange: (Color) -> Unit,
    enableDynamicIcon: Boolean = false,
    onEnableDynamicIconChange: (Boolean) -> Unit = {},
    enableHighRefreshRate: Boolean = true,
    onEnableHighRefreshRateChange: (Boolean) -> Unit = {},
    dynamicTheme: Boolean = false,
    onDynamicThemeChange: (Boolean) -> Unit = {},
    isUsingCustomColor: Boolean = false,
    mainTopGradient: Boolean = false,
    onMainTopGradientChange: (Boolean) -> Unit = {},
    albumTopGradient: Boolean = false,
    onAlbumTopGradientChange: (Boolean) -> Unit = {}
) {
    // Fix: this Column used to size itself with two `weight(1f)` Spacers around a fixed-height
    // mockup box, relying on the Column always having enough vertical room to lay everything out.
    // In New Iride UI, enabling the "curtain" player (see MainActivity's curtainMode/curtainActive)
    // shrinks the Scaffold's actual height by roughly `bottomInset + 76dp` whenever a track is
    // loaded, which — combined with this screen hardcoding `innerPadding = PaddingValues(0.dp)` and
    // never seeing that shrink — pushed the controls (mode circles / palette / switches) below the
    // visible area with nothing to scroll to them, i.e. "the whole theme panel disappears". Landscape
    // was never affected because its controls column already has `verticalScroll`. Fixed by making
    // this Column scrollable too and swapping the `weight(1f)` Spacers (which don't work inside a
    // scrollable Column — unbounded height) for fixed spacing.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        ThemeControls(
            darkMode = darkMode,
            onDarkModeChange = onDarkModeChange,
            pureBlack = pureBlack,
            onPureBlackChange = onPureBlackChange,
            selectedThemeColor = selectedThemeColor,
            onSelectedThemeColorChange = onSelectedThemeColorChange,
            enableDynamicIcon = enableDynamicIcon,
            onEnableDynamicIconChange = onEnableDynamicIconChange,
            enableHighRefreshRate = enableHighRefreshRate,
            onEnableHighRefreshRateChange = onEnableHighRefreshRateChange,
            dynamicTheme = dynamicTheme,
            onDynamicThemeChange = onDynamicThemeChange,
            isUsingCustomColor = isUsingCustomColor,
            mainTopGradient = mainTopGradient,
            onMainTopGradientChange = onMainTopGradientChange,
            albumTopGradient = albumTopGradient,
            onAlbumTopGradientChange = onAlbumTopGradientChange
        )

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun LandscapeThemeLayout(
    innerPadding: PaddingValues,
    darkMode: DarkMode,
    onDarkModeChange: (DarkMode) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    selectedThemeColor: Color,
    onSelectedThemeColorChange: (Color) -> Unit,
    enableDynamicIcon: Boolean = false,
    onEnableDynamicIconChange: (Boolean) -> Unit = {},
    enableHighRefreshRate: Boolean = true,
    onEnableHighRefreshRateChange: (Boolean) -> Unit = {},
    dynamicTheme: Boolean = false,
    onDynamicThemeChange: (Boolean) -> Unit = {},
    isUsingCustomColor: Boolean = false,
    mainTopGradient: Boolean = false,
    onMainTopGradientChange: (Boolean) -> Unit = {},
    albumTopGradient: Boolean = false,
    onAlbumTopGradientChange: (Boolean) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top))
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 16.dp
                )
        ) {
            ThemeControls(
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange,
                pureBlack = pureBlack,
                onPureBlackChange = onPureBlackChange,
                selectedThemeColor = selectedThemeColor,
                onSelectedThemeColorChange = onSelectedThemeColorChange,
                enableDynamicIcon = enableDynamicIcon,
                onEnableDynamicIconChange = onEnableDynamicIconChange,
                enableHighRefreshRate = enableHighRefreshRate,
                onEnableHighRefreshRateChange = onEnableHighRefreshRateChange,
                dynamicTheme = dynamicTheme,
                onDynamicThemeChange = onDynamicThemeChange,
                isUsingCustomColor = isUsingCustomColor,
                mainTopGradient = mainTopGradient,
                onMainTopGradientChange = onMainTopGradientChange,
                albumTopGradient = albumTopGradient,
                onAlbumTopGradientChange = onAlbumTopGradientChange
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ThemeControls(
    darkMode: DarkMode,
    onDarkModeChange: (DarkMode) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    selectedThemeColor: Color,
    onSelectedThemeColorChange: (Color) -> Unit,
    enableDynamicIcon: Boolean = false,
    onEnableDynamicIconChange: (Boolean) -> Unit = {},
    enableHighRefreshRate: Boolean = true,
    onEnableHighRefreshRateChange: (Boolean) -> Unit = {},
    dynamicTheme: Boolean = false,
    onDynamicThemeChange: (Boolean) -> Unit = {},
    isUsingCustomColor: Boolean = false,
    mainTopGradient: Boolean = false,
    onMainTopGradientChange: (Boolean) -> Unit = {},
    albumTopGradient: Boolean = false,
    onAlbumTopGradientChange: (Boolean) -> Unit = {}
) {
    IrideThemeControls(
        darkMode = darkMode,
        onDarkModeChange = onDarkModeChange,
        pureBlack = pureBlack,
        onPureBlackChange = onPureBlackChange,
        selectedThemeColor = selectedThemeColor,
        onSelectedThemeColorChange = onSelectedThemeColorChange,
        enableDynamicIcon = enableDynamicIcon,
        onEnableDynamicIconChange = onEnableDynamicIconChange,
        enableHighRefreshRate = enableHighRefreshRate,
        onEnableHighRefreshRateChange = onEnableHighRefreshRateChange,
        dynamicTheme = dynamicTheme,
        onDynamicThemeChange = onDynamicThemeChange,
        isUsingCustomColor = isUsingCustomColor,
        mainTopGradient = mainTopGradient,
        onMainTopGradientChange = onMainTopGradientChange,
        albumTopGradient = albumTopGradient,
        onAlbumTopGradientChange = onAlbumTopGradientChange
    )
}

@Composable
private fun IrideThemeControls(
    darkMode: DarkMode,
    onDarkModeChange: (DarkMode) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    selectedThemeColor: Color,
    onSelectedThemeColorChange: (Color) -> Unit,
    enableDynamicIcon: Boolean,
    onEnableDynamicIconChange: (Boolean) -> Unit,
    enableHighRefreshRate: Boolean,
    onEnableHighRefreshRateChange: (Boolean) -> Unit,
    dynamicTheme: Boolean,
    onDynamicThemeChange: (Boolean) -> Unit,
    isUsingCustomColor: Boolean,
    mainTopGradient: Boolean,
    onMainTopGradientChange: (Boolean) -> Unit,
    albumTopGradient: Boolean,
    onAlbumTopGradientChange: (Boolean) -> Unit
) {
    // ── New Iride Ui / Main+Album screens top gradient toggles ───────────
    Spacer(modifier = Modifier.height(16.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Read here rather than threaded down for the same reason as irideAnimations below: only
        // this row and TopNavigationBar itself (AppNavigation.kt) need it.
        val (compactTopBar, onCompactTopBarChange) =
            rememberPreference(CompactTopNavigationBarKey, defaultValue = true)
        IrideThemeToggleRow(
            title = stringResource(R.string.compact_top_navigation_bar),
            description = stringResource(R.string.compact_top_navigation_bar_desc),
            checked = compactTopBar,
            onCheckedChange = onCompactTopBarChange
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 1.dp)
        // Escape hatch for the Iride motion layer (see ui/utils/IrideMotion.kt). Read here rather
        // than threaded down from ThemeScreen: it's only ever used by this row and by the
        // composables that animate, both of which read the DataStore directly.
        val (irideAnimations, onIrideAnimationsChange) =
            rememberPreference(IrideAnimationsKey, defaultValue = true)
        IrideThemeToggleRow(
            title = stringResource(R.string.iride_animations),
            description = stringResource(R.string.iride_animations_desc),
            checked = irideAnimations,
            onCheckedChange = onIrideAnimationsChange
        )
        // Auto-hide return panel row removed from UI (still defaults ON via PlayerAutoHideTopPanelKey,
        // read directly by the curtain player in MainActivity — no settings control anymore).
        HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 1.dp)
        IrideThemeToggleRow(
            title = stringResource(R.string.main_top_gradient),
            description = stringResource(R.string.main_top_gradient_desc),
            checked = mainTopGradient,
            onCheckedChange = onMainTopGradientChange
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 1.dp)
        IrideThemeToggleRow(
            title = stringResource(R.string.album_top_gradient),
            description = stringResource(R.string.album_top_gradient_desc),
            checked = albumTopGradient,
            onCheckedChange = onAlbumTopGradientChange
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // ── Palette / system toggles ───────────────────────────────────────────
    // Theme Mode circle picker, Dynamic Icon and Dynamic Theme rows are hidden per settings
    // cleanup — dark/pure-black mode still applies via its stored default, only the picker UI
    // is gone. Only High Refresh Rate stays visible below.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IrideThemeSectionTitle(stringResource(R.string.color_palette))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(PaletteColors) { palette ->
                    val isDynamicPalette = palette.seedColor == Color.Transparent
                    val isSelected = if (isDynamicPalette) {
                        selectedThemeColor == DefaultThemeColor
                    } else {
                        selectedThemeColor == palette.seedColor
                    }

                    PaletteItem(
                        palette = palette,
                        isSelected = isSelected,
                        onClick = {
                            val colorToSave = if (isDynamicPalette) DefaultThemeColor else palette.seedColor
                            onSelectedThemeColorChange(colorToSave)
                        }
                    )
                }
            }
        }

        Column {
            IrideThemeSectionTitle(stringResource(R.string.settings_theme))
            Spacer(modifier = Modifier.height(4.dp))

            IrideThemeToggleRow(
                title = stringResource(R.string.enable_high_refresh_rate),
                checked = enableHighRefreshRate,
                onCheckedChange = onEnableHighRefreshRateChange
            )
        }
    }
}

@Composable
private fun IrideThemeSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = SpaceMonoFontFamily,
            letterSpacing = (-0.1).sp,
        ),
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}

@Composable
private fun IrideThemeToggleRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 15.sp,
                    letterSpacing = (-0.1).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            )
            description?.let { desc ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        IrideSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = {
                Icon(
                    painter = painterResource(if (checked) R.drawable.check else R.drawable.close),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        )
    }
}

@Composable
fun PaletteItem(
    palette: ThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    
    val colorScheme = rememberDynamicColorScheme(
        seedColor = palette.seedColor,
        isDark = isSystemDark,
        style = PaletteStyle.TonalSpot
    )
    
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 48.dp * 0.25f else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cornerRadius"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "borderWidth"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    
    val paletteName = stringResource(palette.nameRes)
    val contentDesc = stringResource(R.string.cd_palette_item, paletteName)
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(
                        width = borderWidth,
                        color = MaterialTheme.colorScheme.inversePrimary,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .semantics {
                contentDescription = contentDesc
            }
    ) {
        if (palette.seedColor == Color.Transparent) {
            // Draw Dynamic/System icon using Material Design icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.palette),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                
                drawRect(
                    color = colorScheme.onPrimary,
                    topLeft = Offset(0f, 0f),
                    size = Size(width, height / 2)
                )
                
                drawRect(
                    color = colorScheme.secondary,
                    topLeft = Offset(0f, height / 2),
                    size = Size(width / 2, height / 2)
                )
                
                drawRect(
                    color = colorScheme.tertiary,
                    topLeft = Offset(width / 2, height / 2),
                    size = Size(width / 2, height / 2)
                )
            }
        }
    }
}
