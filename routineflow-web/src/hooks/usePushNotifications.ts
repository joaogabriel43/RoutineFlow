import { useState, useEffect, useCallback } from 'react'
import { pushApi } from '@/services/api'

/**
 * Helper to convert a base64 URL-safe string to a Uint8Array.
 * Required for the applicationServerKey parameter of PushManager.subscribe().
 */
function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const rawData = window.atob(base64)
  const outputArray = new Uint8Array(rawData.length)
  for (let i = 0; i < rawData.length; i++) {
    outputArray[i] = rawData.charCodeAt(i)
  }
  return outputArray
}

export function usePushNotifications() {
  const isSupported =
    typeof window !== 'undefined' &&
    'Notification' in window &&
    'serviceWorker' in navigator &&
    'PushManager' in window

  const [permission, setPermission] = useState<NotificationPermission>(
    isSupported ? Notification.permission : 'denied',
  )
  const [isSubscribed, setIsSubscribed] = useState(false)
  const [loading, setLoading] = useState(false)

  // Check if already subscribed on mount
  useEffect(() => {
    if (!isSupported) return

    const checkSubscription = async () => {
      try {
        const registration = await navigator.serviceWorker.ready
        const subscription = await registration.pushManager.getSubscription()
        setIsSubscribed(subscription !== null)
      } catch {
        // SW not ready yet — ignore
      }
    }
    void checkSubscription()
  }, [isSupported])

  const requestPermission = useCallback(async (): Promise<NotificationPermission> => {
    if (!isSupported) return 'denied'
    const result = await Notification.requestPermission()
    setPermission(result)
    return result
  }, [isSupported])

  const subscribe = useCallback(async (): Promise<boolean> => {
    if (!isSupported) return false
    setLoading(true)

    try {
      // 1. Request permission if not granted
      let perm = Notification.permission
      if (perm === 'default') {
        perm = await Notification.requestPermission()
        setPermission(perm)
      }
      if (perm !== 'granted') {
        setLoading(false)
        return false
      }

      // 2. Get VAPID public key from backend
      const vapidKey = await pushApi.getVapidPublicKey()
      if (!vapidKey || vapidKey === 'Push not configured') {
        setLoading(false)
        return false
      }

      // 3. Subscribe via PushManager
      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(vapidKey),
      })

      // 4. Extract keys and send to backend
      const key = subscription.getKey('p256dh')
      const auth = subscription.getKey('auth')
      if (!key || !auth) {
        setLoading(false)
        return false
      }

      const p256dh = btoa(String.fromCharCode(...new Uint8Array(key as ArrayBuffer)))
      const authStr = btoa(String.fromCharCode(...new Uint8Array(auth as ArrayBuffer)))

      await pushApi.subscribe({
        endpoint: subscription.endpoint,
        p256dh,
        auth: authStr,
      })

      setIsSubscribed(true)
      return true
    } catch (err) {
      console.error('Push subscription failed:', err)
      return false
    } finally {
      setLoading(false)
    }
  }, [isSupported])

  const unsubscribe = useCallback(async (): Promise<boolean> => {
    if (!isSupported) return false
    setLoading(true)

    try {
      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.getSubscription()

      if (subscription) {
        // Notify backend
        await pushApi.unsubscribe(subscription.endpoint).catch(() => {
          // Backend may already have removed it — ignore
        })
        await subscription.unsubscribe()
      }

      setIsSubscribed(false)
      return true
    } catch (err) {
      console.error('Push unsubscribe failed:', err)
      return false
    } finally {
      setLoading(false)
    }
  }, [isSupported])

  return {
    isSupported,
    permission,
    isSubscribed,
    loading,
    requestPermission,
    subscribe,
    unsubscribe,
  }
}
