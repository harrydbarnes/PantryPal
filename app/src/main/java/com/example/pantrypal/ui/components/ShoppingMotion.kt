package com.example.pantrypal.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared spring motion for shopping and pantry lists, including their chip carousels. */
fun LazyItemScope.shoppingMotion(): Modifier = Modifier.animateItem(
    fadeInSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
    placementSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
    fadeOutSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
)

fun LazyListScope.expressiveItem(key: Any? = null, content: @Composable LazyItemScope.() -> Unit) {
    item(key = key) {
        val itemScope = this
        Column(shoppingMotion()) { content(itemScope) }
    }
}

fun <T> LazyListScope.expressiveItems(
    values: List<T>, key: ((T) -> Any)? = null,
    content: @Composable LazyItemScope.(T) -> Unit
) {
    items(values, key = key) { value ->
        val itemScope = this
        Column(shoppingMotion()) { content(itemScope, value) }
    }
}

fun <T> LazyListScope.expressiveItemsIndexed(
    values: List<T>, key: ((Int, T) -> Any)? = null,
    content: @Composable LazyItemScope.(Int, T) -> Unit
) {
    itemsIndexed(values, key = key) { index, value ->
        val itemScope = this
        Column(shoppingMotion()) { content(itemScope, index, value) }
    }
}

/** A single row has both rounded ends; adjoining rows have compact inner corners. */
fun shoppingSegmentShape(index: Int, count: Int) = RoundedCornerShape(
    topStart = if (index == 0) 20.dp else 4.dp,
    topEnd = if (index == 0) 20.dp else 4.dp,
    bottomStart = if (index == count - 1) 20.dp else 4.dp,
    bottomEnd = if (index == count - 1) 20.dp else 4.dp
)
