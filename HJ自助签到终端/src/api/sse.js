/**
 * Sets up a persistent Server-Sent Events connection to the backend notification engine.
 * @param {string} clientId - The unique identifier for this patient terminal.
 * @param {Function} onMessageCallback - The callback invoked when a routed event fires.
 * @returns {EventSource} The configured connection object allowing manual close().
 */
export function setupSseListener(clientId, onMessageCallback) {
  const url = `/api/notifications/subscribe?clientId=${clientId}`;
  const eventSource = new EventSource(url);

  eventSource.onmessage = (event) => {
    // Default message handler
    try {
      const parsedData = JSON.parse(event.data);
      onMessageCallback({ type: 'MESSAGE', data: parsedData });
    } catch (e) {
      console.warn('Received non-JSON SSE data:', event.data);
    }
  };

  // Specific Custom Events mappings injected from the Java NotificationEngine
  eventSource.addEventListener('QUEUE_UPDATE', (event) => {
    onMessageCallback({ type: 'QUEUE_UPDATE', data: JSON.parse(event.data) });
  });

  eventSource.addEventListener('CALL_NEXT', (event) => {
    onMessageCallback({ type: 'CALL_NEXT', data: JSON.parse(event.data) });
  });

  eventSource.onerror = (error) => {
    console.error('SSE Connection lost / Error occurred:', error);
    // Note: EventSource automatically attempts to reconnect on its own
  };

  return eventSource;
}
