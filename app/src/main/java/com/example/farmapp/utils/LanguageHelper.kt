import android.content.Context
import java.util.Locale

object LanguageHelper {
    fun setLocale(context: Context, lang: String): Context {
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        // For newer versions
        val updatedContext = context.createConfigurationContext(config)
        
        // For compatibility (sometimes needed for strings)
        resources.updateConfiguration(config, resources.displayMetrics)
        
        return updatedContext
    }
}

