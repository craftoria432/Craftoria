// src/services/verificationPhotoService.js
import { collection, query, where, getDocs, deleteDoc, doc } from 'firebase/firestore';
import { db } from './firebase';

/**
 * Delete verification photo from Cloudinary and Firestore after admin review
 * @param {string} userId - The user ID
 * @param {string} reviewStatus - 'approved' or 'rejected'
 */
export const deleteVerificationPhotoAfterReview = async (userId, reviewStatus) => {
  try {
    console.log(`🗑️ Deleting verification photo for user ${userId} (${reviewStatus})`);

    // Get the seller_verifications document
    const verificationQuery = query(
      collection(db, 'seller_verifications'),
      where('userId', '==', userId)
    );
    const verificationDocs = await getDocs(verificationQuery);

    if (verificationDocs.empty) {
      console.warn('⚠️ No verification document found for user:', userId);
      return { success: false, error: 'No verification document found' };
    }

    const verificationDoc = verificationDocs.docs[0];
    const verificationData = verificationDoc.data();
    const cloudinaryUrl = verificationData.imageUrl;

    // Delete from Cloudinary (optional - Cloudinary has auto-expiry)
    if (cloudinaryUrl) {
      try {
        // Extract public_id from Cloudinary URL
        // URL format: https://res.cloudinary.com/{cloud_name}/image/upload/v{version}/{public_id}.{format}
        const urlParts = cloudinaryUrl.split('/');
        const fileNameWithExt = urlParts[urlParts.length - 1];
        const publicId = `seller_verifications/${fileNameWithExt.split('.')[0]}`;

        console.log('📸 Cloudinary URL:', cloudinaryUrl);
        console.log('🔑 Public ID:', publicId);

        // Note: Cloudinary deletion requires backend API call with API secret
        // For security, this should be done via Cloud Function
        // For now, we'll just delete the Firestore document
        console.log('ℹ️ Cloudinary deletion should be handled by Cloud Function');
      } catch (cloudinaryError) {
        console.error('⚠️ Cloudinary URL parsing error:', cloudinaryError);
      }
    }

    // Delete the Firestore document
    await deleteDoc(doc(db, 'seller_verifications', verificationDoc.id));
    console.log('✅ Verification document deleted from Firestore');

    return { success: true };
  } catch (error) {
    console.error('❌ Error deleting verification photo:', error);
    return { success: false, error: error.message };
  }
};

/**
 * Get verification photo URL for a user
 * @param {string} userId - The user ID
 * @returns {Promise<string|null>} - Cloudinary URL or null
 */
export const getVerificationPhotoUrl = async (userId) => {
  try {
    const verificationQuery = query(
      collection(db, 'seller_verifications'),
      where('userId', '==', userId)
    );
    const verificationDocs = await getDocs(verificationQuery);

    if (verificationDocs.empty) {
      return null;
    }

    const verificationData = verificationDocs.docs[0].data();
    return verificationData.imageUrl || null;
  } catch (error) {
    console.error('Error fetching verification photo URL:', error);
    return null;
  }
};
