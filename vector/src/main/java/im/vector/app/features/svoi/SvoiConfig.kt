/*
 * СВОи Мессенджер — Конфигурация монетизации
 */
package im.vector.app.features.svoi

object SvoiConfig {
    // Demo режим: все платные функции бесплатны
    const val DEMO_MODE: Boolean = true

    // Admin учётная запись (всегда полный доступ)
    const val ADMIN_EMAIL: String = "den67635430@gmail.com"

    // Цены подписок (в рублях)
    const val FREE_PRICE: Int = 0
    const val PREMIUM_PRICE: Int = 449
    const val BUSINESS_PRICE: Int = 749

    // API endpoint (VPS backend)
    const val API_ENDPOINT: String = "http://svo.kodkontenta.ru:8080"

    // SharedPreferences keys
    const val PREF_SUBSCRIPTION_TIER = "svoi_subscription_tier"
    const val PREF_SUBSCRIPTION_EXPIRES = "svoi_subscription_expires"
    const val PREF_USER_EMAIL = "svoi_user_email"
    const val PREF_IS_ADMIN = "svoi_is_admin"
}

enum class SubscriptionTier(val displayName: String, val price: Int) {
    FREE("Free", SvoiConfig.FREE_PRICE),
    PREMIUM("Premium", SvoiConfig.PREMIUM_PRICE),
    BUSINESS("Business", SvoiConfig.BUSINESS_PRICE);

    fun hasAccess(feature: PremiumFeature): Boolean {
        return feature.minTier.ordinal <= this.ordinal
    }
}

enum class PremiumFeature(val minTier: SubscriptionTier, val displayName: String) {
    // Free
    READ_RECEIPTS(SubscriptionTier.FREE, "Видеть когда прочитано"),
    MESSAGE_EDITING(SubscriptionTier.FREE, "Редактирование сообщений"),
    AUTO_DELETE(SubscriptionTier.FREE, "Автоудаление сообщений"),
    // Premium
    CUSTOM_STICKERS(SubscriptionTier.PREMIUM, "Кастомные стикеры"),
    CUSTOM_BUBBLES(SubscriptionTier.PREMIUM, "Кастомные пузырьки"),
    UNLIMITED_FILES(SubscriptionTier.PREMIUM, "Неограниченные файлы"),
    SCHEDULED_MESSAGES(SubscriptionTier.PREMIUM, "Отправка по расписанию"),
    PREMIUM_BADGE(SubscriptionTier.PREMIUM, "Премиум-бейдж"),
    // Business
    PRIORITY_SUPPORT(SubscriptionTier.BUSINESS, "Приоритетная поддержка"),
    ANALYTICS(SubscriptionTier.BUSINESS, "Аналитика и статистика"),
    API_ACCESS(SubscriptionTier.BUSINESS, "API для интеграций"),
    CUSTOM_DOMAIN(SubscriptionTier.BUSINESS, "Кастомные домены"),
    BULK_MESSAGING(SubscriptionTier.BUSINESS, "Массовая рассылка"),
    BACKUP(SubscriptionTier.BUSINESS, "Резервные копии"),
}
