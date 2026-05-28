import { useEffect, useState, useCallback } from 'react';
import {
  collection,
  query,
  where,
  onSnapshot,
  getDocs,
  arrayUnion,
  doc,
  setDoc,
} from 'firebase/firestore';
import { db } from '../services/firebase';
import { useAuth } from '../contexts/AuthContext';

/**
 * Optimized hook for real-time notification counts
 * Consolidates multiple listeners into efficient queries
 * Includes error handling and fallback mechanisms
 */
export const useNotificationCountsOptimized = () => {
  const { currentUser } = useAuth();
  const [counts, setCounts] = useState({
    pendingSellers: 0,
    pendingReports: 0,
    flaggedProducts: 0,
    newUsers: 0,
    pendingOrders: 0,
    newStores: 0,
    newLearningResources: 0,
    pendingSettings: 0,
    pendingCommissions: 0,
    totalPending: 0,
  });

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const isAdminRole = currentUser?.role && 
    ['super_admin', 'admin', 'moderator'].includes(currentUser.role.toLowerCase());
  const currentAdminId = currentUser?.id;

  // Consolidated listener setup
  useEffect(() => {
    if (!isAdminRole || !currentAdminId) {
      setCounts(prev => ({
        ...prev,
        pendingSellers: 0,
        pendingReports: 0,
        flaggedProducts: 0,
        newUsers: 0,
        pendingOrders: 0,
        newStores: 0,
        newLearningResources: 0,
        pendingSettings: 0,
        pendingCommissions: 0,
      }));
      setLoading(false);
      return;
    }

    const unsubscribers = [];
    let activeListeners = 0;
    const totalListeners = 9;

    const updateCounts = (field, value) => {
      setCounts(prev => ({
        ...prev,
        [field]: value,
      }));
    };

    const handleListenerError = (field, error) => {
      console.error(`Error in ${field} listener:`, error);
      setError(`Failed to load ${field}`);
      
      // Fallback to polling
      const fallbackInterval = setInterval(async () => {
        try {
          const snapshot = await getDocs(getQueryForField(field));
          const unviewedCount = snapshot.docs.filter(doc => {
            const viewedBy = doc.data().viewed_by_admins || [];
            return !viewedBy.includes(currentAdminId);
          }).length;
          updateCounts(field, unviewedCount);
        } catch (err) {
          console.error(`Fallback polling failed for ${field}:`, err);
        }
      }, 30000); // Poll every 30 seconds

      return () => clearInterval(fallbackInterval);
    };

    const getQueryForField = (field) => {
      const oneDayAgo = Date.now() - 24 * 60 * 60 * 1000;
      const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;

      const queries = {
        pendingSellers: query(
          collection(db, 'users'),
          where('role', '==', 'seller'),
          where('verification_status', '==', 'pending')
        ),
        pendingReports: query(
          collection(db, 'reports'),
          where('status', '==', 'New')
        ),
        flaggedProducts: query(
          collection(db, 'products'),
          where('approval_status', '==', 'pending')
        ),
        newUsers: query(
          collection(db, 'users'),
          where('created_at', '>=', oneDayAgo)
        ),
        pendingOrders: query(
          collection(db, 'orders'),
          where('status', '==', 'pending')
        ),
        newStores: query(
          collection(db, 'co_seller_stores'),
          where('created_at', '>=', sevenDaysAgo)
        ),
        newLearningResources: query(
          collection(db, 'learning_resources')
        ),
        pendingSettings: query(
          collection(db, 'settings')
        ),
        pendingCommissions: query(
          collection(db, 'admin_commissions'),
          where('status', '==', 'pending')
        ),
      };

      return queries[field];
    };

    // Setup listeners for each field
    const setupListener = (field, needsViewedByFilter = true) => {
      try {
        const q = getQueryForField(field);
        
        const unsubscribe = onSnapshot(
          q,
          (snapshot) => {
            activeListeners++;
            if (activeListeners === totalListeners) {
              setLoading(false);
              setError(null);
            }

            if (needsViewedByFilter) {
              const unviewedCount = snapshot.docs.filter(doc => {
                const viewedBy = doc.data().viewed_by_admins || [];
                return !viewedBy.includes(currentAdminId);
              }).length;
              updateCounts(field, unviewedCount);
            } else {
              updateCounts(field, snapshot.docs.length);
            }
          },
          (error) => {
            const fallbackUnsub = handleListenerError(field, error);
            if (fallbackUnsub) unsubscribers.push(fallbackUnsub);
          }
        );

        unsubscribers.push(unsubscribe);
      } catch (err) {
        console.error(`Failed to setup listener for ${field}:`, err);
      }
    };

    // Setup all listeners
    setupListener('pendingSellers', true);
    setupListener('pendingReports', true);
    setupListener('flaggedProducts', true);
    setupListener('newUsers', true);
    setupListener('pendingOrders', true);
    setupListener('newStores', true);
    setupListener('newLearningResources', true);
    setupListener('pendingSettings', true);
    setupListener('pendingCommissions', false);

    return () => {
      unsubscribers.forEach(unsub => unsub?.());
    };
  }, [isAdminRole, currentAdminId]);

  // Calculate total pending
  useEffect(() => {
    const total =
      counts.pendingSellers +
      counts.pendingReports +
      counts.flaggedProducts +
      counts.newUsers +
      counts.pendingOrders +
      counts.newStores +
      counts.newLearningResources +
      counts.pendingSettings +
      counts.pendingCommissions;

    setCounts(prev => ({
      ...prev,
      totalPending: total,
    }));
  }, [
    counts.pendingSellers,
    counts.pendingReports,
    counts.flaggedProducts,
    counts.newUsers,
    counts.pendingOrders,
    counts.newStores,
    counts.newLearningResources,
    counts.pendingSettings,
    counts.pendingCommissions,
  ]);

  return { counts, loading, error };
};

/**
 * Mark item as viewed by admin
 */
export const markItemAsViewed = async (collectionName, itemId, adminId) => {
  if (!itemId || !adminId) return;

  try {
    const itemRef = doc(db, collectionName, itemId);
    await setDoc(
      itemRef,
      {
        viewed_by_admins: arrayUnion(adminId),
      },
      { merge: true }
    );
  } catch (error) {
    console.error(`Error marking ${collectionName}/${itemId} as viewed:`, error);
  }
};

export default useNotificationCountsOptimized;
