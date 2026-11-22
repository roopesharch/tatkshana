package com.example.tatkshana

import android.content.Context
import android.net.Uri
import java.lang.reflect.Modifier

data class BundledRingtone(
    val id: String,
    val displayName: String,
    val rawResName: String
)

object BundledRingtones {

    // Optional friendly names for files in app/src/main/res/raw/
    // Any file in res/raw/ is automatically detected even if not listed here.
    private val customLabels: Map<String, String> = mapOf(
        "tatkshana_default" to "Tatkshana Default Ringtone",
        "focus_chime" to "Focus Chime",
        "urgent_siren" to "Urgent Siren"
    )

    fun getAvailableRingtones(context: Context): List<BundledRingtone> {
        val found = mutableListOf<BundledRingtone>()
        try {
            val rawClass = Class.forName("${context.packageName}.R\$raw")
            for (field in rawClass.declaredFields) {
                if (Modifier.isStatic(field.modifiers) && field.type == Int::class.javaPrimitiveType) {
                    val resName = field.name
                    val title = customLabels[resName] ?: formatResourceName(resName)
                    found.add(
                        BundledRingtone(
                            id = resName,
                            displayName = title,
                            rawResName = resName
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }

        if (found.isEmpty()) {
            // Fallback entries so the dropdown always shows options even before res/raw files are added
            return customLabels.map { (resName, label) ->
                BundledRingtone(id = resName, displayName = label, rawResName = resName)
            }
        }
        return found.sortedBy { it.displayName.lowercase() }
    }

    fun resolveUri(context: Context, rawResName: String): Uri? {
        val resId = context.resources.getIdentifier(rawResName, "raw", context.packageName)
        if (resId == 0) return null
        return Uri.parse("android.resource://${context.packageName}/$resId")
    }

    private fun formatResourceName(rawName: String): String {
        return rawName
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { ch -> ch.uppercase() }
            }
    }
}