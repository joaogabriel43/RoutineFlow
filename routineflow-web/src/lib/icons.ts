/**
 * Icon name helpers (Sprint 25).
 *
 * Icons are STORED in kebab-case (e.g. "book-open") — matching lucide's own
 * `iconNames` from `lucide-react/dynamic`. lucide's static export is PascalCase
 * ("BookOpen"), so the IconPicker (which renders from the static export) converts
 * kebab → Pascal. DynamicIcon uses the kebab name directly and needs no conversion.
 *
 * This module imports NOTHING from lucide — keeping it out of the main bundle's
 * icon weight. Only the lazy IconPicker chunk pulls in the icon components.
 */

/** "book-open" → "BookOpen", "a-arrow-down" → "AArrowDown", "volume-2" → "Volume2". */
export function kebabToPascal(kebab: string): string {
  return kebab
    .split('-')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('')
}
