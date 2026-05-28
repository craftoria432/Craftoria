// Migration script to fix users who became sellers without admin approval
// Run this in Firebase console or as a Cloud Function

const { getFirestore, collection, query, where, getDocs, updateDoc, doc } = require('firebase/firestore');

const fixSellerRoleIssue = async () => {
  const db = getFirestore();
  
  try {
    // Find all users who are sellers but not verified and have no verification photo
    // These are likely users who clicked "Become a Seller" but never completed verification
    const problematicSellersQuery = query(
      collection(db, 'users'),
      where('role', '==', 'seller'),
      where('verified', '==', false),
      where('verification_status', '==', 'not_submitted')
    );
    
    const snapshot = await getDocs(problematicSellersQuery);
    console.log(`Found ${snapshot.docs.length} users to fix`);
    
    const fixes = [];
    
    for (const userDoc of snapshot.docs) {
      const userData = userDoc.data();
      
      // Check if they have verification photo - if not, they likely never intended to be sellers
      if (!userData.verification_photo_url || userData.verification_photo_url === '') {
        fixes.push({
          userId: userDoc.id,
          name: userData.name,
          email: userData.email,
          currentRole: userData.role
        });
        
        // Reset them back to buyer with pending seller application
        await updateDoc(doc(db, 'users', userDoc.id), {
          role: 'buyer',                        // Reset to buyer
          seller_application_status: 'pending', // Set as pending application
          verification_status: 'not_submitted',
          verified: false,
          migration_fixed: true,                // Mark as fixed by migration
          migration_date: new Date()
        });
        
        console.log(`✅ Fixed user: ${userData.name} (${userData.email})`);
      }
    }
    
    console.log(`\n🎉 Migration completed! Fixed ${fixes.length} users:`);
    fixes.forEach(user => {
      console.log(`- ${user.name} (${user.email})`);
    });
    
    return fixes;
    
  } catch (error) {
    console.error('❌ Migration failed:', error);
    throw error;
  }
};

// Run the migration
fixSellerRoleIssue()
  .then(fixes => {
    console.log('Migration successful!');
  })
  .catch(error => {
    console.error('Migration failed:', error);
  });

module.exports = { fixSellerRoleIssue };