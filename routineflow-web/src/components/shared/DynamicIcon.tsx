import { useEffect, useState } from 'react'

/**
 * Renders a lucide icon by its kebab-case name (what the backend stores).
 *
 * The lucide dynamic engine (`lucide-react/dynamic`) ships an index of ~1900
 * lazy `import()`s (~120 KB gzip). To keep that OUT of the main bundle, we load
 * the engine itself lazily (in parallel, after first paint) and cache it as a
 * module singleton. Individual icon components are then code-split on demand —
 * only the icons the user's areas/tasks actually use get fetched.
 *
 * Safe by construction: a null name, a legacy emoji ("⭐"), or any string that
 * isn't a real lucide icon falls back to `fallback` without throwing.
 */

type DynModule = typeof import('lucide-react/dynamic')

let enginePromise: Promise<DynModule> | null = null
let engineModule: DynModule | null = null
let validIcons: Set<string> | null = null

function loadEngine(): Promise<DynModule> {
  if (!enginePromise) {
    enginePromise = import('lucide-react/dynamic').then((mod) => {
      validIcons = new Set<string>(mod.iconNames as readonly string[])
      engineModule = mod
      return mod
    })
  }
  return enginePromise
}

interface DynamicIconProps {
  name: string | null | undefined
  size?: number
  color?: string
  className?: string
  /** lucide kebab name used when `name` is missing/invalid. Default "circle". */
  fallback?: string
}

export function DynamicIcon({
  name,
  size = 20,
  color,
  className,
  fallback = 'circle',
}: DynamicIconProps) {
  // If the engine is already loaded (singleton), mount with it — no flicker.
  const [engine, setEngine] = useState<DynModule | null>(engineModule)

  useEffect(() => {
    if (engine) return
    let active = true
    loadEngine().then((mod) => {
      if (active) setEngine(mod)
    })
    return () => {
      active = false
    }
  }, [engine])

  // Reserve space until the engine resolves — avoids layout shift / flicker.
  if (!engine || !validIcons) {
    return <span aria-hidden style={{ display: 'inline-block', width: size, height: size }} />
  }

  const { DynamicIcon: LucideDynamicIcon, iconNames } = engine
  const resolved =
    name && validIcons.has(name)
      ? name
      : validIcons.has(fallback)
        ? fallback
        : 'circle'

  return (
    <LucideDynamicIcon
      name={resolved as (typeof iconNames)[number]}
      size={size}
      color={color}
      className={className}
    />
  )
}
