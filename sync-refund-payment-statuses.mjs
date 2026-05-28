#!/usr/bin/env node

/**
 * Sync Refund Payment Statuses Script
 * 
 * This script synchronizes payment statuses with their corresponding refund statuses
 * for all existing refunds in the database. This is a one-time migration script.
 * 
 * Usage:
 *   node sync-refund-payment-statuses.mjs
 * 
 * Prerequisites:
 *   - Firebase Admin SDK initialized
 *   - Service account key file (serviceAccountKey.json or app/serviceAccountKey.json)
 */

import admin from 'firebase-admin';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Configuration
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

const DRY_RUN = process.argv.includes('--dry-run');
const VERBOSE = process.argv.includes('--verbose');
const BATCH_SIZE = 500; // Firestore batch write limit

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Initialize Firebase Admin
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

function initializeFirebase() {
  if (admin.apps.length > 0) {
    console.log('✅ Firebase already initialized');
    return;
  }

  // Try multiple possible locations for service account key
  const possiblePaths = [
    join(__dirname, 'serviceAccountKey.json'),
    join(__dirname, 'app', 'serviceAccountKey.json'),
    join(__dirname, 'functions', 'serviceAccountKey.json'),
  ];

  let serviceAccount = null;
  let usedPath = null;

  for (const path of possiblePaths) {
    try {
      serviceAccount = JSON.parse(readFileSync(path, 'utf8'));
      usedPath = path;
      break;
    } catch (error) {
      // Continue to next path
    }
  }

  if (!serviceAccount) {
    console.error('❌ Error: Could not find serviceAccountKey.json');
    console.error('   Searched in:');
    possiblePaths.forEach(path => console.error(`   - ${path}`));
    console.error('\n   Please ensure serviceAccountKey.json exists in one of these locations.');
    process.exit(1);
  }

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });

  console.log(`✅ Firebase initialized using: ${usedPath}`);
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Status Mapping
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

function mapRefundStatusToPaymentStatus(refundStatus) {
  const statusMap = {
    // Pending states
    'requested': 'refund_pending',
    'under_review': 'refund_pending',
    
    // Processing states
    'approved_by_seller': 'refund_processing',
    'approved_by_admin': 'refund_processing',
    'processing': 'refund_processing',
    
    // Final states
    'completed': 'refunded',
    'rejected_by_seller': 'refund_rejected',
    'rejected_by_admin': 'refund_rejected',
    'failed': 'refund_rejected', // Treat failed as rejected for payment status
  };

  return statusMap[refundStatus.toLowerCase()] || null;
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Main Sync Function
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

async function syncRefundPaymentStatuses() {
  console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('🔄 Refund Payment Status Synchronization');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

  if (DRY_RUN) {
    console.log('🔍 DRY RUN MODE - No changes will be made to the database\n');
  }

  const db = admin.firestore();
  const stats = {
    total: 0,
    synced: 0,
    skipped: 0,
    errors: 0,
    byStatus: {}
  };

  try {
    // Fetch all refunds
    console.log('📥 Fetching refunds from database...');
    const refundsSnapshot = await db.collection('refunds').get();
    stats.total = refundsSnapshot.size;
    console.log(`✅ Found ${stats.total} refund records\n`);

    if (stats.total === 0) {
      console.log('ℹ️  No refunds found. Nothing to sync.');
      return;
    }

    // Process refunds in batches
    const refunds = refundsSnapshot.docs;
    let batch = db.batch();
    let batchCount = 0;
    let processedCount = 0;

    console.log('🔄 Processing refunds...\n');

    for (const refundDoc of refunds) {
      processedCount++;
      const refund = refundDoc.data();
      const refundId = refundDoc.id;
      const paymentId = refund.payment_id;
      const refundStatus = refund.status;

      if (VERBOSE) {
        console.log(`\n[${processedCount}/${stats.total}] Processing refund: ${refundId}`);
        console.log(`   Payment ID: ${paymentId}`);
        console.log(`   Refund Status: ${refundStatus}`);
      }

      // Validate payment ID
      if (!paymentId) {
        console.log(`⚠️  [${processedCount}/${stats.total}] Skipping refund ${refundId}: No payment_id`);
        stats.skipped++;
        continue;
      }

      // Map refund status to payment status
      const paymentStatus = mapRefundStatusToPaymentStatus(refundStatus);
      
      if (!paymentStatus) {
        console.log(`⚠️  [${processedCount}/${stats.total}] Skipping refund ${refundId}: Unknown status "${refundStatus}"`);
        stats.skipped++;
        continue;
      }

      // Check if payment exists
      const paymentRef = db.collection('seller_payments').doc(paymentId);
      const paymentDoc = await paymentRef.get();

      if (!paymentDoc.exists) {
        console.log(`⚠️  [${processedCount}/${stats.total}] Skipping refund ${refundId}: Payment ${paymentId} not found`);
        stats.skipped++;
        continue;
      }

      const payment = paymentDoc.data();
      const currentPaymentStatus = payment.status;

      // Check if update is needed
      if (currentPaymentStatus === paymentStatus) {
        if (VERBOSE) {
          console.log(`   ℹ️  Payment already has correct status: ${paymentStatus}`);
        }
        stats.skipped++;
        continue;
      }

      // Prepare update data
      const updateData = {
        status: paymentStatus,
        updated_at: Date.now()
      };

      // Add refund details for completed refunds
      if (paymentStatus === 'refunded' && refund.refund_amount) {
        updateData.refund_amount = refund.refund_amount;
        updateData.refund_reason = refund.reason || '';
        updateData.refund_date = refund.completed_at || Date.now();
      }

      if (!DRY_RUN) {
        // Add to batch
        batch.update(paymentRef, updateData);
        batchCount++;

        // Commit batch if it reaches the limit
        if (batchCount >= BATCH_SIZE) {
          await batch.commit();
          console.log(`✅ Committed batch of ${batchCount} updates`);
          batch = db.batch();
          batchCount = 0;
        }
      }

      // Update stats
      stats.synced++;
      stats.byStatus[paymentStatus] = (stats.byStatus[paymentStatus] || 0) + 1;

      console.log(`✅ [${processedCount}/${stats.total}] ${DRY_RUN ? 'Would update' : 'Updated'} payment ${paymentId}: ${currentPaymentStatus} → ${paymentStatus}`);
    }

    // Commit remaining batch
    if (!DRY_RUN && batchCount > 0) {
      await batch.commit();
      console.log(`✅ Committed final batch of ${batchCount} updates`);
    }

  } catch (error) {
    console.error('\n❌ Error during synchronization:', error);
    stats.errors++;
  }

  // Print summary
  console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('📊 Synchronization Summary');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  console.log(`Total refunds:        ${stats.total}`);
  console.log(`Payments synced:      ${stats.synced}`);
  console.log(`Skipped:              ${stats.skipped}`);
  console.log(`Errors:               ${stats.errors}`);
  
  if (Object.keys(stats.byStatus).length > 0) {
    console.log('\nBy Payment Status:');
    Object.entries(stats.byStatus).forEach(([status, count]) => {
      console.log(`  ${status.padEnd(20)} ${count}`);
    });
  }

  console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

  if (DRY_RUN) {
    console.log('ℹ️  This was a dry run. No changes were made to the database.');
    console.log('   Run without --dry-run to apply changes.\n');
  } else {
    console.log('✅ Synchronization complete!\n');
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Entry Point
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

async function main() {
  try {
    initializeFirebase();
    await syncRefundPaymentStatuses();
    process.exit(0);
  } catch (error) {
    console.error('\n❌ Fatal error:', error);
    process.exit(1);
  }
}

// Show usage if help flag is present
if (process.argv.includes('--help') || process.argv.includes('-h')) {
  console.log(`
Sync Refund Payment Statuses Script

Usage:
  node sync-refund-payment-statuses.mjs [options]

Options:
  --dry-run    Preview changes without modifying the database
  --verbose    Show detailed processing information
  --help, -h   Show this help message

Examples:
  # Preview changes (recommended first run)
  node sync-refund-payment-statuses.mjs --dry-run

  # Apply changes
  node sync-refund-payment-statuses.mjs

  # Verbose output with dry run
  node sync-refund-payment-statuses.mjs --dry-run --verbose

Status Mapping:
  requested, under_review          → refund_pending
  approved_by_seller, approved_by_admin, processing → refund_processing
  completed                        → refunded
  rejected_by_seller, rejected_by_admin, failed → refund_rejected
`);
  process.exit(0);
}

main();
