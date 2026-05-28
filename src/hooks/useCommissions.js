import { useCallback, useEffect, useState } from 'react';
import { getAllCommissions } from '../services/commissionService';

/**
 * Hook for paginated commission fetching
 * ✅ FIX: Removed lastDoc from useCallback dependencies to prevent infinite loop
 * Now lastDoc is passed directly as a parameter instead of being a dependency
 */
export const useAllCommissions = (pageSize = 20) => {
  const [commissions, setCommissions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [hasMore, setHasMore] = useState(true);
  const [lastDoc, setLastDoc] = useState(null);

  // ✅ FIX: Removed lastDoc from dependencies
  // Now we pass it directly to fetchCommissions instead of relying on closure
  const fetchCommissions = useCallback(
    async (isNextPage = false, currentLastDoc = null) => {
      try {
        setLoading(true);
        const { commissions: data, lastDoc: newLastDoc, hasMore: more } =
          await getAllCommissions(pageSize, isNextPage ? currentLastDoc : null);

        if (isNextPage) {
          setCommissions((prev) => [...prev, ...data]);
        } else {
          setCommissions(data);
        }

        setLastDoc(newLastDoc);
        setHasMore(more);
        setError(null);
      } catch (err) {
        setError(err.message);
        console.error('Error fetching commissions:', err);
      } finally {
        setLoading(false);
      }
    },
    [pageSize]
  );

  // Initial fetch
  useEffect(() => {
    fetchCommissions(false);
  }, [fetchCommissions]);

  // ✅ FIX: loadMore now passes lastDoc directly instead of relying on stale closure
  const loadMore = useCallback(() => {
    if (hasMore && !loading) {
      fetchCommissions(true, lastDoc);
    }
  }, [hasMore, loading, lastDoc, fetchCommissions]);

  return {
    commissions,
    loading,
    error,
    hasMore,
    loadMore,
    refetch: () => fetchCommissions(false),
  };
};
