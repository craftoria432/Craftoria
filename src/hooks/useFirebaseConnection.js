import { useEffect, useState } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import { auth } from '../services/firebase';

/**
 * Hook to monitor Firebase connection status
 * Tracks online/offline state and auth state changes
 * @returns {Object} Connection state object
 */
export const useFirebaseConnection = () => {
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [connectionQuality, setConnectionQuality] = useState('good'); // good, slow, offline

  useEffect(() => {
    // Monitor auth state
    const unsubscribeAuth = onAuthStateChanged(auth, (user) => {
      setIsAuthenticated(!!user);
    });

    // Monitor online/offline
    const handleOnline = () => {
      setIsOnline(true);
      setConnectionQuality('good');
    };

    const handleOffline = () => {
      setIsOnline(false);
      setConnectionQuality('offline');
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Monitor connection quality via latency
    const checkConnectionQuality = async () => {
      if (!navigator.onLine) {
        setConnectionQuality('offline');
        return;
      }

      const start = performance.now();
      try {
        await fetch('/ping', { method: 'HEAD', cache: 'no-store' });
        const latency = performance.now() - start;
        setConnectionQuality(latency > 1000 ? 'slow' : 'good');
      } catch {
        setConnectionQuality('slow');
      }
    };

    // Check quality every 30 seconds
    const qualityInterval = setInterval(checkConnectionQuality, 30000);

    return () => {
      unsubscribeAuth();
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      clearInterval(qualityInterval);
    };
  }, []);

  return {
    isOnline,
    isAuthenticated,
    connectionQuality,
    isConnected: isOnline && isAuthenticated,
  };
};

export default useFirebaseConnection;
