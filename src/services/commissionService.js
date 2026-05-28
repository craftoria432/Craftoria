import {
  collection,
  query,
  where,
  getDocs,
  onSnapshot,
  doc,
  getDoc,
  updateDoc,
  Timestamp,
  orderBy,
  limit,
  startAfter,
} from 'firebase/firestore';
import { db } from '../firebase';

const COMMISSIONS_COLLECTION = 'admin_commissions';
const ADMIN_EARNINGS_COLLECTION = 'admin_earnings';

/**
 * Get total admin earnings
 * ✅ FIX: Removed dead docRef variable
 */
export const getAdminEarnings = async () => {
  try {
    const docSnap = await getDocs(
      query(collection(db, ADMIN_EARNINGS_COLLECTION))
    );

    if (docSnap.empty) {
      return {
        totalEarnings: 0,
        lastUpdated: null,
      };
    }

    const doc = docSnap.docs[0];
    return {
      totalEarnings: doc.data().total_earnings || 0,
      lastUpdated: doc.data().last_updated,
    };
  } catch (error) {
    console.error('Error fetching admin earnings:', error);
    throw error;
  }
};

/**
 * Get commission statistics for a date range
 * ✅ FIX: Wrapped date range in Firestore Timestamp for consistency
 * ✅ FIX: Changed field name from "amount" to "commission_amount"
 * ✅ FIX: Now fetches BOTH pending and paid commissions for accurate stats
 */
export const getCommissionStats = async (startDate, endDate) => {
  try {
    // Wrap dates in Firestore Timestamp for consistency with mobile
    const startTs = Timestamp.fromDate(startDate);
    const endTs = Timestamp.fromDate(endDate);

    // Fetch PAID commissions
    const paidSnapshot = await getDocs(
      query(
        collection(db, COMMISSIONS_COLLECTION),
        where('created_at', '>=', startTs),
        where('created_at', '<=', endTs),
        where('status', '==', 'paid')
      )
    );

    // Fetch PENDING commissions
    const pendingSnapshot = await getDocs(
      query(
        collection(db, COMMISSIONS_COLLECTION),
        where('created_at', '>=', startTs),
        where('created_at', '<=', endTs),
        where('status', '==', 'pending')
      )
    );

    // Calculate amounts
    const paidAmount = paidSnapshot.docs.reduce(
      (sum, doc) => sum + (doc.data().commission_amount || 0),
      0
    );

    const pendingAmount = pendingSnapshot.docs.reduce(
      (sum, doc) => sum + (doc.data().commission_amount || 0),
      0
    );

    const stats = {
      totalCommissions: paidAmount + pendingAmount,
      pendingAmount,
      paidAmount,
      count: paidSnapshot.docs.length + pendingSnapshot.docs.length,
    };

    return stats;
  } catch (error) {
    console.error('Error fetching commission stats:', error);
    throw error;
  }
};

/**
 * Subscribe to pending commissions
 * ✅ FIX: Always deliver data; only log fromCache at debug level
 */
export const subscribeToPendingCommissions = (callback, onError) => {
  try {
    const unsubscribe = onSnapshot(
      query(
        collection(db, COMMISSIONS_COLLECTION),
        where('status', '==', 'pending'),
        orderBy('created_at', 'desc')
      ),
      (snapshot) => {
        // ✅ Always deliver data to UI
        const commissions = snapshot.docs.map((doc) => ({
          id: doc.id,
          ...doc.data(),
          created_at: doc.data().created_at?.toDate?.() || new Date(),
        }));

        callback(commissions);

        // ✅ Only log cache status at debug level
        if (snapshot.metadata.fromCache) {
          console.debug('Serving pending commissions from local cache');
        }
      },
      (error) => {
        console.error('Error subscribing to pending commissions:', error);
        if (onError) onError(error);
      }
    );

    return unsubscribe;
  } catch (error) {
    console.error('Error setting up pending commissions subscription:', error);
    if (onError) onError(error);
  }
};

/**
 * Subscribe to admin earnings
 * ✅ FIX: Always deliver data; only log fromCache at debug level
 */
export const subscribeToAdminEarnings = (callback, onError) => {
  try {
    const unsubscribe = onSnapshot(
      collection(db, ADMIN_EARNINGS_COLLECTION),
      (snapshot) => {
        // ✅ Always deliver data to UI
        const earnings = snapshot.docs.map((doc) => ({
          id: doc.id,
          ...doc.data(),
          last_updated: doc.data().last_updated?.toDate?.() || new Date(),
        }));

        callback(earnings);

        // ✅ Only log cache status at debug level
        if (snapshot.metadata.fromCache) {
          console.debug('Serving admin earnings from local cache');
        }
      },
      (error) => {
        console.error('Error subscribing to admin earnings:', error);
        if (onError) onError(error);
      }
    );

    return unsubscribe;
  } catch (error) {
    console.error('Error setting up admin earnings subscription:', error);
    if (onError) onError(error);
  }
};

/**
 * Mark a commission as paid
 */
export const markCommissionAsPaid = async (commissionId) => {
  try {
    const commissionRef = doc(db, COMMISSIONS_COLLECTION, commissionId);
    await updateDoc(commissionRef, {
      status: 'paid',
      paid_at: Timestamp.now(),
      updated_at: Timestamp.now(),
    });
  } catch (error) {
    console.error('Error marking commission as paid:', error);
    throw error;
  }
};

/**
 * Get all commissions with pagination
 */
export const getAllCommissions = async (pageSize = 20, lastDocSnapshot = null) => {
  try {
    let q = query(
      collection(db, COMMISSIONS_COLLECTION),
      orderBy('created_at', 'desc'),
      limit(pageSize + 1)
    );

    if (lastDocSnapshot) {
      q = query(
        collection(db, COMMISSIONS_COLLECTION),
        orderBy('created_at', 'desc'),
        startAfter(lastDocSnapshot),
        limit(pageSize + 1)
      );
    }

    const snapshot = await getDocs(q);
    const docs = snapshot.docs.slice(0, pageSize);
    const hasMore = snapshot.docs.length > pageSize;

    const commissions = docs.map((doc) => ({
      id: doc.id,
      ...doc.data(),
      created_at: doc.data().created_at?.toDate?.() || new Date(),
    }));

    return {
      commissions,
      lastDoc: docs.length > 0 ? docs[docs.length - 1] : null,
      hasMore,
    };
  } catch (error) {
    console.error('Error fetching all commissions:', error);
    throw error;
  }
};
