/**
 * Curated icon catalog for the IconPicker (Sprint 26).
 *
 * Every name here is a valid lucide-react kebab-case icon name — validated
 * against the installed lucide-react package. The IconPicker shows these
 * categories by default (no search needed), with full search as fallback.
 */

export const ICON_CATEGORIES: Record<string, readonly string[]> = {
  'Saúde & Fitness': [
    'dumbbell', 'heart', 'heart-pulse', 'activity',
    'footprints', 'bike', 'apple', 'salad',
    'pill', 'bed', 'droplet', 'flame',
  ],
  'Estudo': [
    'book-open', 'graduation-cap', 'pencil', 'notebook',
    'lightbulb', 'brain', 'languages', 'library',
  ],
  'Trabalho': [
    'briefcase', 'laptop', 'monitor', 'code',
    'mail', 'phone', 'calendar', 'clipboard-check',
    'target', 'trending-up',
  ],
  'Casa & Rotina': [
    'house', 'shower-head', 'shirt', 'utensils',
    'coffee', 'shopping-cart', 'dog', 'cat', 'sprout',
  ],
  'Lazer & Hobbies': [
    'gamepad-2', 'music', 'headphones', 'palette',
    'camera', 'film', 'tv', 'guitar', 'plane',
  ],
  'Bem-estar': [
    'smile', 'sun', 'moon', 'sparkles',
    'leaf', 'flower', 'hand-heart',
  ],
  'Finanças': [
    'dollar-sign', 'wallet', 'piggy-bank', 'credit-card', 'coins',
  ],
  'Geral': [
    'users', 'message-circle', 'gift', 'star',
    'bell', 'map-pin', 'check', 'zap', 'folder', 'circle',
  ],
} as const

/** Flat list of all curated icon names (for quick lookup). */
export const CURATED_ICONS: readonly string[] = Object.values(ICON_CATEGORIES).flat()
