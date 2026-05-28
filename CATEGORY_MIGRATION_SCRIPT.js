/**
 * Category Migration Script
 * 
 * This script updates existing products in Firestore to use the new standardized category names.
 * 
 * IMPORTANT: 
 * - Run this ONCE after deploying the category standardization changes
 * - Test on a small batch first before running on all products
 * - Backup your database before running
 * 
 * How to run:
 * 1. Deploy this as a Cloud Function
 * 2. Call it via HTTP trigger or run directly in Firebase Console
 * 3. Monitor the logs to see progress
 */

const admin = require('firebase-admin');

// Initialize Firebase Admin (if not already initialized)
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

/**
 * Mapping of old category names to new standardized names
 */
const CATEGORY_MAPPING = {
  // Old name → New standardized name
  'Textiles': 'Textiles & Embroidery',
  'Embroidery': 'Textiles & Embroidery',
  'Jewelry': 'Jewelry & Accessories',
  'Pottery': 'Pottery & Ceramics',
  'Art & Paintings': 'Paintings & Art',
  'Other Handicrafts': 'Handicrafts',
  // These don't need changes but included for completeness
  'Home Décor': 'Home Décor',
  'Woodwork': 'Woodwork',
  'Pottery & Ceramics': 'Pottery & Ceramics',
  'Textiles & Embroidery': 'Textiles & Embroidery',
  'Jewelry & Accessories': 'Jewelry & Accessories',
  'Paintings & Art': 'Paintings & Art',
  'Handicrafts': 'Handicrafts',
};

/**
 * Valid standardized categories
 */
const VALID_CATEGORIES = [
  'Pottery & Ceramics',
  'Textiles & Embroidery',
  'Jewelry & Accessories',
  'Woodwork',
  'Paintings & Art',
  'Handicrafts',
  'Home Décor',
];

/**
 * Main migration function
 */
async function migrateCategoriesInProducts() {
  console.log('🚀 Starting category migration...');
  console.log('📋 Category mapping:', CATEGORY_MAPPING);
  
  try {
    const productsRef = db.collection('products');
    const snapshot = await productsRef.get();
    
    console.log(`📊 Found ${snapshot.size} total products`);
    
    if (snapshot.empty) {
      console.log('⚠️  No products found in database');
      return {
        success: true,
        totalProducts: 0,
        updatedProducts: 0,
        skippedProducts: 0,
        errors: 0,
      };
    }
    
    let updatedCount = 0;
    let skippedCount = 0;
    let errorCount = 0;
    const errors = [];
    
    // Process in batches of 500 (Firestore batch limit)
    const batchSize = 500;
    let batch = db.batch();
    let batchCount = 0;
    
    for (const doc of snapshot.docs) {
      try {
        const data = doc.data();
        const oldCategory = data.category;
        
        // Skip if no category
        if (!oldCategory) {
          console.log(`⚠️  Product ${doc.id} has no category, skipping`);
          skippedCount++;
          continue;
        }
        
        // Get new category name
        const newCategory = CATEGORY_MAPPING[oldCategory];
        
        // Skip if category doesn't need updating
        if (!newCategory || oldCategory === newCategory) {
          skippedCount++;
          continue;
        }
        
        // Validate new category
        if (!VALID_CATEGORIES.includes(newCategory)) {
          console.error(`❌ Invalid new category "${newCategory}" for product ${doc.id}`);
          errorCount++;
          errors.push({
            productId: doc.id,
            oldCategory,
            newCategory,
            error: 'Invalid new category',
          });
          continue;
        }
        
        // Update the product
        batch.update(doc.ref, {
          category: newCategory,
          categoryUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
          categoryMigrationVersion: '1.0',
        });
        
        console.log(`✅ Updating ${doc.id}: "${oldCategory}" → "${newCategory}"`);
        updatedCount++;
        batchCount++;
        
        // Commit batch if it reaches the limit
        if (batchCount >= batchSize) {
          await batch.commit();
          console.log(`💾 Committed batch of ${batchCount} updates`);
          batch = db.batch();
          batchCount = 0;
        }
      } catch (error) {
        console.error(`❌ Error processing product ${doc.id}:`, error);
        errorCount++;
        errors.push({
          productId: doc.id,
          error: error.message,
        });
      }
    }
    
    // Commit remaining batch
    if (batchCount > 0) {
      await batch.commit();
      console.log(`💾 Committed final batch of ${batchCount} updates`);
    }
    
    // Summary
    console.log('\n' + '='.repeat(60));
    console.log('📊 MIGRATION SUMMARY');
    console.log('='.repeat(60));
    console.log(`Total products:   ${snapshot.size}`);
    console.log(`✅ Updated:       ${updatedCount}`);
    console.log(`⏭️  Skipped:       ${skippedCount}`);
    console.log(`❌ Errors:        ${errorCount}`);
    console.log('='.repeat(60));
    
    if (errors.length > 0) {
      console.log('\n❌ ERRORS:');
      errors.forEach((err, index) => {
        console.log(`${index + 1}. Product ${err.productId}:`, err.error || err);
      });
    }
    
    return {
      success: errorCount === 0,
      totalProducts: snapshot.size,
      updatedProducts: updatedCount,
      skippedProducts: skippedCount,
      errors: errorCount,
      errorDetails: errors,
    };
  } catch (error) {
    console.error('❌ Migration failed:', error);
    throw error;
  }
}

/**
 * Dry run - shows what would be updated without actually updating
 */
async function dryRunMigration() {
  console.log('🔍 Running DRY RUN (no changes will be made)...');
  
  try {
    const productsRef = db.collection('products');
    const snapshot = await productsRef.get();
    
    console.log(`📊 Found ${snapshot.size} total products`);
    
    const changes = [];
    let wouldUpdateCount = 0;
    let wouldSkipCount = 0;
    
    snapshot.forEach(doc => {
      const data = doc.data();
      const oldCategory = data.category;
      
      if (!oldCategory) {
        wouldSkipCount++;
        return;
      }
      
      const newCategory = CATEGORY_MAPPING[oldCategory];
      
      if (!newCategory || oldCategory === newCategory) {
        wouldSkipCount++;
        return;
      }
      
      changes.push({
        productId: doc.id,
        productTitle: data.title || 'Untitled',
        oldCategory,
        newCategory,
      });
      
      wouldUpdateCount++;
    });
    
    console.log('\n' + '='.repeat(60));
    console.log('📊 DRY RUN SUMMARY');
    console.log('='.repeat(60));
    console.log(`Total products:        ${snapshot.size}`);
    console.log(`Would update:          ${wouldUpdateCount}`);
    console.log(`Would skip:            ${wouldSkipCount}`);
    console.log('='.repeat(60));
    
    if (changes.length > 0) {
      console.log('\n📝 CHANGES THAT WOULD BE MADE:');
      changes.forEach((change, index) => {
        console.log(`${index + 1}. ${change.productTitle} (${change.productId})`);
        console.log(`   "${change.oldCategory}" → "${change.newCategory}"`);
      });
    }
    
    return {
      totalProducts: snapshot.size,
      wouldUpdate: wouldUpdateCount,
      wouldSkip: wouldSkipCount,
      changes,
    };
  } catch (error) {
    console.error('❌ Dry run failed:', error);
    throw error;
  }
}

/**
 * Verify migration - checks if all products have valid categories
 */
async function verifyMigration() {
  console.log('🔍 Verifying migration...');
  
  try {
    const productsRef = db.collection('products');
    const snapshot = await productsRef.get();
    
    console.log(`📊 Checking ${snapshot.size} products`);
    
    const issues = [];
    let validCount = 0;
    let invalidCount = 0;
    
    snapshot.forEach(doc => {
      const data = doc.data();
      const category = data.category;
      
      if (!category) {
        issues.push({
          productId: doc.id,
          productTitle: data.title || 'Untitled',
          issue: 'No category',
        });
        invalidCount++;
        return;
      }
      
      if (!VALID_CATEGORIES.includes(category)) {
        issues.push({
          productId: doc.id,
          productTitle: data.title || 'Untitled',
          category,
          issue: 'Invalid category',
        });
        invalidCount++;
        return;
      }
      
      validCount++;
    });
    
    console.log('\n' + '='.repeat(60));
    console.log('📊 VERIFICATION SUMMARY');
    console.log('='.repeat(60));
    console.log(`Total products:   ${snapshot.size}`);
    console.log(`✅ Valid:         ${validCount}`);
    console.log(`❌ Invalid:       ${invalidCount}`);
    console.log('='.repeat(60));
    
    if (issues.length > 0) {
      console.log('\n❌ ISSUES FOUND:');
      issues.forEach((issue, index) => {
        console.log(`${index + 1}. ${issue.productTitle} (${issue.productId})`);
        console.log(`   Issue: ${issue.issue}`);
        if (issue.category) {
          console.log(`   Current category: "${issue.category}"`);
        }
      });
    } else {
      console.log('\n✅ All products have valid categories!');
    }
    
    return {
      success: invalidCount === 0,
      totalProducts: snapshot.size,
      validProducts: validCount,
      invalidProducts: invalidCount,
      issues,
    };
  } catch (error) {
    console.error('❌ Verification failed:', error);
    throw error;
  }
}

// Export functions for use in Cloud Functions
module.exports = {
  migrateCategoriesInProducts,
  dryRunMigration,
  verifyMigration,
};

// If running directly (not as a Cloud Function)
if (require.main === module) {
  (async () => {
    try {
      // Step 1: Dry run to see what would change
      console.log('\n📋 STEP 1: DRY RUN\n');
      await dryRunMigration();
      
      // Uncomment to run actual migration
      // console.log('\n🚀 STEP 2: ACTUAL MIGRATION\n');
      // await migrateCategoriesInProducts();
      
      // Uncomment to verify after migration
      // console.log('\n✅ STEP 3: VERIFICATION\n');
      // await verifyMigration();
      
      process.exit(0);
    } catch (error) {
      console.error('❌ Script failed:', error);
      process.exit(1);
    }
  })();
}
