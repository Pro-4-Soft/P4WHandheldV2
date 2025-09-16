package com.p4handheld.ui.compose

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.p4handheld.R

val FontAwesome = FontFamily(Font(R.font.font_awesome))

fun getFontAwesomeIcon(icon: String): String {
    return when (icon) {
        "fa-folder" -> "\uf07b"
        "fa-tasks" -> "\uf0ae"
        "fa-truck-loading" -> "\uf4de"
        "fa-camera" -> "\uf030"
        "fa-barcode" -> "\uf02a"
        "fa-exclamation-triangle" -> "\uf071"
        "fa-box" -> "\uf466"
        "fa-boxes" -> "\uf468"
        "fa-pallet" -> "\uf482"
        "fa-user" -> "\uf007"
        "fa-box-open" -> "\uf49e"
        "fa-clipboard-check" -> "\uf46c"
        "fa-cart-arrow-down" -> "\uf218"
        "fa-clipboard-list" -> "\uf46d"
        "fa-print" -> "\uf02f"
        "fa-shipping-fast" -> "\uf48b"
        "fa-arrow-alt-circle-left" -> "\uf359"
        "fa-shopping-cart" -> "\uf07a"
        "fa-cogs" -> "\uf085"
        "fa-arrow-left" -> "\uf060"
        "fa-align-justify" -> "\uf039"
        "fa-exchange-alt" -> "\uf362"
        "fa-ruler-combined" -> "\uf546"
        "fa-comments" -> "\uf086"
        else -> "\uf128" // default: question mark
    }
}