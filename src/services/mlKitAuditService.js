// src/services/mlKitAuditService.js
// ✅ PRODUCTION-READY: ML Kit decision audit logging
import { collection, addDoc, serverTimestamp, query, where, getDocs } from 'firebase/firestore';
import { db } from './firebase';

/**
 * Log ML Kit-based admin decision to audit trail
 */
export const logMLKitDecision = async (userId, decision, mlKitData, adminEmail) => {
  try {
    await addDoc(collection(db, 'ml_kit_audit_logs'), {
      userId,
      decision, // 'approved', 'rejected', 'flagged'
      mlKitConfidence: mlKitData?.confidence || 0,
      mlKitFaceCount: mlKitData?.faceCount || 0,
      mlKitIsValid: mlKitData?.isValid || false,
      mlKitMessage: mlKitData?.message || '',
      adminEmail,
      timestamp: serverTimestamp(),
      notes: `Admin decision: ${decision} based on ML Kit confidence ${(mlKitData?.confidence || 0).toFixed(1)}%`,
    });
    console.log('✅ ML Kit decision logged:', { userId, decision, confidence: mlKitData?.confidence });
  } catch (error) {
    console.error('❌ Failed to log ML Kit decision:', error);
  }
};

/**
 * Get ML Kit audit history for a specific user
 */
export const getMLKitAuditHistory = async (userId) => {
  try {
    const q = query(collection(db, 'ml_kit_audit_logs'), where('userId', '==', userId));
    const snapshot = await getDocs(q);
    return snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
      timestamp: doc.data().timestamp?.toDate?.() || new Date(),
    }));
  } catch (error) {
    console.error('❌ Failed to fetch ML Kit audit history:', error);
    return [];
  }
};

/**
 * Get ML Kit statistics for dashboard
 */
export const getMLKitStatistics = async () => {
  try {
    const snapshot = await getDocs(collection(db, 'ml_kit_audit_logs'));
    const logs = snapshot.docs.map(doc => doc.data());

    const stats = {
      totalDecisions: logs.length,
      approved: logs.filter(l => l.decision === 'approved').length,
      rejected: logs.filter(l => l.decision === 'rejected').length,
      flagged: logs.filter(l => l.decision === 'flagged').length,
      avgConfidence: (logs.reduce((sum, l) => sum + (l.mlKitConfidence || 0), 0) / logs.length).toFixed(1),
      validFaceRate: (
        (logs.filter(l => l.mlKitIsValid).length / logs.length) *
        100
      ).toFixed(1),
    };

    return stats;
  } catch (error) {
    console.error('❌ Failed to fetch ML Kit statistics:', error);
    return null;
  }
};

/**
 * Flag verification for manual review based on ML Kit confidence
 */
export const flagVerificationForReview = async (userId, mlKitData, reason, adminEmail) => {
  try {
    await addDoc(collection(db, 'ml_kit_audit_logs'), {
      userId,
      decision: 'flagged',
      mlKitConfidence: mlKitData?.confidence || 0,
      mlKitFaceCount: mlKitData?.faceCount || 0,
      mlKitIsValid: mlKitData?.isValid || false,
      mlKitMessage: mlKitData?.message || '',
      adminEmail,
      flagReason: reason,
      timestamp: serverTimestamp(),
      notes: `Flagged for review: ${reason}. ML Kit confidence: ${(mlKitData?.confidence || 0).toFixed(1)}%`,
    });
    console.log('✅ Verification flagged for review:', { userId, reason });
  } catch (error) {
    console.error('❌ Failed to flag verification:', error);
  }
};
