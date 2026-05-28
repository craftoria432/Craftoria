// src/config/permissions.js - COMPLETE PRODUCTION VERSION

export const ROLES = {
  SUPER_ADMIN: 'super_admin',
  ADMIN: 'admin',
  MODERATOR: 'moderator',
  SELLER: 'seller',
  BUYER: 'buyer',
};

export const PERMISSIONS = {
  // Dashboard & Analytics
  VIEW_DASHBOARD: 'view_dashboard',
  VIEW_ANALYTICS: 'view_analytics',
  EXPORT_REPORTS: 'export_reports',

  // User Management
  VIEW_USERS: 'view_users',
  EDIT_USERS: 'edit_users',
  DELETE_USERS: 'delete_users',
  BAN_USERS: 'ban_users',
  VERIFY_SELLERS: 'verify_sellers',

  // Product Management
  VIEW_PRODUCTS: 'view_products',
  EDIT_PRODUCTS: 'edit_products',
  DELETE_PRODUCTS: 'delete_products',
  APPROVE_PRODUCTS: 'approve_products',
  FEATURE_PRODUCTS: 'feature_products',

  // Order Management
  VIEW_ORDERS: 'view_orders',
  EDIT_ORDERS: 'edit_orders',
  CANCEL_ORDERS: 'cancel_orders',
  REFUND_ORDERS: 'refund_orders',

  // Co-Seller Stores
  VIEW_COSELLER_STORES: 'view_coseller_stores',
  APPROVE_COSELLER_STORES: 'approve_coseller_stores',
  EDIT_COSELLER_STORES: 'edit_coseller_stores',
  DELETE_COSELLER_STORES: 'delete_coseller_stores',

  // Reports & Moderation
  VIEW_REPORTS: 'view_reports',
  RESOLVE_REPORTS: 'resolve_reports',
  DELETE_REPORTED_CONTENT: 'delete_reported_content',

  // Learning Resources
  VIEW_LEARNING_RESOURCES: 'view_learning_resources',
  CREATE_LEARNING_RESOURCES: 'create_learning_resources',
  EDIT_LEARNING_RESOURCES: 'edit_learning_resources',
  DELETE_LEARNING_RESOURCES: 'delete_learning_resources',

  // System Settings
  VIEW_SETTINGS: 'view_settings',
  EDIT_SYSTEM_SETTINGS: 'edit_system_settings',
  MANAGE_ADMINS: 'manage_admins',

  // Notifications
  SEND_NOTIFICATIONS: 'send_notifications',
  SEND_BULK_NOTIFICATIONS: 'send_bulk_notifications',

  // Financial
  VIEW_FINANCIAL_DATA: 'view_financial_data',
  MANAGE_PAYOUTS: 'manage_payouts',
  ADJUST_COMMISSION: 'adjust_commission',
};

export const ROLE_PERMISSIONS = {
  [ROLES.SUPER_ADMIN]: [
    // Full access to everything
    PERMISSIONS.VIEW_DASHBOARD,
    PERMISSIONS.VIEW_ANALYTICS,
    PERMISSIONS.EXPORT_REPORTS,
    
    PERMISSIONS.VIEW_USERS,
    PERMISSIONS.EDIT_USERS,
    PERMISSIONS.DELETE_USERS,
    PERMISSIONS.BAN_USERS,
    PERMISSIONS.VERIFY_SELLERS,
    
    PERMISSIONS.VIEW_PRODUCTS,
    PERMISSIONS.EDIT_PRODUCTS,
    PERMISSIONS.DELETE_PRODUCTS,
    PERMISSIONS.APPROVE_PRODUCTS,
    PERMISSIONS.FEATURE_PRODUCTS,
    
    PERMISSIONS.VIEW_ORDERS,
    PERMISSIONS.EDIT_ORDERS,
    PERMISSIONS.CANCEL_ORDERS,
    PERMISSIONS.REFUND_ORDERS,
    
    PERMISSIONS.VIEW_COSELLER_STORES,
    PERMISSIONS.APPROVE_COSELLER_STORES,
    PERMISSIONS.EDIT_COSELLER_STORES,
    PERMISSIONS.DELETE_COSELLER_STORES,
    
    PERMISSIONS.VIEW_REPORTS,
    PERMISSIONS.RESOLVE_REPORTS,
    PERMISSIONS.DELETE_REPORTED_CONTENT,
    
    PERMISSIONS.VIEW_LEARNING_RESOURCES,
    PERMISSIONS.CREATE_LEARNING_RESOURCES,
    PERMISSIONS.EDIT_LEARNING_RESOURCES,
    PERMISSIONS.DELETE_LEARNING_RESOURCES,
    
    PERMISSIONS.VIEW_SETTINGS,
    PERMISSIONS.EDIT_SYSTEM_SETTINGS,
    PERMISSIONS.MANAGE_ADMINS,
    
    PERMISSIONS.SEND_NOTIFICATIONS,
    PERMISSIONS.SEND_BULK_NOTIFICATIONS,
    
    PERMISSIONS.VIEW_FINANCIAL_DATA,
    PERMISSIONS.MANAGE_PAYOUTS,
    PERMISSIONS.ADJUST_COMMISSION,
  ],

  [ROLES.ADMIN]: [
    // Can manage most things but not system settings or admins
    PERMISSIONS.VIEW_DASHBOARD,
    PERMISSIONS.VIEW_ANALYTICS,
    PERMISSIONS.EXPORT_REPORTS,
    
    PERMISSIONS.VIEW_USERS,
    PERMISSIONS.EDIT_USERS,
    PERMISSIONS.BAN_USERS,
    PERMISSIONS.VERIFY_SELLERS,
    
    PERMISSIONS.VIEW_PRODUCTS,
    PERMISSIONS.EDIT_PRODUCTS,
    PERMISSIONS.DELETE_PRODUCTS,
    PERMISSIONS.APPROVE_PRODUCTS,
    PERMISSIONS.FEATURE_PRODUCTS,
    
    PERMISSIONS.VIEW_ORDERS,
    PERMISSIONS.EDIT_ORDERS,
    PERMISSIONS.CANCEL_ORDERS,
    PERMISSIONS.REFUND_ORDERS,
    
    PERMISSIONS.VIEW_COSELLER_STORES,
    PERMISSIONS.APPROVE_COSELLER_STORES,
    PERMISSIONS.EDIT_COSELLER_STORES,
    
    PERMISSIONS.VIEW_REPORTS,
    PERMISSIONS.RESOLVE_REPORTS,
    PERMISSIONS.DELETE_REPORTED_CONTENT,
    
    PERMISSIONS.VIEW_LEARNING_RESOURCES,
    // Cannot create/edit/delete learning resources
    
    PERMISSIONS.VIEW_SETTINGS,
    // Cannot edit system settings or manage admins
    
    PERMISSIONS.SEND_NOTIFICATIONS,
    
    PERMISSIONS.VIEW_FINANCIAL_DATA,
    PERMISSIONS.MANAGE_PAYOUTS,
  ],

  [ROLES.MODERATOR]: [
    // Limited moderation capabilities
    PERMISSIONS.VIEW_DASHBOARD,
    PERMISSIONS.VIEW_ANALYTICS,
    
    PERMISSIONS.VIEW_USERS,
    PERMISSIONS.BAN_USERS,
    
    PERMISSIONS.VIEW_PRODUCTS,
    PERMISSIONS.EDIT_PRODUCTS,
    PERMISSIONS.APPROVE_PRODUCTS,
    
    PERMISSIONS.VIEW_ORDERS,
    
    PERMISSIONS.VIEW_COSELLER_STORES,
    PERMISSIONS.APPROVE_COSELLER_STORES,
    
    PERMISSIONS.VIEW_REPORTS,
    PERMISSIONS.RESOLVE_REPORTS,
    
    PERMISSIONS.VIEW_LEARNING_RESOURCES,
    // View only for learning resources
    
    PERMISSIONS.VIEW_SETTINGS,
    // View only for settings
    
    PERMISSIONS.SEND_NOTIFICATIONS,
  ],

  [ROLES.SELLER]: [],
  [ROLES.BUYER]: [],
};

// Helper function to get role display name
export const getRoleName = (role) => {
  const roleNames = {
    [ROLES.SUPER_ADMIN]: 'Super Admin',
    [ROLES.ADMIN]: 'Admin',
    [ROLES.MODERATOR]: 'Moderator',
    [ROLES.SELLER]: 'Seller',
    [ROLES.BUYER]: 'Buyer',
  };
  return roleNames[role] || 'Unknown';
};

// Helper function to get role color
export const getRoleColor = (role) => {
  const roleColors = {
    [ROLES.SUPER_ADMIN]: { bg: 'linear-gradient(135deg, #667eea, #764ba2)', text: 'white' },
    [ROLES.ADMIN]: { bg: 'linear-gradient(135deg, #f093fb, #f5576c)', text: 'white' },
    [ROLES.MODERATOR]: { bg: 'linear-gradient(135deg, #4facfe, #00f2fe)', text: 'white' },
    [ROLES.SELLER]: { bg: 'rgba(76,175,80,0.12)', text: '#4CAF50' },
    [ROLES.BUYER]: { bg: 'rgba(33,150,243,0.12)', text: '#2196F3' },
  };
  return roleColors[role] || { bg: '#e0e0e0', text: '#666' };
};

// Helper function to check if user has permission
export const hasPermission = (userRole, permission) => {
  if (!userRole || !permission) return false;
  const rolePermissions = ROLE_PERMISSIONS[userRole] || [];
  return rolePermissions.includes(permission);
};

// Helper function to check if user has any of the permissions
export const hasAnyPermission = (userRole, permissions) => {
  if (!userRole || !permissions || !Array.isArray(permissions)) return false;
  return permissions.some(permission => hasPermission(userRole, permission));
};

// Helper function to check if user has all permissions
export const hasAllPermissions = (userRole, permissions) => {
  if (!userRole || !permissions || !Array.isArray(permissions)) return false;
  return permissions.every(permission => hasPermission(userRole, permission));
};

// Helper function to get all permissions for a role
export const getRolePermissions = (role) => {
  return ROLE_PERMISSIONS[role] || [];
};

// Helper function to check if role is admin-level
export const isAdminRole = (role) => {
  return [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.MODERATOR].includes(role);
};

// Helper function to check if role can access admin panel
export const canAccessAdminPanel = (role) => {
  return isAdminRole(role);
};

export default {
  ROLES,
  PERMISSIONS,
  ROLE_PERMISSIONS,
  getRoleName,
  getRoleColor,
  hasPermission,
  hasAnyPermission,
  hasAllPermissions,
  getRolePermissions,
  isAdminRole,
  canAccessAdminPanel,
};
