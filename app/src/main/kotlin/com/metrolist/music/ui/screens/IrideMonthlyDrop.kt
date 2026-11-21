package com.metrolist.music.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IridePageBackButton
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.gestureNavigationEnabled
import com.metrolist.music.ui.theme.CollectionArtworkShape
import com.metrolist.music.ui.theme.FolioShape
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.utils.MonthlyDrop
import com.metrolist.music.utils.MonthlyDropPolicy
import com.metrolist.music.utils.MonthlyDropSchedule
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

internal const val IrideMonthlyDropRoute = "iride/monthly-drop"

@Composable
internal fun rememberMonthlyDropTime(): ZonedDateTime {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableStateOf(ZonedDateTime.now()) }

    DisposableEffect(context, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) now = ZonedDateTime.now()
        }
        lifecycle.addObserver(observer)
        val clockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                now = ZonedDateTime.now()
            }
        }
        val clockFilter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, clockReceiver, clockFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            lifecycle.removeObserver(observer)
            context.unregisterReceiver(clockReceiver)
        }
    }

    LaunchedEffect(now) {
        val nextMonth = YearMonth.from(now).plusMonths(1).atDay(1).atStartOfDay(now.zone)
        val remaining = nextMonth.toInstant().toEpochMilli() - System.currentTimeMillis()
        delay(remaining.coerceAtLeast(1L))
        now = ZonedDateTime.now()
    }
    return now
}

@Composable
internal fun rememberCurrentMonthlyDrop(): MonthlyDrop? {
    if (!MonthlyDropPolicy.enabledForBuild || !MonthlyDropSchedule.hasScheduledDrops) return null
    return MonthlyDropSchedule.currentDrop(rememberMonthlyDropTime())
}

@Composable
private fun monthlyDropMonthLabel(month: YearMonth): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(month, locale) { month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)) }
}

@Composable
internal fun monthlyDropTitle(month: YearMonth): String =
    stringResource(R.string.iride_monthly_drop_title, monthlyDropMonthLabel(month))

@Composable
internal fun IrideMonthlyDropHomeCard(
    drop: MonthlyDrop,
    artworkSize: Dp,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier.width(artworkSize)
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier.size(artworkSize)
                    .clip(CollectionArtworkShape)
                    .border(1.dp, MaterialTheme.colorScheme.strokeCard, CollectionArtworkShape),
        ) {
            IrideMonthlyDropArtwork(Modifier.fillMaxSize())
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp).size(44.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 5.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.ic_logo),
                        contentDescription = stringResource(R.string.about_iride_name),
                        colorFilter = ColorFilter.tint(Color(0xFF342058)),
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = monthlyDropMonthLabel(drop.month),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = pluralStringResource(R.plurals.iride_monthly_drop_changes, drop.features.size, drop.features.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.iride_monthly_drop_tap_to_read),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
internal fun IrideMonthlyDropScreen(navController: NavController) {
    if (!MonthlyDropPolicy.enabledForBuild) return
    val drop = rememberCurrentMonthlyDrop()
    val listState = rememberLazyListState()
    IridePageLayout(
        title = if (drop == null) stringResource(R.string.iride_monthly_drop_settings_section)
        else monthlyDropTitle(drop.month),
        state = listState,
        showTopBar = true,
        navigationIcon =
            if (gestureNavigationEnabled()) null else ({ IridePageBackButton(navController) }),
        bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
    ) {
        if (drop != null) {
            item(key = "iride_monthly_drop_${drop.month}") {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = IrideFolio.HorizontalPadding)
                        .padding(bottom = 24.dp),
                ) {
                    for (feature in drop.features) {
                        MonthlyDropTextItem(feature.titleRes, feature.descriptionRes)
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .height(300.dp)
                            .clip(FolioShape(24.dp))
                            .border(1.dp, MaterialTheme.colorScheme.strokeCard, FolioShape(24.dp)),
                    ) {
                        IrideMonthlyDropArtwork(Modifier.fillMaxSize())
                    }
                }
            }
        }
        item(key = "iride_monthly_drop_guide") {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = IrideFolio.HorizontalPadding)
                        .padding(bottom = 24.dp),
            ) {
                Text(
                    text = stringResource(R.string.iride_monthly_drop_guide_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.iride_monthly_drop_guide_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                MonthlyDropTextItem(
                    R.string.iride_monthly_drop_guide_schedule_title,
                    R.string.iride_monthly_drop_guide_schedule,
                )
                MonthlyDropTextItem(
                    R.string.iride_monthly_drop_guide_fixes_title,
                    R.string.iride_monthly_drop_guide_fixes,
                )
                MonthlyDropTextItem(
                    R.string.iride_monthly_drop_guide_download_title,
                    R.string.iride_monthly_drop_guide_download,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.iride_monthly_drop_guide_install),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MonthlyDropTextItem(
    titleRes: Int,
    descriptionRes: Int,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(descriptionRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IrideMonthlyDropArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val shortestSide = minOf(width, height)
        drawRect(
            brush =
                Brush.linearGradient(
                    colors = listOf(Color(0xFF3E236F), Color(0xFFB64F9B), Color(0xFFFF9568)),
                    start = Offset(0f, height),
                    end = Offset(width, 0f),
                ),
        )

        val discCenter = Offset(width * 0.69f, height * 0.46f)
        val discRadius = shortestSide * 0.36f
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFFD989), Color(0xFFFF8F85), Color(0xFF8B56D7)),
                    center = discCenter,
                    radius = discRadius,
                ),
            radius = discRadius,
            center = discCenter,
        )
        for (ring in 1..5) {
            drawCircle(
                color = Color.White.copy(alpha = 0.19f - ring * 0.018f),
                radius = discRadius * (0.36f + ring * 0.105f),
                center = discCenter,
                style = Stroke(width = shortestSide * 0.006f),
            )
        }
        drawCircle(
            brush = Brush.linearGradient(listOf(Color(0xFF51E4D0), Color(0xFF7B63ED))),
            radius = discRadius * 0.19f,
            center = discCenter,
        )

        val ribbon = Path().apply {
            moveTo(0f, height * 0.68f)
            cubicTo(
                width * 0.18f,
                height * 0.43f,
                width * 0.34f,
                height * 0.91f,
                width * 0.55f,
                height * 0.72f,
            )
            cubicTo(
                width * 0.76f,
                height * 0.53f,
                width * 0.83f,
                height * 0.96f,
                width,
                height * 0.71f,
            )
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = ribbon,
            brush =
                Brush.linearGradient(
                    listOf(Color(0xFF3BDBCD), Color(0xFF7462E8), Color(0xFFE566B5)),
                ),
            alpha = 0.72f,
        )

        val barHeights = listOf(0.14f, 0.23f, 0.17f, 0.31f, 0.22f, 0.38f, 0.26f, 0.18f)
        val barWidth = shortestSide * 0.025f
        val barGap = shortestSide * 0.025f
        barHeights.forEachIndexed { index, fraction ->
            val barHeight = shortestSide * fraction
            drawRoundRect(
                color = Color.White.copy(alpha = 0.8f),
                topLeft =
                    Offset(
                        shortestSide * 0.08f + index * (barWidth + barGap),
                        height * 0.81f - barHeight / 2f,
                    ),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth, barWidth),
            )
        }

        listOf(
            Offset(width * 0.24f, height * 0.2f),
            Offset(width * 0.39f, height * 0.29f),
            Offset(width * 0.88f, height * 0.18f),
            Offset(width * 0.16f, height * 0.51f),
        ).forEachIndexed { index, point ->
            drawCircle(
                color = if (index % 2 == 0) Color(0xFFFFE5A6) else Color.White,
                radius = shortestSide * if (index == 2) 0.022f else 0.014f,
                center = point,
                alpha = 0.9f,
            )
        }
    }
}
