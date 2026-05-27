/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.utils.backToMain
import java.net.URLEncoder

private data class GenreItem(val name: String, val hasPlus: Boolean)

private val TOP_15_GENRES = listOf(
    GenreItem("Pop", false),
    GenreItem("Hip Hop", true),
    GenreItem("Rap", true),
    GenreItem("Rock", true),
    GenreItem("Trap", true),
    GenreItem("EDM", false),
    GenreItem("Reggaeton", true),
    GenreItem("R&B", true),
    GenreItem("K-Pop", true),
    GenreItem("Metal", true),
    GenreItem("Country", true),
    GenreItem("Electronic", false),
    GenreItem("Indie", true),
    GenreItem("Latin", true),
    GenreItem("Techno", false),
)

private val ALL_GENRES = listOf(
    GenreItem("Acid House", false), GenreItem("Acid Jazz", false), GenreItem("Acid Rock", false),
    GenreItem("Acoustic", false), GenreItem("Afrobeat", true), GenreItem("Afrobeats", true),
    GenreItem("Alternative", false), GenreItem("Alternative Metal", false), GenreItem("Alternative Rock", false),
    GenreItem("Ambient", false), GenreItem("Amapiano", true), GenreItem("Ambient Techno", false),
    GenreItem("Anarcho Punk", false), GenreItem("Art Rock", false), GenreItem("Avant-garde", false),
    GenreItem("Bachata", true), GenreItem("Balearic", false), GenreItem("Bass House", false),
    GenreItem("Big Band", false), GenreItem("Black Metal", true), GenreItem("Blues", true),
    GenreItem("Blues Rock", false), GenreItem("Boogie", false), GenreItem("Bossa Nova", true),
    GenreItem("Breakbeat", false), GenreItem("Breakcore", false), GenreItem("Britpop", true),
    GenreItem("Brazilian Funk", true), GenreItem("Brazilian Phonk", true),
    GenreItem("C-Pop", true), GenreItem("Celtic", true), GenreItem("Chamber Music", false),
    GenreItem("Chillout", false), GenreItem("Chillwave", false), GenreItem("Christian Rock", false),
    GenreItem("Classical", true), GenreItem("Cloud Rap", false), GenreItem("Contemporary R&B", false),
    GenreItem("Country", true), GenreItem("Country Rock", false), GenreItem("Crunk", false),
    GenreItem("Cumbia", true),
    GenreItem("Dance", false), GenreItem("Dance-Pop", false), GenreItem("Dark Ambient", false),
    GenreItem("Darkwave", false), GenreItem("Death Metal", true), GenreItem("Deathcore", false),
    GenreItem("Deep House", false), GenreItem("Disco", false), GenreItem("Doom Metal", true),
    GenreItem("Dream Pop", false), GenreItem("Drill", true), GenreItem("Drone", false),
    GenreItem("Drum and Bass", false), GenreItem("Dub", true), GenreItem("Dubstep", false),
    GenreItem("Dungeon Synth", false),
    GenreItem("East Coast Hip Hop", true), GenreItem("EDM", false), GenreItem("Electro", false),
    GenreItem("Electroclash", false), GenreItem("Electronic", false), GenreItem("Electronica", false),
    GenreItem("Emo", false), GenreItem("Emo Rap", false), GenreItem("Epicore", false),
    GenreItem("Eurodance", false), GenreItem("Europop", false), GenreItem("Experimental", false),
    GenreItem("Folk", true), GenreItem("Folk Rock", true), GenreItem("Folktronica", false),
    GenreItem("Freestyle", false), GenreItem("Funk", true), GenreItem("Funk Rock", false),
    GenreItem("Future Bass", false), GenreItem("Fusion", false),
    GenreItem("Gabber", false), GenreItem("Gangsta Rap", true), GenreItem("Garage Rock", true),
    GenreItem("Glam Rock", false), GenreItem("Gospel", true), GenreItem("Gothic Rock", false),
    GenreItem("Grime", true), GenreItem("Grindcore", false), GenreItem("Grunge", true),
    GenreItem("Happy Hardcore", false), GenreItem("Hard Rock", true), GenreItem("Hardcore", false),
    GenreItem("Hardstyle", false), GenreItem("Heavy Metal", true), GenreItem("Hi-NRG", false),
    GenreItem("Hip Hop", true), GenreItem("Horrorcore", false), GenreItem("House", false),
    GenreItem("Hyperpop", false),
    GenreItem("IDM", false), GenreItem("Indie", true), GenreItem("Indie Folk", true),
    GenreItem("Indie Pop", true), GenreItem("Indie Rock", true), GenreItem("Industrial", false),
    GenreItem("Italo Disco", true),
    GenreItem("J-Pop", true), GenreItem("J-Rock", true), GenreItem("Jazz", true),
    GenreItem("Jazz Fusion", false), GenreItem("Jazz Rock", false), GenreItem("Jersey Club", false),
    GenreItem("K-Pop", true), GenreItem("Krautrock", true),
    GenreItem("Latin", true), GenreItem("Latin Jazz", false), GenreItem("Latin Pop", true),
    GenreItem("Lo-Fi", false),
    GenreItem("Math Rock", false), GenreItem("Melodic Death Metal", true), GenreItem("Metal", true),
    GenreItem("Metalcore", true), GenreItem("Minimal Techno", false), GenreItem("Mumble Rap", false),
    GenreItem("Neoclassical", false), GenreItem("Neo-Soul", false), GenreItem("New Age", false),
    GenreItem("New Wave", false), GenreItem("Noise", false), GenreItem("Nu Metal", false),
    GenreItem("Opera", true), GenreItem("Orchestral", false),
    GenreItem("Phonk", false), GenreItem("Plugg", false), GenreItem("Pop", true),
    GenreItem("Pop Punk", true), GenreItem("Pop Rock", true), GenreItem("Post-Punk", false),
    GenreItem("Post-Rock", false), GenreItem("Power Metal", true), GenreItem("Progressive House", false),
    GenreItem("Progressive Metal", true), GenreItem("Progressive Rock", true),
    GenreItem("Psychedelic Rock", true), GenreItem("Psytrance", false), GenreItem("Punk", true),
    GenreItem("Punk Rock", true),
    GenreItem("R&B", true), GenreItem("Rap", true), GenreItem("Reggae", true),
    GenreItem("Reggaeton", true), GenreItem("Rock", true), GenreItem("Rock and Roll", true),
    GenreItem("Rockabilly", true),
    GenreItem("Salsa", true), GenreItem("Samba", true), GenreItem("Schlager", true),
    GenreItem("Shoegaze", false), GenreItem("Sigilkore", false), GenreItem("Ska", true),
    GenreItem("Soul", true), GenreItem("Southern Rock", true), GenreItem("Speed Metal", false),
    GenreItem("Surf Rock", false), GenreItem("Swing", false), GenreItem("Synth-Pop", false),
    GenreItem("Synthwave", false),
    GenreItem("Tango", true), GenreItem("Tech House", false), GenreItem("Techno", false),
    GenreItem("Thrash Metal", true), GenreItem("Trance", false), GenreItem("Trap", true),
    GenreItem("Trip Hop", false),
    GenreItem("UK Drill", true), GenreItem("UK Garage", true),
    GenreItem("Vaporwave", false), GenreItem("Visual Kei", true),
    GenreItem("West Coast Hip Hop", true), GenreItem("Western Classical", false),
    GenreItem("Zydeco", true),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenresScreen(navController: NavController) {
    var expanded by remember { mutableStateOf(false) }
    val displayList = if (expanded) ALL_GENRES else TOP_15_GENRES

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 12.dp,
            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!expanded) {
            item(span = { GridItemSpan(2) }, key = "top15_label") {
                Text(
                    text = "Top 15 Most Listened",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        } else {
            item(span = { GridItemSpan(2) }, key = "all_label") {
                Text(
                    text = "All Genres (A–Z)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }

        items(items = displayList, key = { it.name + if (expanded) "_all" else "_top" }) { genre ->
            GenreChip(
                genre = genre,
                onClick = {
                    // TODO: no filter pre-selection mechanism in OnlineSearchResult (filter resets to null on launch)
                    navController.navigate("search/${URLEncoder.encode(genre.name, "UTF-8")}")
                },
            )
        }

        if (!expanded) {
            item(span = { GridItemSpan(2) }, key = "expand_btn") {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Text("Show all genres ↓")
                }
            }
        }
    }

    TopAppBar(
        title = { Text("Find your genres") },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun GenreChip(genre: GenreItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = genre.name,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 12.dp),
            maxLines = 1,
        )
        if (genre.hasPlus) {
            Text(
                text = "+",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 4.dp),
            )
        }
    }
}
