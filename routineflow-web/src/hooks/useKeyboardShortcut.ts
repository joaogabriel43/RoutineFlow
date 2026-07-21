import { useEffect } from 'react'

type ShortcutHandler = (e: KeyboardEvent) => void

interface ShortcutOptions {
  preventDefault?: boolean
}

export function useKeyboardShortcut(
  keys: string[],
  callback: ShortcutHandler,
  options: ShortcutOptions = { preventDefault: true }
) {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Check if the pressed key is in the target keys array
      const keyMatch = keys.some(k => event.key.toLowerCase() === k.toLowerCase())
      
      // Check for modifier keys if they are part of the target shortcut
      const ctrlMatch = keys.includes('ctrl') ? event.ctrlKey || event.metaKey : true
      
      if (keyMatch && ctrlMatch) {
        // Only trigger if we're not typing in an input field (unless it's a global shortcut like Escape)
        const target = event.target as HTMLElement
        const isInput = target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable
        
        // Let Escape bypass the input check so we can close modals
        if (isInput && event.key !== 'Escape') return

        if (options.preventDefault) {
          event.preventDefault()
        }
        callback(event)
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [keys, callback, options])
}
