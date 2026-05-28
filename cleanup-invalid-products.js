// cleanup-invalid-products.js
// Deletes all products with missing or empty IDs from Firestore
// Run with: node cleanup-invalid-products.js

import dotenv from 'dotenv';
import { initializeApp } from 'firebase/app';
import { getFirestore, collection, getDocs, deleteDoc, doc } from 'firebase/firestore';
import * as readline from 'readline';

// Load environment variables
dotenv.config();

// Firebase config from environment variables
const firebaseConfig = {
  apiKey: process.env.VITE_FIREBASE_API_KEY,
  authDomain: process.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: process.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.VITE_FIREBASE_APP_ID
};

// Validate config
if (!firebaseConfig.apiKey) {
  console.error('❌ Firebase config not found. Make sure .env file exists in project root.');
  console.error('   Required variables: VITE_FIREBASE_API_KEY, VITE_FIREBASE_AUTH_DOMAIN, etc.');
  process.exit(1);
}

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

// Create readline interface for user confirmation
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

function askQuestion(query) {
  return new Promise(resolve => rl.question(query, resolve));
}

async function cleanupInvalidProducts() {
  try {
    console.log('🔍 Scanning products collection for invalid entries...\n');
    
    const productsRef = collection(db, 'products');
    const snapshot = await getDocs(productsRef);
    
    const invalidProducts = [];
    const validProducts = [];
    
    // Analyze all products
    snapshot.docs.forEach(docSnap => {
      const docId = docSnap.id;
      const data = docSnap.data();
      
      // Check for various invalid conditions
      const isInvalid = 
        !docId ||                           // No document ID
        docId.trim() === '' ||              // Empty document ID
        !data.title ||                      // No title
        data.title.trim() === '' ||         // Empty title
        data.title === 'No ID';             // Placeholder title
      
      if (isInvalid) {
        invalidProducts.push({
          docId: docId,
          title: data.title || 'Untitled',
          seller: data.seller_name || data.seller || 'Unknown',
          price: data.price || 0,
          category: data.category || 'N/A',
          status: data.status || 'N/A'
        });
      } else {
        validProducts.push(docId);
      }
    });
    
    console.log(`📊 Scan Results:`);
    console.log(`   Total products: ${snapshot.docs.length}`);
    console.log(`   Valid products: ${validProducts.length}`);
    console.log(`   Invalid products: ${invalidProducts.length}\n`);
    
    if (invalidProducts.length === 0) {
      console.log('✅ No invalid products found! Your database is clean.');
      rl.close();
      process.exit(0);
      return;
    }
    
    // Display invalid products
    console.log('⚠️  Invalid products found:\n');
    invalidProducts.forEach((p, idx) => {
      console.log(`  ${idx + 1}. "${p.title}"`);
      console.log(`     - ID: ${p.docId}`);
      console.log(`     - Seller: ${p.seller}`);
      console.log(`     - Price: PKR ${p.price}`);
      console.log(`     - Category: ${p.category}`);
      console.log(`     - Status: ${p.status}\n`);
    });
    
    // Ask for confirmation
    const answer = await askQuestion(
      `❓ Do you want to delete these ${invalidProducts.length} invalid product(s)? (yes/no): `
    );
    
    if (answer.toLowerCase() !== 'yes' && answer.toLowerCase() !== 'y') {
      console.log('\n❌ Cleanup cancelled by user.');
      rl.close();
      process.exit(0);
      return;
    }
    
    console.log('\n🗑️  Deleting invalid products...\n');
    
    let deleted = 0;
    let errors = 0;
    
    // Delete all invalid products
    for (const product of invalidProducts) {
      try {
        await deleteDoc(doc(db, 'products', product.docId));
        console.log(`  ✓ Deleted: "${product.title}" (${product.docId})`);
        deleted++;
      } catch (err) {
        console.error(`  ✗ Failed to delete "${product.title}":`, err.message);
        errors++;
      }
    }
    
    console.log(`\n✅ Cleanup complete!`);
    console.log(`   Successfully deleted: ${deleted}`);
    console.log(`   Errors: ${errors}`);
    console.log(`   Remaining valid products: ${validProducts.length}\n`);
    
    rl.close();
    process.exit(0);
    
  } catch (error) {
    console.error('\n❌ Error during cleanup:', error);
    console.error('   Message:', error.message);
    rl.close();
    process.exit(1);
  }
}

// Run the cleanup
console.log('═══════════════════════════════════════════════════');
console.log('  Craftoria - Invalid Products Cleanup Script');
console.log('═══════════════════════════════════════════════════\n');

cleanupInvalidProducts();
