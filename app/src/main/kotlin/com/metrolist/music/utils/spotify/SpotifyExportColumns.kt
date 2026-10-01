package com.metrolist.music.utils.spotify

import java.util.Locale

/** Column labels from the official Exportify translations, plus its earlier English schema. */
internal object SpotifyExportColumns {
    val titles = setOf(
        "اسم الأغنية",
        "Track-Name",
        "Όνομα κομματιού",
        "Track Name",
        "Nombre de la canción",
        "Nom du titre",
        "Nome della traccia",
        "トラック名",
        "Nummernaam",
        "Nome da faixa",
        "Låtens namn",
        "Parça Adı",
    ).map { it.lowercase(Locale.ROOT) }.toSet()

    val artists = setOf(
        "أسماء الفنانين",
        "Künstlername(n)",
        "Όνομα/τα καλλιτέχνη",
        "Artist Name(s)",
        "Nombre(s) del artista",
        "Nom(s) de l'artiste",
        "Nome dell'artista",
        "アーティスト名",
        "Naam van artiest",
        "Nome(s) do artista",
        "Artistens namn",
        "Sanatçı Adı",
    ).map { it.lowercase(Locale.ROOT) }.toSet()

    val durations = setOf(
        "مدة الأغنية (بالمللي ثانية)",
        "Track-Dauer (ms)",
        "Διάρκεια κομματιού (ms)",
        "Track Duration (ms)",
        "Duración de la canción (ms)",
        "Durée du titre (ms)",
        "Durata della traccia (ms)",
        "トラックの長さ（ミリ秒）",
        "Nummerduur (ms)",
        "Duração da faixa (ms)",
        "Låtlängd (ms)",
        "Parça Süresi (ms)",
        "Duration (ms)",
    ).map { it.lowercase(Locale.ROOT) }.toSet()

    val uris = setOf(
        "رابط الأغنية (URI)",
        "Track-URI",
        "URI κομματιού",
        "Track URI",
        "URI de la canción",
        "URI du titre",
        "URI della traccia",
        "トラックURI",
        "Nummer URI",
        "URI da faixa",
        "Låtens URI",
        "Parça URI",
    ).map { it.lowercase(Locale.ROOT) }.toSet()

}
