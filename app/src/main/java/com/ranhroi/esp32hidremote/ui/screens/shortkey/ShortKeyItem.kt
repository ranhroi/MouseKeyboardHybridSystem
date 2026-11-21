package com.ranhroi.esp32hidremote.ui.screens.shortkey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ranhroi.esp32hidremote.models.ShortKeyModel
import com.ranhroi.esp32hidremote.ui.components.IconAny
import com.ranhroi.esp32hidremote.utils.ConstantsUtils
import com.ranhroi.esp32hidremote.utils.IconsMap


@Composable
fun ShortKeyItem(shortKey: ShortKeyModel) {
    val icon = remember(shortKey.icon) { IconsMap.getIconOrDefault(shortKey.icon) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyColumn {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconAny(
                        text = shortKey.name,
                        icon = icon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = shortKey.name,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            itemsIndexed(shortKey.keys) { index, key ->
                Text(
                    text = "${ConstantsUtils.getOsLabel(index)}: $key",
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

        }
    }
}
