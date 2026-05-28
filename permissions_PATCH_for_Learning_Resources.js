// PATCH FILE: Add these to your existing src/config/permissions.js

// ============================================================================
// STEP 1: Add these new permissions to your PERMISSIONS object
// ============================================================================

export const PERMISSIONS = {
  // ... your existing permissions ...
  
  // Learning Resources (ADD THESE)
  VIEW_LEARNING_RESOURCES: 'view_learning_resources',
  CREATE_LEARNING_RESOURCES: 'create_learning_resources',
  EDIT_LEARNING_RESOURCES: 'edit_learning_resources',
  DELETE_LEARNING_RESOURCES: 'delete_learning_resources',
};

// ============================================================================
// STEP 2: Update ROLE_PERMISSIONS - Add Learning Resources permissions
// ============================================================================

export const ROLE_PERMISSIONS = {
  [ROLES.SUPER_ADMIN]: [
    // ... your existing permissions ...
    
    // Learning Resources (ADD THESE)
    PERMISSIONS.VIEW_LEARNING_RESOURCES,
    PERMISSIONS.CREATE_LEARNING_RESOURCES,
    PERMISSIONS.EDIT_LEARNING_RESOURCES,
    PERMISSIONS.DELETE_LEARNING_RESOURCES,
  ],

  [ROLES.ADMIN]: [
    // ... your existing permissions ...
    
    // Learning Resources (ADD THIS - View Only)
    PERMISSIONS.VIEW_LEARNING_RESOURCES,
    // Note: Admin can view but NOT create/edit/delete
  ],

  [ROLES.MODERATOR]: [
    // ... your existing permissions ...
    
    // Learning Resources (ADD THIS - View Only)
    PERMISSIONS.VIEW_LEARNING_RESOURCES,
    // Note: Moderator can view but NOT create/edit/delete
  ],
};

// ============================================================================
// STEP 3: Add Learning Resources route to ROUTE_ACCESS
// ============================================================================

export const ROUTE_ACCESS = {
  // ... your existing routes ...
  
  '/learning-resources': [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.MODERATOR],
};

// ============================================================================
// COMPLETE UPDATED PERMISSIONS OBJECT (for reference)
// ============================================================================

/*
export const PERMISSIONS = {
  // Dashboard
  VIEW_DASHBOARD: 'view_dashboard',
  VIEW_ANALYTICS: 'view_analytics',

  // Seller Verification
  VIEW_SELLERS: 'view_sellers',
  APPROVE_SELLERS: 'approve_sellers',
  REJECT_SELLERS: 'reject_sellers',

  // Product Management
  VIEW_PRODUCTS: 'view_products',
  EDIT_PRODUCTS: 'edit_products',
  DELETE_PRODUCTS: 'delete_products',
  FLAG_PRODUCTS: 'flag_products',
  ADD_FEATURED_PRODUCTS: 'add_featured_products',

  // User Management
  VIEW_USERS: 'view_users',
  SUSPEND_USERS: 'suspend_users',
  DELETE_USERS: 'delete_users',
  MESSAGE_USERS: 'message_users',

  // Order Management
  VIEW_ORDERS: 'view_orders',
  UPDATE_ORDER_STATUS: 'update_order_status',

  // Store Management
  VIEW_STORES: 'view_stores',
  FLAG_STORES: 'flag_stores',
  DELETE_STORES: 'delete_stores',

  // Reports & Complaints
  VIEW_REPORTS: 'view_reports',
  INVESTIGATE_REPORTS: 'investigate_reports',
  TAKE_ACTION_REPORTS: 'take_action_reports',
  DISMISS_REPORTS: 'dismiss_reports',

  // Settings
  VIEW_SETTINGS: 'view_settings',
  EDIT_SYSTEM_SETTINGS: 'edit_system_settings',
  MANAGE_ADMINS: 'manage_admins',
  ADD_ADMINS: 'add_admins',
  REMOVE_ADMINS: 'remove_admins',

  // ✅ Learning Resources (NEW)
  VIEW_LEARNING_RESOURCES: 'view_learning_resources',
  CREATE_LEARNING_RESOURCES: 'create_learning_resources',
  EDIT_LEARNING_RESOURCES: 'edit_learning_resources',
  DELETE_LEARNING_RESOURCES: 'delete_learning_resources',
};
*/
