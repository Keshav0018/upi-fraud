import { useEffect, useRef } from 'react'
import { streamUrl } from './api'

/**
 * Subscribes to the backend's SSE stream and dispatches named events to the
 * handlers given. Handlers are read from a ref on every call so callers don't
 * need to memoize them or reconnect when they change.
 */
export function useEventStream(handlers) {
  const handlersRef = useRef(handlers)
  handlersRef.current = handlers

  useEffect(() => {
    const source = new EventSource(streamUrl())
    const eventNames = ['connected', 'payment.checked', 'alert.created', 'alert.analyzed', 'alert.decided']

    const listeners = eventNames.map((name) => {
      const listener = (event) => {
        const handler = handlersRef.current[name]
        if (!handler) return
        if (name === 'connected') {
          handler(event.data)
          return
        }
        try {
          handler(JSON.parse(event.data))
        } catch (err) {
          console.error(`Failed to parse SSE payload for ${name}`, err)
        }
      }
      source.addEventListener(name, listener)
      return { name, listener }
    })

    source.onerror = () => {
      // EventSource auto-reconnects; nothing to do here besides letting it retry.
    }

    return () => {
      listeners.forEach(({ name, listener }) => source.removeEventListener(name, listener))
      source.close()
    }
  }, [])
}
