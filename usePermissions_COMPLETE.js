// src/hooks/usePermissions.js - COMPLETE PRODUCTION VERSION

import { useMemo } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { ROLE_PERMISSIONS, hasPermission, hasAnyPermission, hasAllPermissions } from '../config/permissions';

/**
 * Custom hook for checking user permissions
 * 
 * Usage:
 * const { can, canAny, canAll, permissions } = usePermissions();
 * 
 * if (can(PERMISSIONS.EDIT_USERS)) {
 *   // Show edit button
 * }
 */
export const usePermissions = () => {
  const { currentUser } = useAuth();
  const userRole = currentUser?.role;

  // Get all permissions for current user's role
  const permissions = useMemo(() => {
    if (!userRole) return [];
    return ROLE_PERMISSIONS[userRole] || [];
  }, [userRole]);

  // Check if user has a specific permission
  const can = useMemo(() => {
    return (permission) => {
      if (!userRole || !permission) return false;
      return hasPermission(userRole, permission);
    };
  }, [userRole]);

  // Check if user has any of the specified permissions
  const canAny = useMemo(() => {
    return (permissionArray) => {
      if (!userRole || !permissionArray || !Array.isArray(permissionArray)) return false;
      return hasAnyPermission(userRole, permissionArray);
    };
  }, [userRole]);

  // Check if user has all of the specified permissions
  const canAll = useMemo(() => {
    return (permissionArray) => {
      if (!userRole || !permissionArray || !Array.isArray(permissionArray)) return false;
      return hasAllPermissions(userRole, permissionArray);
    };
  }, [userRole]);

  return {
    can,
    canAny,
    canAll,
    permissions,
    userRole,
  };
};

export default usePermissions;
