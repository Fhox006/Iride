/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens

import com.metrolist.music.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.gestureNavigationEnabled
import com.metrolist.music.ui.component.IridePageBackButton
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import java.net.URLEncoder
import kotlinx.coroutines.flow.MutableStateFlow

private data class GenreItem(val name: String)

private val GENRE_LABEL_RESOURCES =
    mapOf(
        "Pop" to R.string.genre_pop,
        "Hip Hop" to R.string.genre_hip_hop,
        "Rap" to R.string.genre_rap,
        "Rock" to R.string.genre_rock,
        "Trap" to R.string.genre_trap,
        "EDM" to R.string.genre_edm,
        "Reggaeton" to R.string.genre_reggaeton,
        "R&B" to R.string.genre_r_and_b,
        "K-Pop" to R.string.genre_k_pop,
        "Metal" to R.string.genre_metal,
        "Country" to R.string.genre_country,
        "Electronic" to R.string.genre_electronic,
        "Indie" to R.string.genre_indie,
        "Latin" to R.string.genre_latin,
        "Techno" to R.string.genre_techno,
        "Acid House" to R.string.genre_acid_house,
        "Acid Jazz" to R.string.genre_acid_jazz,
        "Acid Rock" to R.string.genre_acid_rock,
        "Acoustic" to R.string.genre_acoustic,
        "Afrobeat" to R.string.genre_afrobeat,
        "Afrobeats" to R.string.genre_afrobeats,
        "Alternative" to R.string.genre_alternative,
        "Alternative Metal" to R.string.genre_alternative_metal,
        "Alternative Rock" to R.string.genre_alternative_rock,
        "Ambient" to R.string.genre_ambient,
        "Amapiano" to R.string.genre_amapiano,
        "Ambient Techno" to R.string.genre_ambient_techno,
        "Anarcho Punk" to R.string.genre_anarcho_punk,
        "Art Rock" to R.string.genre_art_rock,
        "Avant-garde" to R.string.genre_avant_garde,
        "Bachata" to R.string.genre_bachata,
        "Balearic" to R.string.genre_balearic,
        "Bass House" to R.string.genre_bass_house,
        "Big Band" to R.string.genre_big_band,
        "Black Metal" to R.string.genre_black_metal,
        "Blues" to R.string.genre_blues,
        "Blues Rock" to R.string.genre_blues_rock,
        "Boogie" to R.string.genre_boogie,
        "Bossa Nova" to R.string.genre_bossa_nova,
        "Breakbeat" to R.string.genre_breakbeat,
        "Breakcore" to R.string.genre_breakcore,
        "Britpop" to R.string.genre_britpop,
        "Brazilian Funk" to R.string.genre_brazilian_funk,
        "Brazilian Phonk" to R.string.genre_brazilian_phonk,
        "C-Pop" to R.string.genre_c_pop,
        "Celtic" to R.string.genre_celtic,
        "Chamber Music" to R.string.genre_chamber_music,
        "Chillout" to R.string.genre_chillout,
        "Chillwave" to R.string.genre_chillwave,
        "Christian Rock" to R.string.genre_christian_rock,
        "Classical" to R.string.genre_classical,
        "Cloud Rap" to R.string.genre_cloud_rap,
        "Contemporary R&B" to R.string.genre_contemporary_r_and_b,
        "Country Rock" to R.string.genre_country_rock,
        "Crunk" to R.string.genre_crunk,
        "Cumbia" to R.string.genre_cumbia,
        "Dance" to R.string.genre_dance,
        "Dance-Pop" to R.string.genre_dance_pop,
        "Dark Ambient" to R.string.genre_dark_ambient,
        "Darkwave" to R.string.genre_darkwave,
        "Death Metal" to R.string.genre_death_metal,
        "Deathcore" to R.string.genre_deathcore,
        "Deep House" to R.string.genre_deep_house,
        "Disco" to R.string.genre_disco,
        "Doom Metal" to R.string.genre_doom_metal,
        "Dream Pop" to R.string.genre_dream_pop,
        "Drill" to R.string.genre_drill,
        "Drone" to R.string.genre_drone,
        "Drum and Bass" to R.string.genre_drum_and_bass,
        "Dub" to R.string.genre_dub,
        "Dubstep" to R.string.genre_dubstep,
        "Dungeon Synth" to R.string.genre_dungeon_synth,
        "East Coast Hip Hop" to R.string.genre_east_coast_hip_hop,
        "Electro" to R.string.genre_electro,
        "Electroclash" to R.string.genre_electroclash,
        "Electronica" to R.string.genre_electronica,
        "Emo" to R.string.genre_emo,
        "Emo Rap" to R.string.genre_emo_rap,
        "Epicore" to R.string.genre_epicore,
        "Eurodance" to R.string.genre_eurodance,
        "Europop" to R.string.genre_europop,
        "Experimental" to R.string.genre_experimental,
        "Folk" to R.string.genre_folk,
        "Folk Rock" to R.string.genre_folk_rock,
        "Folktronica" to R.string.genre_folktronica,
        "Freestyle" to R.string.genre_freestyle,
        "Funk" to R.string.genre_funk,
        "Funk Rock" to R.string.genre_funk_rock,
        "Future Bass" to R.string.genre_future_bass,
        "Fusion" to R.string.genre_fusion,
        "Gabber" to R.string.genre_gabber,
        "Gangsta Rap" to R.string.genre_gangsta_rap,
        "Garage Rock" to R.string.genre_garage_rock,
        "Glam Rock" to R.string.genre_glam_rock,
        "Gospel" to R.string.genre_gospel,
        "Gothic Rock" to R.string.genre_gothic_rock,
        "Grime" to R.string.genre_grime,
        "Grindcore" to R.string.genre_grindcore,
        "Grunge" to R.string.genre_grunge,
        "Happy Hardcore" to R.string.genre_happy_hardcore,
        "Hard Rock" to R.string.genre_hard_rock,
        "Hardcore" to R.string.genre_hardcore,
        "Hardstyle" to R.string.genre_hardstyle,
        "Heavy Metal" to R.string.genre_heavy_metal,
        "Hi-NRG" to R.string.genre_hi_nrg,
        "Horrorcore" to R.string.genre_horrorcore,
        "House" to R.string.genre_house,
        "Hyperpop" to R.string.genre_hyperpop,
        "IDM" to R.string.genre_idm,
        "Indie Folk" to R.string.genre_indie_folk,
        "Indie Pop" to R.string.genre_indie_pop,
        "Indie Rock" to R.string.genre_indie_rock,
        "Industrial" to R.string.genre_industrial,
        "Italo Disco" to R.string.genre_italo_disco,
        "J-Pop" to R.string.genre_j_pop,
        "J-Rock" to R.string.genre_j_rock,
        "Jazz" to R.string.genre_jazz,
        "Jazz Fusion" to R.string.genre_jazz_fusion,
        "Jazz Rock" to R.string.genre_jazz_rock,
        "Jersey Club" to R.string.genre_jersey_club,
        "Krautrock" to R.string.genre_krautrock,
        "Latin Jazz" to R.string.genre_latin_jazz,
        "Latin Pop" to R.string.genre_latin_pop,
        "Lo-Fi" to R.string.genre_lo_fi,
        "Math Rock" to R.string.genre_math_rock,
        "Melodic Death Metal" to R.string.genre_melodic_death_metal,
        "Metalcore" to R.string.genre_metalcore,
        "Minimal Techno" to R.string.genre_minimal_techno,
        "Mumble Rap" to R.string.genre_mumble_rap,
        "Neoclassical" to R.string.genre_neoclassical,
        "Neo-Soul" to R.string.genre_neo_soul,
        "New Age" to R.string.genre_new_age,
        "New Wave" to R.string.genre_new_wave,
        "Noise" to R.string.genre_noise,
        "Nu Metal" to R.string.genre_nu_metal,
        "Opera" to R.string.genre_opera,
        "Orchestral" to R.string.genre_orchestral,
        "Phonk" to R.string.genre_phonk,
        "Plugg" to R.string.genre_plugg,
        "Pop Punk" to R.string.genre_pop_punk,
        "Pop Rock" to R.string.genre_pop_rock,
        "Post-Punk" to R.string.genre_post_punk,
        "Post-Rock" to R.string.genre_post_rock,
        "Power Metal" to R.string.genre_power_metal,
        "Progressive House" to R.string.genre_progressive_house,
        "Progressive Metal" to R.string.genre_progressive_metal,
        "Progressive Rock" to R.string.genre_progressive_rock,
        "Psychedelic Rock" to R.string.genre_psychedelic_rock,
        "Psytrance" to R.string.genre_psytrance,
        "Punk" to R.string.genre_punk,
        "Punk Rock" to R.string.genre_punk_rock,
        "Reggae" to R.string.genre_reggae,
        "Rock and Roll" to R.string.genre_rock_and_roll,
        "Rockabilly" to R.string.genre_rockabilly,
        "Salsa" to R.string.genre_salsa,
        "Samba" to R.string.genre_samba,
        "Schlager" to R.string.genre_schlager,
        "Shoegaze" to R.string.genre_shoegaze,
        "Sigilkore" to R.string.genre_sigilkore,
        "Ska" to R.string.genre_ska,
        "Soul" to R.string.genre_soul,
        "Southern Rock" to R.string.genre_southern_rock,
        "Speed Metal" to R.string.genre_speed_metal,
        "Surf Rock" to R.string.genre_surf_rock,
        "Swing" to R.string.genre_swing,
        "Synth-Pop" to R.string.genre_synth_pop,
        "Synthwave" to R.string.genre_synthwave,
        "Tango" to R.string.genre_tango,
        "Tech House" to R.string.genre_tech_house,
        "Thrash Metal" to R.string.genre_thrash_metal,
        "Trance" to R.string.genre_trance,
        "Trip Hop" to R.string.genre_trip_hop,
        "UK Drill" to R.string.genre_uk_drill,
        "UK Garage" to R.string.genre_uk_garage,
        "Vaporwave" to R.string.genre_vaporwave,
        "Visual Kei" to R.string.genre_visual_kei,
        "West Coast Hip Hop" to R.string.genre_west_coast_hip_hop,
        "Western Classical" to R.string.genre_western_classical,
        "Zydeco" to R.string.genre_zydeco,
    )

private val TOP_15_GENRES =
    listOf(
        GenreItem("Pop"),
        GenreItem("Hip Hop"),
        GenreItem("Rap"),
        GenreItem("Rock"),
        GenreItem("Trap"),
        GenreItem("EDM"),
        GenreItem("Reggaeton"),
        GenreItem("R&B"),
        GenreItem("K-Pop"),
        GenreItem("Metal"),
        GenreItem("Country"),
        GenreItem("Electronic"),
        GenreItem("Indie"),
        GenreItem("Latin"),
        GenreItem("Techno"),
    )

private val ALL_GENRES =
    listOf(
        GenreItem("Acid House"),
        GenreItem("Acid Jazz"),
        GenreItem("Acid Rock"),
        GenreItem("Acoustic"),
        GenreItem("Afrobeat"),
        GenreItem("Afrobeats"),
        GenreItem("Alternative"),
        GenreItem("Alternative Metal"),
        GenreItem("Alternative Rock"),
        GenreItem("Ambient"),
        GenreItem("Amapiano"),
        GenreItem("Ambient Techno"),
        GenreItem("Anarcho Punk"),
        GenreItem("Art Rock"),
        GenreItem("Avant-garde"),
        GenreItem("Bachata"),
        GenreItem("Balearic"),
        GenreItem("Bass House"),
        GenreItem("Big Band"),
        GenreItem("Black Metal"),
        GenreItem("Blues"),
        GenreItem("Blues Rock"),
        GenreItem("Boogie"),
        GenreItem("Bossa Nova"),
        GenreItem("Breakbeat"),
        GenreItem("Breakcore"),
        GenreItem("Britpop"),
        GenreItem("Brazilian Funk"),
        GenreItem("Brazilian Phonk"),
        GenreItem("C-Pop"),
        GenreItem("Celtic"),
        GenreItem("Chamber Music"),
        GenreItem("Chillout"),
        GenreItem("Chillwave"),
        GenreItem("Christian Rock"),
        GenreItem("Classical"),
        GenreItem("Cloud Rap"),
        GenreItem("Contemporary R&B"),
        GenreItem("Country"),
        GenreItem("Country Rock"),
        GenreItem("Crunk"),
        GenreItem("Cumbia"),
        GenreItem("Dance"),
        GenreItem("Dance-Pop"),
        GenreItem("Dark Ambient"),
        GenreItem("Darkwave"),
        GenreItem("Death Metal"),
        GenreItem("Deathcore"),
        GenreItem("Deep House"),
        GenreItem("Disco"),
        GenreItem("Doom Metal"),
        GenreItem("Dream Pop"),
        GenreItem("Drill"),
        GenreItem("Drone"),
        GenreItem("Drum and Bass"),
        GenreItem("Dub"),
        GenreItem("Dubstep"),
        GenreItem("Dungeon Synth"),
        GenreItem("East Coast Hip Hop"),
        GenreItem("EDM"),
        GenreItem("Electro"),
        GenreItem("Electroclash"),
        GenreItem("Electronic"),
        GenreItem("Electronica"),
        GenreItem("Emo"),
        GenreItem("Emo Rap"),
        GenreItem("Epicore"),
        GenreItem("Eurodance"),
        GenreItem("Europop"),
        GenreItem("Experimental"),
        GenreItem("Folk"),
        GenreItem("Folk Rock"),
        GenreItem("Folktronica"),
        GenreItem("Freestyle"),
        GenreItem("Funk"),
        GenreItem("Funk Rock"),
        GenreItem("Future Bass"),
        GenreItem("Fusion"),
        GenreItem("Gabber"),
        GenreItem("Gangsta Rap"),
        GenreItem("Garage Rock"),
        GenreItem("Glam Rock"),
        GenreItem("Gospel"),
        GenreItem("Gothic Rock"),
        GenreItem("Grime"),
        GenreItem("Grindcore"),
        GenreItem("Grunge"),
        GenreItem("Happy Hardcore"),
        GenreItem("Hard Rock"),
        GenreItem("Hardcore"),
        GenreItem("Hardstyle"),
        GenreItem("Heavy Metal"),
        GenreItem("Hi-NRG"),
        GenreItem("Hip Hop"),
        GenreItem("Horrorcore"),
        GenreItem("House"),
        GenreItem("Hyperpop"),
        GenreItem("IDM"),
        GenreItem("Indie"),
        GenreItem("Indie Folk"),
        GenreItem("Indie Pop"),
        GenreItem("Indie Rock"),
        GenreItem("Industrial"),
        GenreItem("Italo Disco"),
        GenreItem("J-Pop"),
        GenreItem("J-Rock"),
        GenreItem("Jazz"),
        GenreItem("Jazz Fusion"),
        GenreItem("Jazz Rock"),
        GenreItem("Jersey Club"),
        GenreItem("K-Pop"),
        GenreItem("Krautrock"),
        GenreItem("Latin"),
        GenreItem("Latin Jazz"),
        GenreItem("Latin Pop"),
        GenreItem("Lo-Fi"),
        GenreItem("Math Rock"),
        GenreItem("Melodic Death Metal"),
        GenreItem("Metal"),
        GenreItem("Metalcore"),
        GenreItem("Minimal Techno"),
        GenreItem("Mumble Rap"),
        GenreItem("Neoclassical"),
        GenreItem("Neo-Soul"),
        GenreItem("New Age"),
        GenreItem("New Wave"),
        GenreItem("Noise"),
        GenreItem("Nu Metal"),
        GenreItem("Opera"),
        GenreItem("Orchestral"),
        GenreItem("Phonk"),
        GenreItem("Plugg"),
        GenreItem("Pop"),
        GenreItem("Pop Punk"),
        GenreItem("Pop Rock"),
        GenreItem("Post-Punk"),
        GenreItem("Post-Rock"),
        GenreItem("Power Metal"),
        GenreItem("Progressive House"),
        GenreItem("Progressive Metal"),
        GenreItem("Progressive Rock"),
        GenreItem("Psychedelic Rock"),
        GenreItem("Psytrance"),
        GenreItem("Punk"),
        GenreItem("Punk Rock"),
        GenreItem("R&B"),
        GenreItem("Rap"),
        GenreItem("Reggae"),
        GenreItem("Reggaeton"),
        GenreItem("Rock"),
        GenreItem("Rock and Roll"),
        GenreItem("Rockabilly"),
        GenreItem("Salsa"),
        GenreItem("Samba"),
        GenreItem("Schlager"),
        GenreItem("Shoegaze"),
        GenreItem("Sigilkore"),
        GenreItem("Ska"),
        GenreItem("Soul"),
        GenreItem("Southern Rock"),
        GenreItem("Speed Metal"),
        GenreItem("Surf Rock"),
        GenreItem("Swing"),
        GenreItem("Synth-Pop"),
        GenreItem("Synthwave"),
        GenreItem("Tango"),
        GenreItem("Tech House"),
        GenreItem("Techno"),
        GenreItem("Thrash Metal"),
        GenreItem("Trance"),
        GenreItem("Trap"),
        GenreItem("Trip Hop"),
        GenreItem("UK Drill"),
        GenreItem("UK Garage"),
        GenreItem("Vaporwave"),
        GenreItem("Visual Kei"),
        GenreItem("West Coast Hip Hop"),
        GenreItem("Western Classical"),
        GenreItem("Zydeco"),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenresScreen(navController: NavController) {
    var expanded by remember { mutableStateOf(false) }
    val displayList = if (expanded) ALL_GENRES else TOP_15_GENRES

    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by
        remember(playerConnection) {
                playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null)
            }
            .collectAsStateWithLifecycle()
    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
        )

    val gridState = rememberLazyGridState()
    val frostBackdrop = rememberFrostBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        IridePageLayout(
            background = {
                if (mainTopGradient) {
                    TopScreenGradientBackground(
                        mediaMetadata = mediaMetadata,
                        playerBackground = playerBackgroundStyle,
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                }
            },
            state = gridState,
            columns = GridCells.Fixed(2),
            title = stringResource(R.string.genres_find),
            showTopBar = true,
            navigationIcon = if (gestureNavigationEnabled()) null else ({ IridePageBackButton(navController) }),
            bottomPadding =
                LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!expanded) {
                item(span = { GridItemSpan(2) }, key = "top15_label") {
                    Text(
                        text = stringResource(R.string.genres_top_fifteen),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            } else {
                item(span = { GridItemSpan(2) }, key = "all_label") {
                    Text(
                        text = stringResource(R.string.genres_all),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            items(items = displayList, key = { it.name + if (expanded) "_all" else "_top" }) { genre
                ->
                GenreChip(
                    genre = genre,
                    onClick = {
                        if (genre.name == "Rock") {
                            navController.navigate("rock_module")
                        } else {
                            // TODO: no filter pre-selection mechanism in OnlineSearchResult (filter
                            // resets to null on launch)
                            navController.navigate(
                                "search/${URLEncoder.encode(genre.name, "UTF-8")}"
                            )
                        }
                    },
                )
            }

            if (!expanded) {
                item(span = { GridItemSpan(2) }, key = "expand_btn") {
                    TextButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    ) {
                        Text(stringResource(R.string.genres_show_all))
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreChip(genre: GenreItem, onClick: () -> Unit) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(56.dp)
                .clip(FolioShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onClick)
    ) {
        Text(
            text = stringResource(GENRE_LABEL_RESOURCES[genre.name] ?: R.string.genre_pop),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 12.dp),
            maxLines = 1,
        )
    }
}
