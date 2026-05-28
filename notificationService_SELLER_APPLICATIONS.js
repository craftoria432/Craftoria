// Add these functions to your existing notificationService.js

/**
 * Notify user when their seller application is approved
 */
export const notifyApplicationApproved = async (userId, welcomeMessage = '') => {
  try {
    const notification = {
      userId,
      title: 'Seller Application Approved! 🎉',
      description: welcomeMessage || 'Congratulations! Your seller application has been approved. You can now complete your identity verification to start selling.',
      category: 'SYSTEM',
      actionType: 'VIEW_PROFILE',
      actionData: { application_status: 'approved' },
      createdAt: new Date(),
      read: false
    };

    await addDoc(collection(db, 'notifications'), notification);
    console.log('✅ Seller application approval notification sent:', userId);
  } catch (error) {
    console.error('❌ Failed to send application approval notification:', error);
    throw error;
  }
};

/**
 * Notify user when their seller application is rejected
 */
export const notifyApplicationRejected = async (userId, reason = '') => {
  try {
    const notification = {
      userId,
      title: 'Seller Application Update',
      description: reason 
        ? `Your seller application has been rejected. Reason: ${reason}. You can apply again after addressing the issues.`
        : 'Your seller application has been rejected. You can apply again after reviewing our seller requirements.',
      category: 'SYSTEM',
      actionType: 'VIEW_PROFILE',
      actionData: { application_status: 'rejected' },
      createdAt: new Date(),
      read: false
    };

    await addDoc(collection(db, 'notifications'), notification);
    console.log('✅ Seller application rejection notification sent:', userId);
  } catch (error) {
    console.error('❌ Failed to send application rejection notification:', error);
    throw error;
  }
};

/**
 * Notify admin when new seller application is submitted
 */
export const notifyAdminNewSellerApplication = async (userId, userName, userEmail) => {
  try {
    // Get all admin users
    const adminsQuery = query(
      collection(db, 'users'),
      where('role', '==', 'admin')
    );
    
    const adminSnapshot = await getDocs(adminsQuery);
    
    // Send notification to each admin
    const notifications = adminSnapshot.docs.map(adminDoc => ({
      userId: adminDoc.id,
      title: 'New Seller Application',
      description: `${userName} (${userEmail}) has applied to become a seller. Review their application in the admin panel.`,
      category: 'ADMIN',
      actionType: 'VIEW_SELLER_APPLICATIONS',
      actionData: { 
        applicant_id: userId,
        applicant_name: userName,
        applicant_email: userEmail
      },
      createdAt: new Date(),
      read: false
    }));

    // Batch create notifications
    const batch = writeBatch(db);
    notifications.forEach(notification => {
      const notificationRef = doc(collection(db, 'notifications'));
      batch.set(notificationRef, notification);
    });
    
    await batch.commit();
    console.log('✅ Admin notifications sent for new seller application:', userId);
  } catch (error) {
    console.error('❌ Failed to send admin notifications:', error);
    throw error;
  }
};