// src/components/dashboard/RecentActivity.jsx
import React, { useState, useEffect } from 'react';
import { Card, CardContent, Box, Typography, Divider, CircularProgress } from '@mui/material';
import PersonAddAltRoundedIcon from '@mui/icons-material/PersonAddAltRounded';
import ShoppingBagRoundedIcon from '@mui/icons-material/ShoppingBagRounded';
import PaletteRoundedIcon from '@mui/icons-material/PaletteRounded';
import { 
  collection, 
  query, 
  orderBy, 
  limit, 
  onSnapshot // ✅ Real-time listener
} from 'firebase/firestore';
import { db } from '../../services/firebase';
import { getTimeAgo } from '../../utils/formatters';

// Activity type → icon + colors config
const ACTIVITY_CONFIG = {
  registration: {
    icon: <PersonAddAltRoundedIcon sx={{ fontSize: 18, color: '#7C4DFF' }} />,
    bg: 'rgba(124,77,255,0.12)',
  },
  order: {
    icon: <ShoppingBagRoundedIcon sx={{ fontSize: 18, color: '#F57C00' }} />,
    bg: 'rgba(245,124,0,0.12)',
  },
  product: {
    icon: <PaletteRoundedIcon sx={{ fontSize: 18, color: '#E91E63' }} />,
    bg: 'rgba(233,30,99,0.12)',
  },
};

/**
 * Helper to safely convert Firestore Timestamp to Date
 */
const convertTimestamp = (timestamp) => {
  if (!timestamp) return new Date();
  if (timestamp.toDate) return timestamp.toDate(); // Firestore Timestamp
  if (timestamp.seconds) return new Date(timestamp.seconds * 1000); // Plain object
  return new Date(timestamp); // Already a date or string
};

const RecentActivity = () => {
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // ✅ Set up real-time listeners for all three collections
    const usersQuery = query(
      collection(db, 'users'), 
      orderBy('created_at', 'desc'), 
      limit(5)
    );
    
    const productsQuery = query(
      collection(db, 'products'), 
      orderBy('created_at', 'desc'), 
      limit(5)
    );
    
    const ordersQuery = query(
      collection(db, 'orders'), 
      orderBy('created_at', 'desc'), 
      limit(5)
    );

    // Store activities from each collection
    let usersData = [];
    let productsData = [];
    let ordersData = [];

    // Function to merge and update activities
    const mergeActivities = () => {
      const merged = [...usersData, ...productsData, ...ordersData];
      merged.sort((a, b) => b.time - a.time);
      setActivities(merged.slice(0, 6)); // Show top 6 most recent
      setLoading(false);
    };

    // ✅ Real-time listener for users
    const unsubscribeUsers = onSnapshot(
      usersQuery,
      (snapshot) => {
        usersData = snapshot.docs.map(doc => {
          const user = doc.data();
          return {
            id: `user_${doc.id}`,
            type: 'registration',
            text: `${user.name || 'A user'} registered as a new ${user.role || 'user'}`,
            time: convertTimestamp(user.created_at),
          };
        });
        mergeActivities();
      },
      (err) => {
        console.error('Error listening to users:', err);
        setError('Failed to load user activities');
        setLoading(false);
      }
    );

    // ✅ Real-time listener for orders
    const unsubscribeOrders = onSnapshot(
      ordersQuery,
      (snapshot) => {
        ordersData = snapshot.docs.map(doc => {
          const order = doc.data();
          return {
            id: `order_${doc.id}`,
            type: 'order',
            text: `New order #${doc.id.slice(0, 8)} placed by ${order.buyer_name || 'a buyer'}`,
            time: convertTimestamp(order.created_at),
          };
        });
        mergeActivities();
      },
      (err) => {
        console.error('Error listening to orders:', err);
        setError('Failed to load order activities');
        setLoading(false);
      }
    );

    // ✅ Real-time listener for products
    const unsubscribeProducts = onSnapshot(
      productsQuery,
      (snapshot) => {
        productsData = snapshot.docs.map(doc => {
          const product = doc.data();
          return {
            id: `product_${doc.id}`,
            type: 'product',
            text: `${product.seller_name || 'A seller'} added "${product.title || 'a product'}"`,
            time: convertTimestamp(product.created_at),
          };
        });
        mergeActivities();
      },
      (err) => {
        console.error('Error listening to products:', err);
        setError('Failed to load product activities');
        setLoading(false);
      }
    );

    // ✅ Cleanup: Unsubscribe from all listeners when component unmounts
    return () => {
      unsubscribeUsers();
      unsubscribeOrders();
      unsubscribeProducts();
    };
  }, []); // Empty dependency array - listeners stay active

  if (loading) {
    return (
      <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none' }}>
        <CardContent sx={{ p: 3 }}>
          <Box sx={{ mb: 2.5 }}>
            <Typography variant="h6" sx={{ fontSize: '1.05rem', fontWeight: 600, color: '#333', mb: 0.5 }}>
              Recent Activity
            </Typography>
            <Typography variant="caption" sx={{ fontSize: '0.75rem', color: '#666' }}>
              Latest platform activities
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
            <CircularProgress sx={{ color: '#E91E63' }} size={30} />
          </Box>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none' }}>
        <CardContent sx={{ p: 3 }}>
          <Box sx={{ mb: 2.5 }}>
            <Typography variant="h6" sx={{ fontSize: '1.05rem', fontWeight: 600, color: '#333', mb: 0.5 }}>
              Recent Activity
            </Typography>
          </Box>
          <Typography variant="body2" color="error" textAlign="center" sx={{ py: 3 }}>
            {error}
          </Typography>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none' }}>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ mb: 2.5 }}>
          <Typography variant="h6" sx={{ fontSize: '1.05rem', fontWeight: 600, color: '#333', mb: 0.5 }}>
            Recent Activity
          </Typography>
          <Typography variant="caption" sx={{ fontSize: '0.75rem', color: '#666' }}>
            Latest platform activities • Live updates
          </Typography>
        </Box>

        {activities.length === 0 ? (
          <Typography variant="body2" color="text.secondary" textAlign="center" sx={{ py: 3 }}>
            No recent activities
          </Typography>
        ) : (
          activities.map((activity, index) => {
            const config = ACTIVITY_CONFIG[activity.type] || ACTIVITY_CONFIG.registration;
            
            return (
              <Box key={activity.id}>
                <Box sx={{ display: 'flex', gap: 1.75, py: 1.75 }}>
                  {/* Icon with colored pill background */}
                  <Box
                    sx={{
                      width: 40,
                      height: 40,
                      borderRadius: '10px',
                      background: config.bg,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                    }}
                  >
                    {config.icon}
                  </Box>

                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography
                      sx={{
                        fontSize: '0.8rem',
                        color: '#333',
                        mb: 0.5,
                        '& strong': { fontWeight: 600, color: '#E91E63' },
                      }}
                      dangerouslySetInnerHTML={{
                        __html: activity.text.replace(
                          /([A-Z][a-z]+\s[A-Z][a-z]+|#[\w]+|"[^"]+"|[A-Z][a-z]+\s[A-Z][a-z]+\s[A-Z][a-z]+)/g,
                          '<strong>$1</strong>'
                        ),
                      }}
                    />
                    <Typography sx={{ fontSize: '0.7rem', color: '#999' }}>
                      {getTimeAgo(activity.time)}
                    </Typography>
                  </Box>
                </Box>

                {index < activities.length - 1 && (
                  <Divider sx={{ borderColor: '#f0f0f0', borderWidth: '2px' }} />
                )}
              </Box>
            );
          })
        )}
      </CardContent>
    </Card>
  );
};

export default RecentActivity;
