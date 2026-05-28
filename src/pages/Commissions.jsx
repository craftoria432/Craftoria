import React, { useEffect, useState } from 'react';
import { useAllCommissions } from '../hooks/useCommissions';
import {
  subscribeToPendingCommissions,
  getCommissionStats,
  getAdminEarnings,
} from '../services/commissionService';
import { toast } from 'react-toastify';
import './Commissions.css';

const Commissions = () => {
  const { commissions, loading, error, hasMore, loadMore, refetch } =
    useAllCommissions(20);
  const [pendingCommissions, setPendingCommissions] = useState([]);
  const [stats, setStats] = useState({
    totalCommissions: 0,
    pendingAmount: 0,
    paidAmount: 0,
  });
  const [adminEarnings, setAdminEarnings] = useState(0);
  const [dateRange, setDateRange] = useState({
    startDate: new Date(new Date().setDate(new Date().getDate() - 30)),
    endDate: new Date(),
  });
  const [statsLoading, setStatsLoading] = useState(false);

  // Subscribe to pending commissions
  useEffect(() => {
    const unsubscribe = subscribeToPendingCommissions(
      (data) => {
        setPendingCommissions(data);
      },
      (error) => {
        console.error('Error in pending commissions subscription:', error);
        toast.error('Failed to load pending commissions');
      }
    );

    return () => {
      if (unsubscribe) unsubscribe();
    };
  }, []);

  // Fetch stats on date range change
  useEffect(() => {
    const fetchStats = async () => {
      try {
        setStatsLoading(true);
        const statsData = await getCommissionStats(
          dateRange.startDate,
          dateRange.endDate
        );
        setStats(statsData);
      } catch (err) {
        console.error('Error fetching stats:', err);
        toast.error('Failed to load commission statistics');
      } finally {
        setStatsLoading(false);
      }
    };

    fetchStats();
  }, [dateRange]);

  // Fetch admin earnings
  useEffect(() => {
    const fetchEarnings = async () => {
      try {
        const earnings = await getAdminEarnings();
        setAdminEarnings(earnings.totalEarnings);
      } catch (err) {
        console.error('Error fetching admin earnings:', err);
      }
    };

    fetchEarnings();
  }, []);

  /**
   * ✅ FIX: Refresh button now actually triggers data refresh
   * Instead of just showing a toast, it refetches the stats and commissions
   */
  const handleRefresh = () => {
    try {
      // Refetch commissions list
      refetch();

      // Refetch stats for current date range
      const fetchStats = async () => {
        try {
          setStatsLoading(true);
          const statsData = await getCommissionStats(
            dateRange.startDate,
            dateRange.endDate
          );
          setStats(statsData);
        } catch (err) {
          console.error('Error refreshing stats:', err);
          toast.error('Failed to refresh statistics');
        } finally {
          setStatsLoading(false);
        }
      };

      fetchStats();

      // Refetch admin earnings
      const fetchEarnings = async () => {
        try {
          const earnings = await getAdminEarnings();
          setAdminEarnings(earnings.totalEarnings);
        } catch (err) {
          console.error('Error refreshing earnings:', err);
        }
      };

      fetchEarnings();

      toast.success('Data refreshed successfully');
    } catch (err) {
      console.error('Error during refresh:', err);
      toast.error('Failed to refresh data');
    }
  };

  const handleDateRangeChange = (field, value) => {
    setDateRange((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  return (
    <div className="commissions-container">
      <div className="commissions-header">
        <h1>Commission Management</h1>
        <button
          className="refresh-btn"
          onClick={handleRefresh}
          disabled={loading || statsLoading}
        >
          {loading || statsLoading ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {/* Admin Earnings Card */}
      <div className="earnings-card">
        <h2>Total Admin Earnings</h2>
        <p className="earnings-amount">PKR {adminEarnings.toLocaleString()}</p>
      </div>

      {/* Statistics Section */}
      <div className="stats-section">
        <h2>Commission Statistics</h2>

        {/* Date Range Filter */}
        <div className="date-range-filter">
          <div className="date-input-group">
            <label>Start Date</label>
            <input
              type="date"
              value={dateRange.startDate.toISOString().split('T')[0]}
              onChange={(e) =>
                handleDateRangeChange('startDate', new Date(e.target.value))
              }
            />
          </div>
          <div className="date-input-group">
            <label>End Date</label>
            <input
              type="date"
              value={dateRange.endDate.toISOString().split('T')[0]}
              onChange={(e) =>
                handleDateRangeChange('endDate', new Date(e.target.value))
              }
            />
          </div>
        </div>

        {/* Stats Cards */}
        <div className="stats-grid">
          <div className="stat-card">
            <h3>Total Commissions</h3>
            <p className="stat-value">
              PKR {stats.totalCommissions.toLocaleString()}
            </p>
          </div>
          <div className="stat-card">
            <h3>Paid Amount</h3>
            <p className="stat-value">
              PKR {stats.paidAmount.toLocaleString()}
            </p>
          </div>
          <div className="stat-card">
            <h3>Pending Amount</h3>
            <p className="stat-value">
              PKR {stats.pendingAmount.toLocaleString()}
            </p>
          </div>
          <div className="stat-card">
            <h3>Commission Count</h3>
            <p className="stat-value">{stats.count}</p>
          </div>
        </div>
      </div>

      {/* Pending Commissions Section */}
      <div className="pending-section">
        <h2>Pending Commissions ({pendingCommissions.length})</h2>
        {pendingCommissions.length === 0 ? (
          <p className="no-data">No pending commissions</p>
        ) : (
          <div className="commissions-list">
            {pendingCommissions.map((commission) => (
              <div key={commission.id} className="commission-item">
                <div className="commission-info">
                  <p className="seller-name">{commission.seller_name}</p>
                  <p className="commission-amount">
                    PKR {commission.commission_amount?.toLocaleString() || 0}
                  </p>
                  <p className="commission-date">
                    {new Date(commission.created_at).toLocaleDateString()}
                  </p>
                </div>
                <div className="commission-status">
                  <span className="status-badge pending">
                    {commission.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* All Commissions Section */}
      <div className="all-commissions-section">
        <h2>All Commissions</h2>
        {error && <p className="error-message">{error}</p>}
        {commissions.length === 0 && !loading ? (
          <p className="no-data">No commissions found</p>
        ) : (
          <>
            <div className="commissions-list">
              {commissions.map((commission) => (
                <div key={commission.id} className="commission-item">
                  <div className="commission-info">
                    <p className="seller-name">{commission.seller_name}</p>
                    <p className="commission-amount">
                      PKR {commission.commission_amount?.toLocaleString() || 0}
                    </p>
                    <p className="commission-date">
                      {new Date(commission.created_at).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="commission-status">
                    <span
                      className={`status-badge ${commission.status?.toLowerCase()}`}
                    >
                      {commission.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>
            {hasMore && (
              <button
                className="load-more-btn"
                onClick={loadMore}
                disabled={loading}
              >
                {loading ? 'Loading...' : 'Load More'}
              </button>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default Commissions;
