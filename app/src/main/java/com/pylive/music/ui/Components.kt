package com.pylive.music.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pylive.music.R
import com.pylive.music.domain.Track
import com.pylive.music.domain.formatDuration
import com.pylive.music.ui.theme.PylivePalette

/**
 * Bibliothèque de composants Pylive Music.
 *
 * Règles appliquées partout :
 *  • texte principal ≥ 15 sp, texte secondaire ≥ 13 sp et jamais gris clair ;
 *  • cibles tactiles ≥ 48 dp ;
 *  • chaque action possède un libellé visible ou une description accessible ;
 *  • le violet ne porte que du texte blanc, le jaune ne porte que du texte foncé.
 */

/** Icône du jeu maison, teintée explicitement (aucun tint système imprévisible). */
@Composable
fun PyliveIcon(
    @DrawableRes id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp
) {
    Icon(
        painter = painterResource(id),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint
    )
}

/** Pochette d'album, avec repli sur la marque Pylive quand aucune image n'existe. */
@Composable
fun TrackCover(
    artworkUri: String?,
    title: String,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(PylivePalette.VioletSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_note),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.46f),
                tint = PylivePalette.Violet
            )
        }
        if (!artworkUri.isNullOrBlank()) {
            AsyncImage(
                model = artworkUri,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
    }
}

/** En-tête de section : titre lisible + action facultative. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    @DrawableRes actionIcon: Int? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 26.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            PyliveTextButton(
                text = actionLabel,
                icon = actionIcon,
                onClick = onAction
            )
        }
    }
}

/** Bouton principal : fond violet, texte blanc, coins arrondis, libellé explicite. */
@Composable
fun PyliveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) PylivePalette.Violet else PylivePalette.OutlineSoft,
        contentColor = if (enabled) PylivePalette.InkOnViolet else PylivePalette.InkSoft,
        shadowElevation = if (enabled) 0.dp else 0.dp
    ) {
        Row(
            Modifier
                .height(50.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                PyliveIcon(icon, null, tint = LocalContentColor.current, size = 20.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Bouton secondaire : contour, texte violet sur fond blanc (contraste 5,5:1). */
@Composable
fun PyliveOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = if (enabled) PylivePalette.Violet else PylivePalette.InkSoft,
        border = BorderStroke(1.5.dp, if (enabled) PylivePalette.Violet.copy(alpha = 0.55f) else PylivePalette.Outline)
    ) {
        Row(
            Modifier
                .height(48.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                PyliveIcon(icon, null, tint = LocalContentColor.current, size = 19.dp)
                Spacer(Modifier.width(9.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** Lien d'action textuel : minimum 44 dp de haut, libellé toujours explicite. */
@Composable
fun PyliveTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        contentColor = if (enabled) PylivePalette.Violet else PylivePalette.InkSoft
    ) {
        Row(
            Modifier
                .height(44.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                PyliveIcon(icon, null, size = 18.dp)
                Spacer(Modifier.width(7.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** Bouton d'icône avec description accessible et cible tactile 48 dp. */
@Composable
fun PyliveIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    background: Color = Color.Transparent
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = background,
        contentColor = tint
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PyliveIcon(
                id = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else PylivePalette.Outline,
                size = iconSize
            )
        }
    }
}

/** Pastille d'état : violet plein / blanc, ou jaune avec texte foncé. */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Violet
) {
    val background = when (style) {
        PillStyle.Violet -> PylivePalette.VioletSoft
        PillStyle.Yellow -> PylivePalette.Yellow
        PillStyle.Outline -> Color.Transparent
        PillStyle.Dark -> PylivePalette.Violet
    }
    val content = when (style) {
        PillStyle.Violet -> PylivePalette.VioletDeep
        PillStyle.Yellow -> PylivePalette.Ink
        PillStyle.Outline -> PylivePalette.InkSoft
        PillStyle.Dark -> PylivePalette.White
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = background,
        contentColor = content,
        border = if (style == PillStyle.Outline) BorderStroke(1.dp, PylivePalette.Outline) else null
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 1
        )
    }
}

enum class PillStyle { Violet, Yellow, Outline, Dark }

/** Carte de surface standard. */
@Composable
fun PyliveCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, PylivePalette.OutlineSoft)
    ) {
        content()
    }
}

/**
 * Ligne de morceau de la bibliothèque.
 * Toute la ligne est cliquable pour lire ; le cœur et le menu restent distincts
 * avec des cibles tactiles de 48 dp et des descriptions accessibles.
 */
@Composable
fun TrackRow(
    track: Track,
    isFavorite: Boolean,
    compact: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEnqueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = if (compact) 6.dp else 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!compact) {
            TrackCover(track.artworkUri, track.title, Modifier.size(54.dp), corner = 12.dp)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPlaying) {
                    NowPlayingBars(Modifier.padding(end = 8.dp))
                }
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPlaying) PylivePalette.VioletDeep else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${track.artist} • ${track.durationLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing()
        PyliveIconButton(
            icon = if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart,
            contentDescription = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
            onClick = onToggleFavorite,
            tint = if (isFavorite) PylivePalette.Violet else MaterialTheme.colorScheme.onSurfaceVariant
        )
        TrackMenuButton(
            onEnqueue = onEnqueue,
            onAddToPlaylist = onAddToPlaylist,
            onShowDetails = onShowDetails
        )
    }
}

/** Menu d'options d'un morceau : libellés complets, pas de simples icônes. */
@Composable
fun TrackMenuButton(
    onEnqueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowDetails: () -> Unit
) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box {
        PyliveIconButton(
            icon = R.drawable.ic_more,
            contentDescription = "Options du morceau",
            onClick = { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Ajouter à la file d’attente", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { PyliveIcon(R.drawable.ic_queue, null, size = 20.dp) },
                onClick = { expanded = false; onEnqueue() }
            )
            DropdownMenuItem(
                text = { Text("Ajouter à une playlist", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { PyliveIcon(R.drawable.ic_playlist, null, size = 20.dp) },
                onClick = { expanded = false; onAddToPlaylist() }
            )
            DropdownMenuItem(
                text = { Text("Détails du morceau", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { PyliveIcon(R.drawable.ic_info, null, size = 20.dp) },
                onClick = { expanded = false; onShowDetails() }
            )
        }
    }
}

/** Trois barres animées indiquant le morceau en cours de lecture. */
@Composable
fun NowPlayingBars(modifier: Modifier = Modifier, color: Color = PylivePalette.Violet) {
    Canvas(modifier.size(width = 14.dp, height = 14.dp)) {
        val barWidth = size.width / 5f
        val heights = listOf(0.55f, 1f, 0.70f)
        heights.forEachIndexed { index, ratio ->
            val h = size.height * ratio
            drawRoundRect(
                color = color,
                topLeft = Offset(index * barWidth * 2f, size.height - h),
                size = androidx.compose.ui.geometry.Size(barWidth, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}

/** État vide illustré sans emoji : icône de la famille, titre, explication, action. */
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = PylivePalette.VioletSoft,
            modifier = Modifier.size(72.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PyliveIcon(icon, null, tint = PylivePalette.Violet, size = 32.dp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp)
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            PyliveButton(actionLabel, onAction)
        }
    }
}

/** Bandeau d'information ou d'erreur, toujours lisible (texte foncé sur fond doux). */
@Composable
fun InfoBanner(
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    val background = when (tone) {
        BannerTone.Info -> PylivePalette.VioletSoft
        BannerTone.Warning -> PylivePalette.Yellow
        BannerTone.Error -> PylivePalette.DangerSoft
    }
    val accent = when (tone) {
        BannerTone.Info -> PylivePalette.Violet
        BannerTone.Warning -> PylivePalette.Ink
        BannerTone.Error -> PylivePalette.Danger
    }
    val titleColor = when (tone) {
        BannerTone.Info -> PylivePalette.VioletDeep
        BannerTone.Warning -> PylivePalette.Ink
        BannerTone.Error -> Color(0xFF6E1B16)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = background,
        contentColor = PylivePalette.Ink
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PyliveIcon(
                    when (tone) {
                        BannerTone.Info -> R.drawable.ic_info
                        BannerTone.Warning -> R.drawable.ic_warning
                        BannerTone.Error -> R.drawable.ic_warning
                    },
                    null, tint = accent, size = 20.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
            }
            if (!body.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PylivePalette.Ink.copy(alpha = 0.86f)
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PyliveTextButton(actionLabel, onAction)
                    if (secondaryLabel != null && onSecondary != null) {
                        PyliveTextButton(secondaryLabel, onSecondary)
                    }
                }
            }
        }
    }
}

enum class BannerTone { Info, Warning, Error }

/** Séparateur discret. */
@Composable
fun SoftDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PylivePalette.OutlineSoft)
    )
}

/** Étiquette de la marque, réutilisée dans l'en-tête et le lecteur. */
@Composable
fun BrandMark(size: Dp = 34.dp, onLight: Boolean = false) {
    Icon(
        painter = painterResource(if (onLight) R.drawable.ic_brand_mark_light else R.drawable.ic_brand_mark),
        contentDescription = null,
        modifier = Modifier.size(size),
        tint = Color.Unspecified
    )
}

/** Texte avec chiffres alignés pour les durées. */
@Composable
fun DurationText(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        ),
        color = color,
        maxLines = 1
    )
}

@Composable
fun TrackDurationLabel(track: Track) = DurationText(formatDuration(track.durationMs))
