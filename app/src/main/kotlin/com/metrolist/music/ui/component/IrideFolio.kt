package com.metrolist.music.ui.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.ui.utils.rememberReducedMotion

/**
 * IRIDE FOLIO — protocollo visivo delle pagine, derivato da Home e Libreria > Album.
 *
 * Questo file è il punto di riferimento per modificare il comportamento di TUTTE le pagine.
 * IridePageLayout (liste/griglie), IridePageFrame (contenuti verticali), IridePageTopBar e
 * LibraryGridPage sono adattatori dello stesso protocollo, non stili alternativi.
 *
 * FILOSOFIA
 *
 * Il titolo grande mantiene la posizione iniziale e scorre verso l'alto insieme al contenuto,
 * senza dissolvenze, ridimensionamenti o un titolo piccolo persistente. Home e le pagine
 * principali iniziano dal titolo; nelle pagine con navigazione i pulsanti precedono il titolo,
 * come in Album. Il titolo non possiede uno sfondo.
 *
 * La fascia superiore è SEMPRE trasparente: anche dopo la scomparsa del titolo non si deve generare
 * un pannello nero, una Surface, una barra Material con elevazione o una linea larga quanto lo
 * schermo. Solo i gruppi di pulsanti acquistano il box della pagina Album quando il contenuto
 * raggiunge la loro area. Il valore che attiva il box dipende dalla posizione del contenuto, non
 * dalla semplice scomparsa del titolo. Ricerca e selezione sono contenuti contestuali: possono
 * occupare spazio tra i pulsanti senza reintrodurre il titolo piccolo.
 * I box sfocati campionano contenuto e sfondo già compositi,
 * con blur a 64 dp e tinta nera al 25% (45% quando il blur non è disponibile):
 * testo e immagini sottostanti devono sfocarsi senza riemergere nitidi attraverso il filtro.
 * Navigazione e mini player usano la stessa tinta all'84% (96% senza blur) sulle pagine neutre.
 * Sulle pagine con artwork tutti i pannelli riprendono lo stesso pigmento della pagina al 40%,
 * senza sovrapporre un nuovo bianco o nero. Pigmento e colore del testo si pubblicano insieme.
 * Il mini player usa lo stesso renderer delle floating pills, senza blur o tinta progressivi.
 * La superficie uniforme inizia alla linea divisoria, estesa fino ai bordi dello schermo.
 * La copia sfocata resta opaca: il testo originale non deve riemergere sotto il vetro. I pannelli inferiori campionano
 * la stessa pagina rasterizzata; il blur di navigazione riguarda solo la fascia inferiore.
 * I gradienti della pagina vanno nello slot background, così fanno parte dello snapshot.
 * Il velo superiore parte al 100% di opacità e sfuma verso il basso senza un bordo visibile.
 * Usa il colore dello sfondo: nero sulle pagine nere, colore del gradiente sulle pagine colorate.
 * Gli artisti conservano pigmenti reali senza modificarne luminosità o saturazione.
 * ArtworkContrast accetta tinte già molto scure oppure chiare leggibili con testo nero,
 * con un margine per la grana. Le tinte intermedie ricadono sul nero; sfondo e testo cambiano insieme.
 * Gli artisti campionano i bordi della foto: la tinta arretra in parallasse verso la base bianca
 * o nera dopo uno scorrimento iniziale, mantenendo tutta la schermata tinta all'apertura.
 * Risalendo riappare la tinta, senza aloni radiali o animazioni temporizzate.
 * Foto e tinta sono preparate insieme e riusate dalla cache. Il blur originale si applica solo
 * scorrendo verso i brani, mai al trascinamento verso il basso.
 * Gli album cercano gli accenti prima dei neutri e ricavano posizione e dimensione delle
 * campiture dalla copertina. La tinta arriva dall'alto e sfuma su fondo scuro;
 * le campiture restano ancorate al bordo superiore.
 * Anche il velo
 * superiore usa questa tinta, come negli album.
 * il velo appare in FadeDurationMillis solo quando la foto esce dal viewport.
 *
 * MENU DEI TRE PUNTINI
 *
 * Il riferimento grafico canonico è il pannello aperto da Libreria > Album > tre puntini,
 * non soltanto il pulsante che lo apre. TUTTI i menu di azioni nell'header Folio (incluse
 * impostazioni, account e importazione Spotify) sono generati da IridePageTopBar.
 * Le schermate passano solo menuSections con IrideFolioMenuAction a IridePageLayout,
 * IridePageFrame o IridePageHeaderLayout: Folio genera automaticamente il pulsante,
 * lo stato di apertura e il pannello. IrideFolioMenu è privato e non espone personalizzazioni
 * grafiche alle schermate. Anche gli Album usano lo stesso header, senza una copia locale.
 * Il pannello mantiene sfondo sfocato, bordo, angoli, larghezza e righe compatte degli Album.
 * Le schermate definiscono solo etichette, icone, selezione e azioni; non ricreano DropdownMenu
 * Material o copie locali dello stile. Il pannello appare completo anche quando i box dei
 * pulsanti sono ancora trasparenti: non applicare al menu il progress dell'header.
 * Il popup vive in una finestra separata: il suo osservatore mantiene attivo lo snapshot e
 * ridisegna il blur dall'interno del clip, anche quando i box dell'header sono trasparenti.
 * Per modificare questa grafica, aggiornare IrideFolioMenu e i valori Menu qui sotto:
 * tutte le schermate Folio ereditano la modifica dallo stesso componente privato.
 * Anche le liste dei pannelli usano irideFolioGestures e overscrollEffect = null: ai bordi
 * continuano a muoversi con la stessa resistenza progressiva e molla della pagina. Il contenuto
 * resta nel clip del pannello; superficie e scrollbar rimangono ferme durante il trascinamento.
 *
 * CONTROLLI DELLE COLLEZIONI
 *
 * Artisti, album e playlist (incluse automatiche e Top) usano
 * IrideFolioCollectionControlPanel. Le pagine forniscono solo azioni, icone e stato;
 * capsula, cerchi, dimensioni e colori dipendono dai valori Collection qui sotto.
 * IridePlaylistControlPanel traduce shuffle e download nello stesso pannello.
 * La ricerca delle playlist usa IrideFolioCollectionSearchBox, senza bordo e con
 * il raggio CollectionSearchCornerRadius. Non aggiungere copie grafiche nelle pagine.
 *
 * CLASSIFICHE
 *
 * IrideFolioRankedSongRow riserva un margine esterno al numero della posizione.
 * Solo il numero usa questa eccezione: copertina, titolo e menu restano nel renderer
 * dei brani condiviso. Le pagine passano posizione e contenuto, senza copie dello stile.
 * Le intestazioni generate usano i valori GeneratedPlaylist qui sotto.
 *
 * BOTTONI E POPUP
 *
 * IrideFolioPrimaryButton e IrideFolioSecondaryButton possiedono lo stile delle azioni
 * piene e con solo bordo. Anche i controlli circolari delle collezioni passano dallo
 * stesso renderer di superficie in IrideFolioButtons: forma, colori, pressione e stati
 * disabilitati non si ricreano nelle schermate. I valori Button qui sotto valgono per tutti.
 * IrideFolioDialog possiede finestra, superficie e tipografia dei popup IRIDE.
 * DefaultDialog, ListDialog e IrideFolioAlertDialog ne adattano soltanto il contenuto;
 * aggiornamenti, importazioni e configurazioni usano lo stesso contenitore e valori Dialog.
 *
 * Il movimento elastico appartiene all'intera pagina (titolo, contenuto e pulsanti insieme). Vale a
 * entrambi i bordi anche con zero elementi o con contenuto più corto dello schermo. Non usare
 * canScrollForward/canScrollBackward per DISABILITARLO quando entrambi sono false. Ogni pagina deve
 * avere un ricevitore di gesti esteso al viewport, anche nello stato vuoto. Un solo
 * rubberBandOverscroll verticale deve essere applicato al frame esterno; disabilitare lo
 * stretch/glow Android nei figli tramite LocalOverscrollFactory e overscrollEffect = null. Le liste
 * orizzontali possono conservare il proprio movimento elastico orizzontale.
 *
 * MIGRAZIONE / MANUTENZIONE (anche per future sessioni di sviluppo)
 *
 * Prima di applicare Folio ELIMINARE il vecchio proprietario della barra: topBar di Scaffold,
 * CollapsingScreenHeader, exitUntilCollapsedScrollBehavior, relativa nestedScrollConnection,
 * FrostedPageTitleBar, scrolledTopBarBackground e copie locali di titoli/fade/elevazioni.
 * SOSTITUIRE la struttura con IridePageLayout o IridePageFrame; non sovrapporre due sistemi.
 * IridePageFrame con ScrollState possiede verticalScroll: rimuoverlo dal Column interno.
 * Liste/griglie lazy usano IridePageLayout e mantengono il proprio stato e contenuto; il titolo
 * visibile nella lista scorre con il contenuto. L'elemento del titolo ha indice 0: includerlo nei
 * conteggi headerItems usati da riordino e scrollbar, senza
 * aggiungere un secondo titolo nel contenuto. Il fondo e gli stati vuoti appartengono allo stesso
 * viewport; non sostituire il frame con un Box vuoto quando la lista non contiene risultati.
 *
 * Spostare azioni e filtri negli slot navigationIcon/actions/pageControls/overlay; mantenere le
 * funzioni della pagina senza conservare la grafica della precedente barra. Le schermate WebView
 * usano il frame con viewport finito e integrano il nested scroll della vista, evitando di misurare
 * una WebView o una lista lazy dentro un Column verticale infinito. Non aggiungere tempi di
 * animazione, molle, dimensioni o sfondi locali per "correggere" una singola pagina: modificarli
 * qui cambia il protocollo per tutte le pagine collegate.
 *
 * PUNTI DI MODIFICA
 *
 * FadeDurationMillis cambia il tempo di dissolvenza dei box dei pulsanti e del velo. TitleFontSize,
 * TitleLineHeight e le spaziature regolano la tipografia e la posizione comuni. RubberBandResistance/Damping/Stiffness regolano il trascinamento e il
 * ritorno di tutte le pagine. IridePageLayoutDefaults deve restare un insieme di alias: non
 * ripristinare valori indipendenti negli adattatori o nelle schermate.
 *
 * Il frame con ScrollState scorre contenuti normali; il frame con ScrollableState mantiene un
 * viewport finito per WebView, player e pager. Un documento nativo pubblica il proprio offset per
 * lo scorrimento del titolo, ma il box dei pulsanti dipende sempre dalla posizione reale del
 * viewport. Usare IrideFolioWebView per inoltrare al frame i gesti rimasti ai bordi del documento.
 */
object IrideFolio {
    const val FadeDurationMillis = 440
    const val TopScrimOpacity = 1f
    val TopScrimFadeHeight = 12.dp
    val TopScrimCameraGap = 8.dp
    const val ArtworkBackgroundBrightness = 0.9f
    const val ArtworkMaximumLuminance = 0.10f
    val ArtworkGradientHeight = 380.dp
    const val ArtistGradientViewportFraction = 0.30f
    const val ArtistBackgroundInitialExtent = 2.2f
    const val ArtistBackgroundParallaxFactor = 0.20f
    val ArtworkTransparentTail = 24.dp
    val ArtistTitleOverlap = 44.dp
    val ArtistArtworkFadeHeight = 64.dp
    val ArtworkPullFadeHeight = 96.dp
    const val ArtworkPullFadeScale = 1.5f
    val ArtistPhotoBlurRadius = 40.dp
    val ArtistPhotoBlurLead = 160.dp
    // Smootherstep reaches both ends with zero slope, avoiding a visible cutoff below controls.
    fun topScrimBrush(color: Color) = Brush.verticalGradient(
        colorStops = Array(33) { index ->
            val fraction = index / 32f
            val fade = fraction * fraction * fraction * (fraction * (fraction * 6f - 15f) + 10f)
            fraction to color.copy(alpha = TopScrimOpacity * (1f - fade).coerceIn(0f, 1f))
        },
    )
    fun bottomScrimBrush(color: Color, startFraction: Float = 0f, endFraction: Float = 1f) =
        Brush.verticalGradient(
            colorStops = Array(33) { index ->
                val fraction = index / 32f
                val fade = fraction * fraction * fraction * (fraction * (fraction * 6f - 15f) + 10f)
                (startFraction + fraction * (endFraction - startFraction)) to
                    color.copy(alpha = color.alpha * fade)
            },
        )
    val FrostedBlurRadius = 64.dp
    val NavigationFrostBlurRadius = 36.dp
    const val FrostedSurfaceOpacity = 0.25f
    const val FrostedFallbackOpacity = 0.45f
    const val FrostedLightTintOpacity = 0.10f
    const val FrostedLightSurfaceOpacity = 0.50f
    const val FloatingPillSurfaceOpacity = 0.84f
    const val FloatingPillFallbackOpacity = 0.96f
    const val ArtworkPanelTintOpacity = 0.40f
    fun floatingPillSurfaceAlpha(backdrop: FrostBackdrop?, backgroundColor: Color? = null): Float =
        if (backgroundColor != null && backgroundColor != Color.Black) ArtworkPanelTintOpacity
        else if (backdrop == null) FloatingPillFallbackOpacity else FloatingPillSurfaceOpacity
    const val MiniPlayerFrostWaviness = 0.10f
    val MiniPlayerTopSpacing = 16.dp
    val MiniPlayerDividerHorizontalInset = 0.dp
    val MiniPlayerDividerOffset = 10.dp
    val MiniPlayerBackdropTopOffset = MiniPlayerDividerOffset
    val MiniPlayerDividerStroke = 0.5.dp
    const val MiniPlayerDividerOpacity = 0.18f
    fun frostedSurfaceColor(contentColor: Color, backgroundColor: Color? = null): Color =
        backgroundColor?.takeIf { it != Color.Black } ?:
            if (contentColor.luminance() < 0.5f) Color.White else Color.Black
    fun frostedSurfaceAlpha(contentColor: Color, backgroundColor: Color? = null): Float? =
        if (backgroundColor != null && backgroundColor != Color.Black) ArtworkPanelTintOpacity
        else if (frostedSurfaceColor(contentColor) == Color.White) FrostedLightSurfaceOpacity else null
    // Black-only grain averages one fifth of these peak opacities.
    val GrainSize = 0.55.dp
    const val BackgroundGrainOpacity = 0.065f
    const val LyricsGrainOpacity = 0.05f
    const val SurfaceGrainOpacity = 0.05f
    const val ButtonGrainOpacity = 0.065f
    val HorizontalPadding = 20.dp
    // Horizontal shelves in Home and Discovery share one typographic hierarchy.
    val ShelfSectionFontSize = 24.sp
    val ShelfTitleFontSize = 18.sp
    val ShelfLabelFontSize = 12.sp
    val ShelfItemFontSize = 14.sp
    val ShelfDetailFontSize = 12.sp
    val ShelfTopPadding = 24.dp
    val ShelfBottomPadding = 12.dp
    val ShelfItemSpacing = 12.dp
    val ShelfSelectorIconSize = 18.dp
    val ShelfSelectorMaxWidth = 150.dp
    val SongRowHorizontalPadding = 12.dp
    val ReleaseSpotlightHeight = 176.dp
    val ReleaseSpotlightCoverSize = 72.dp
    val ReleaseSpotlightPadding = 14.dp
    val ReleaseSpotlightCornerRadius = 12.dp
    val ReleaseSpotlightTitleSize = 14.sp
    val ReleaseSpotlightTitleLineHeight = 17.sp
    val ReleaseSpotlightArtistSize = 11.sp
    val ReleaseSpotlightMetadataSize = 9.sp
    val ChartRankMinimumGutter = HorizontalPadding
    val ChartRankGap = 6.dp
    val ChartRankFontSize = 10.sp
    val ChartRankLineHeight = 14.sp
    val ChartRankIndicatorSize = 14.dp
    val ChartRankNewEntryDotSize = 5.dp
    const val ChartRankHighlightedCount = 25
    val ChartRankUpColor = Color(0xFF4CAF50)
    val ChartRankMutedColor = Color(0xFF808080).copy(alpha = 0.45f)
    val GeneratedPlaylistArtworkSize = 240.dp
    val GeneratedPlaylistArtworkRadius = 12.dp
    const val GeneratedPlaylistArtworkSmoothing = 0.45f
    const val GeneratedPlaylistBadgeSizeFraction = 0.65f
    val GeneratedPlaylistArtworkShadow = 24.dp
    val GeneratedPlaylistTopPadding = 12.dp
    val GeneratedPlaylistBottomPadding = 20.dp
    val GeneratedPlaylistTitleGap = 20.dp
    val GeneratedPlaylistMetadataGap = 8.dp
    val GeneratedPlaylistMetadataFontSize = 13.sp
    val CollectionTitleFontSize = 20.sp
    val CollectionTitleLetterSpacing = (-0.2).sp
    const val CollectionTitleMaxLines = 3
    val TitleTopSpacing = 8.dp
    val TitleBottomSpacing = 4.dp
    val TitleFontSize = 34.sp
    val TitleLineHeight = 41.sp
    // Artist actions use 40 dp controls, with a 2 dp inset on each side.
    val ControlsHeight = 48.dp
    val ControlsTouchTargetSize = 40.dp
    const val ControlsScale = 0.875f
    val ControlSurfaceSize = 44.dp
    val NavigationSurfaceHeight = 52.dp
    val NavigationHeight = NavigationSurfaceHeight + 8.dp
    val NavigationTouchTargetSize = 56.dp
    val NavigationMaxWidth = 400.dp
    val NavigationGap = 8.dp
    val NavigationSelectionInset = 4.dp
    val NavigationSelectionCornerRadius = 14.dp
    val NavigationIconSize = 28.dp
    const val NavigationIconStroke = 1.5f
    const val NavigationSelectionDurationMillis = 520
    const val NavigationReturnDurationMillis = 360
    const val NavigationTurnDurationMillis = 680
    val NavigationMotionEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    val ControlsTopSpacing = 4.dp
    val ControlsCornerRadius = 18.dp
    val ControlsShape = RoundedCornerShape(ControlsCornerRadius)
    val ControlsDividerWidth = 1.dp
    val ControlsDividerHeight = 20.dp
    val TitleAfterControls = ControlsTopSpacing + ControlsHeight + TitleTopSpacing
    val MenuWidth = 224.dp
    val MenuOffset = DpOffset(0.dp, 8.dp)
    val MenuViewportVerticalMargin = 8.dp
    val MenuVerticalPadding = 8.dp
    val MenuItemHeight = 42.dp
    val MenuContentPadding = 2.dp
    val MenuBorderWidth = 1.dp
    val MenuContentShape = RoundedCornerShape(ControlsCornerRadius - MenuBorderWidth)
    val MenuSectionHorizontalPadding = 14.dp
    val MenuSectionVerticalPadding = 2.dp
    val MenuDividerThickness = 1.dp
    // Shared scroll indicators: every Folio surface uses these dimensions and ink opacities.
    val ScrollbarWidth = 3.dp
    val ScrollbarTouchTargetWidth = 20.dp
    val ScrollbarEdgeInset = 6.dp
    val ScrollbarVerticalPadding = 10.dp
    val ScrollbarMinimumThumbHeight = 20.dp
    const val ScrollbarTrackOpacity = 0.12f
    const val ScrollbarThumbOpacity = 0.65f
    // Button and popup presentation belongs to Folio; callers supply only content and behavior.
    val ButtonShape = RoundedCornerShape(percent = 50)
    val ButtonMinHeight = 40.dp
    val ButtonHorizontalPadding = 20.dp
    val ButtonVerticalPadding = 10.dp
    val ButtonBorderWidth = 1.dp
    const val ButtonBorderOpacity = 0.22f
    const val ButtonDisabledOpacity = 0.4f
    const val ButtonPressedScale = 0.92f
    val DialogShape = RoundedCornerShape(16.dp)
    val DialogOuterPadding = 24.dp
    val DialogContentPadding = 24.dp
    val DialogBorderWidth = 1.dp
    val DialogSectionSpacing = 16.dp
    val DialogActionSpacing = 8.dp
    val DialogActionsTopSpacing = 24.dp
    // Collection transport controls share one renderer across artists, albums and playlists.
    val CollectionPanelShape = RoundedCornerShape(percent = 50)
    val CollectionPanelBorderWidth = 1.dp
    val CollectionPanelHorizontalPadding = 12.dp
    val CollectionPanelVerticalPadding = 8.dp
    val CollectionPanelButtonSpacing = 10.dp
    val CollectionActionTouchTargetSize = 48.dp
    val CollectionActionSize = 44.dp
    val CollectionActionIconSize = 22.dp
    val CollectionPlaySize = 64.dp
    val CollectionPlayIconSize = 40.dp
    val CollectionPlayIconOffset = 1.5.dp
    val CollectionPlayIcon = R.drawable.ic_iride_play_rounded
    val CollectionPauseIcon = R.drawable.ic_iride_pause_rounded
    val CollectionSearchCornerRadius = 5.dp
    const val CollectionPanelBorderOpacity = 0.14f
    const val RubberBandResistance = 0.2f
    const val RubberBandDamping = 1f
    const val RubberBandStiffness = Spring.StiffnessLow
}

internal val LocalIrideFolioScrimColor = staticCompositionLocalOf { Color.Black }

/** Scale only header controls; page typography and popup rows keep their original density. */
@Composable
internal fun IrideFolioCompactControls(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val controlsDensity = remember(density) {
        Density(density.density * IrideFolio.ControlsScale, density.fontScale)
    }
    val controlsViewConfiguration = remember(viewConfiguration) {
        object : ViewConfiguration by viewConfiguration {
            // Compensate for the visual density so shrinking icons does not shrink touch targets.
            override val minimumTouchTargetSize = DpSize(
                viewConfiguration.minimumTouchTargetSize.width / IrideFolio.ControlsScale,
                viewConfiguration.minimumTouchTargetSize.height / IrideFolio.ControlsScale,
            )
        }
    }
    CompositionLocalProvider(
        LocalDensity provides controlsDensity,
        LocalViewConfiguration provides controlsViewConfiguration,
        content = content,
    )
}

/** Fixed dark media/announcement surfaces keep Folio controls legible in either app theme. */
@Composable
fun IrideFolioDarkSurface(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color.White.copy(alpha = 0.7f),
        ),
        content = content,
    )
}

@Composable
internal fun IrideFolioControlDivider() {
    val borderColor = MaterialTheme.colorScheme.strokeCard
    Box(
        Modifier.width(IrideFolio.ControlsDividerWidth)
            .height(IrideFolio.ControlsDividerHeight)
            .background(borderColor.copy(alpha = borderColor.alpha * LocalPageControlsProgress.current))
    )
}

/** Screen-specific menu content. All presentation belongs to Folio's private renderer. */
data class IrideFolioMenuAction(
    val label: String,
    val icon: Int? = null,
    val onClick: () -> Unit,
    val selected: Boolean = false,
)

/** Owned by the header: callers provide actions, never a separately styled button or popup. */
@Composable
internal fun IrideFolioOverflowMenu(
    sections: List<List<IrideFolioMenuAction>>,
    icon: Int = R.drawable.more_vert,
    contentDescription: String = stringResource(R.string.more_options),
    iconTint: Color = LocalArtworkContentColor.current ?: Color.White,
) {
    if (sections.none { it.isNotEmpty() }) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        IrideFolioCompactControls {
            IrideFolioMenuButton(
                expanded = expanded,
                onClick = { expanded = true },
                modifier = Modifier.size(IrideFolio.ControlsTouchTargetSize),
            ) {
                Icon(
                    painterResource(icon),
                    contentDescription,
                    tint = iconTint,
                )
            }
        }
        IrideFolioMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            sections = sections,
        )
    }
}

/** Overflow controls share the page control surface and its Folio fade, including on song rows. */
@Composable
internal fun IrideFolioMenuButton(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val progress = rememberIrideFolioFade(if (expanded) 1f else 0f)
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(IrideFolio.ControlSurfaceSize)
                // Song buttons are inside the page snapshot. Sampling that snapshot here would
                // make its graphics layer draw itself through its blur layer during the fade.
                .irideFolioControlSurface(progress, backdrop = null, forceArtworkSurface = false),
        )
        IconButton(onClick = onClick, enabled = enabled, content = content)
    }
}

/** Canonical header action menu, taken from Library > Albums. */
@Composable
private fun IrideFolioMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    sections: List<List<IrideFolioMenuAction>>,
) {
    IrideFolioChoiceMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        actions = sections.flatten(),
        sections = sections,
    )
}

@Composable
internal fun Modifier.irideFolioMenuSurface(): Modifier {
    val menuInk = LocalArtworkContentColor.current ?: Color.White
    val artworkBackground = LocalArtworkBackgroundColor.current
    return frostedNavigationSurface(
        LocalScreenFrostBackdrop.current,
        IrideFolio.frostedSurfaceColor(menuInk, artworkBackground),
        IrideFolio.ControlsShape,
        surfaceAlpha = IrideFolio.frostedSurfaceAlpha(menuInk, artworkBackground)
            ?: IrideFolio.FrostedSurfaceOpacity,
        observeBackdrop = true,
    )
        .border(IrideFolio.MenuBorderWidth, MaterialTheme.colorScheme.strokeCard, IrideFolio.ControlsShape)
        // Keep scrolling rows inside the border, including its rounded corners.
        .padding(IrideFolio.MenuBorderWidth)
        .clip(IrideFolio.MenuContentShape)
}

@Composable
internal fun IrideFolioMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(
            horizontal = IrideFolio.MenuSectionHorizontalPadding,
            vertical = IrideFolio.MenuSectionVerticalPadding,
        ),
        color = MaterialTheme.colorScheme.strokeCard,
        thickness = IrideFolio.MenuDividerThickness,
    )
}

@Composable
internal fun IrideFolioMenuItem(
    text: @Composable () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val menuInk = LocalArtworkContentColor.current ?: Color.White
    DropdownMenuItem(
        enabled = enabled,
        text = text,
        leadingIcon = leadingIcon,
        trailingIcon =
            if (selected) {
                { Icon(painterResource(R.drawable.check), null, tint = menuInk) }
            } else null,
        onClick = onClick,
        colors =
            MenuDefaults.itemColors(
                textColor = menuInk,
                leadingIconColor = menuInk,
                trailingIconColor = menuInk,
            ),
        modifier = Modifier.height(IrideFolio.MenuItemHeight).semantics { this.selected = selected },
    )
}

@Composable
internal fun rememberIrideFolioFade(
    target: Float,
    reducedMotion: Boolean = rememberReducedMotion(),
): Float {
    val value by
        animateFloatAsState(
            targetValue = target.coerceIn(0f, 1f),
            animationSpec = tween(if (reducedMotion) 0 else IrideFolio.FadeDurationMillis),
            label = "irideFolioFade",
        )
    return value
}

/** Same box as Album, applied only to controls, never to the full width of the page. */
@Composable
internal fun Modifier.irideFolioControlSurface(
    progress: Float,
    backdrop: FrostBackdrop?,
    forceArtworkSurface: Boolean = true,
): Modifier {
    val stroke = MaterialTheme.colorScheme.strokeCard
    val artworkInk = LocalArtworkContentColor.current
    val artworkBackground = LocalArtworkBackgroundColor.current
    val surfaceProgress = if (forceArtworkSurface && artworkInk != null) 1f else progress
    return layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            // actions can be empty; the surface's own padding must not create a tiny ghost box.
            if (placeable.width <= 4.dp.roundToPx()) layout(0, 0) {}
            else layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
        .frostedNavigationSurface(
            backdrop,
            IrideFolio.frostedSurfaceColor(artworkInk ?: Color.White, artworkBackground),
            IrideFolio.ControlsShape,
            surfaceProgress,
            surfaceAlpha = IrideFolio.frostedSurfaceAlpha(artworkInk ?: Color.White, artworkBackground),
            observeBackdrop = surfaceProgress > 0f,
        )
        .border(1.dp, stroke.copy(alpha = stroke.alpha * surfaceProgress), IrideFolio.ControlsShape)
        .padding(2.dp)
}

/**
 * Only unconsumed distance reaches the outer spring. Do not make the frame scrollable with
 * the child's state: both gesture detectors would grab a new touch during the same fling.
 */
@Composable
internal fun Modifier.irideFolioGestures(state: ScrollableState): Modifier =
    rubberBandOverscroll(Orientation.Vertical, state)

/** Use on siblings of the scrolling content, never on its parent. */
internal fun Modifier.irideFolioScrollReceiver(state: ScrollableState): Modifier =
    scrollable(state, Orientation.Vertical, reverseDirection = true, overscrollEffect = null)
