# Web CoSellerStores.jsx - Rating Integration Updates

## Changes Required for Rating Feature Sync

The web dashboard needs to be updated to match the Android app's rating improvements.

---

## 1. Add Rating Stats to Stats Calculation

### Location: After line ~180 (in the component)

### Add this code:
```javascript
// ✅ Calculate stats including ratings
const stats = useMemo(() => {
  return {
    totalActive: stores.filter(s => s.is_active === true && s.is_flagged !== true).length,
    totalProducts: stores.reduce((sum, s) => sum + (s.product_count || 0), 0),
    totalMembers: stores.reduce((sum, s) => sum + (s.member_count || 0), 0),
    flaggedStores: stores.filter(s => s.is_flagged === true).length,
    totalRatings: stores.reduce((sum, s) => sum + (s.rating_count || 0), 0),
    avgRating: stores.length > 0 
      ? (stores.reduce((sum, s) => sum + (s.average_rating || 0), 0) / stores.length).toFixed(1) 
      : 0,
  };
}, [stores]);
```

---

## 2. Add Rating Column to Table Header

### Location: In the TableHead section where headers are defined

### Change from:
```javascript
{['Store', 'Owner', 'Members', 'Products', 'Sales', 'Status', 'Created', 'Actions'].map(h => (
```

### Change to:
```javascript
{['Store', 'Owner', 'Members', 'Products', 'Rating', 'Sales', 'Status', 'Created', 'Actions'].map(h => (
```

---

## 3. Add Rating Display in Table Body

### Location: In the TableBody where store data is displayed

### Add this cell after Products column:
```javascript
{/* Rating cell */}
<TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
    <StarIcon sx={{ fontSize: 16, color: '#FFB400' }} />
    <Typography sx={{ fontSize: '0.85rem', fontWeight: 600 }}>
      {store.average_rating ? `${store.average_rating.toFixed(1)}` : 'New'}
    </Typography>
    {store.rating_count > 0 && (
      <Typography sx={{ fontSize: '0.75rem', color: '#999' }}>
        ({store.rating_count})
      </Typography>
    )}
  </Box>
</TableCell>
```

---

## 4. Update View Modal to Show Rating Details

### Location: In the ViewModal DialogContent section

### Add this section after Store Statistics:
```javascript
{/* Rating Information */}
<Box sx={{ mb: 3 }}>
  <Typography sx={{ fontSize: '0.9rem', fontWeight: 600, color: '#333', mb: 1.5 }}>
    Rating Information
  </Typography>
  <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 2 }}>
    <Box sx={{ p: 1.5, background: '#f5f5f5', borderRadius: '10px' }}>
      <Typography sx={{ fontSize: '0.7rem', color: '#666', mb: 0.5 }}>
        Average Rating
      </Typography>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
        <StarIcon sx={{ fontSize: 18, color: '#FFB400' }} />
        <Typography sx={{ fontSize: '1.2rem', fontWeight: 700, color: '#333' }}>
          {viewModal.store?.average_rating ? viewModal.store.average_rating.toFixed(1) : 'N/A'}
        </Typography>
      </Box>
    </Box>
    <Box sx={{ p: 1.5, background: '#f5f5f5', borderRadius: '10px' }}>
      <Typography sx={{ fontSize: '0.7rem', color: '#666', mb: 0.5 }}>
        Total Ratings
      </Typography>
      <Typography sx={{ fontSize: '1.2rem', fontWeight: 700, color: '#333' }}>
        {viewModal.store?.rating_count || 0}
      </Typography>
    </Box>
  </Box>
</Box>
```

---

## 5. Update Stat Cards Display

### Location: Where StatCard components are rendered (around line 400-450)

### Add rating stat card:
```javascript
<StatCard 
  title="Average Store Rating" 
  value={stats.avgRating} 
  change={5} 
  iconType="rating" 
/>
```

---

## 6. Add Star Icon Import

### Location: At the top with other imports

### Add:
```javascript
import { Star as StarIcon } from '@mui/icons-material';
```

---

## 7. Update Firestore Query to Include Rating Fields

### Location: In the onSnapshot query where stores are fetched

### Ensure these fields are included in the data mapping:
```javascript
const data = snapshot.docs.map((doc) => {
  const d = doc.data();
  return {
    id: doc.id,
    ...d,
    average_rating: d.average_rating || 0,      // ADD THIS
    rating_count: d.rating_count || 0,          // ADD THIS
    created_at: convertTimestamp(d.created_at),
    // ... rest of fields
  };
});
```

---

## 8. Update Filter/Sort Options (Optional)

### Add rating-based sorting:
```javascript
if (sortBy === 'rating') {
  filtered = [...filtered].sort((a, b) => 
    (b.average_rating || 0) - (a.average_rating || 0)
  );
}
```

### Add to sort dropdown:
```javascript
<MenuItem value="rating">Highest Rated</MenuItem>
```

---

## Summary of Changes

| Change | Type | Impact |
|--------|------|--------|
| Add rating stats | Data | Shows overall platform rating metrics |
| Add rating column | UI | Displays store ratings in table |
| Show rating count | UI | Shows transparency (e.g., "4.5 (23)") |
| Update view modal | UI | Shows detailed rating info |
| Add stat card | UI | Displays average rating metric |
| Add Star icon | Import | Visual indicator for ratings |
| Update Firestore query | Data | Fetches rating fields |
| Add rating sort | Feature | Sort stores by rating |

---

## Testing Checklist

- [ ] Rating column displays in table
- [ ] Rating count shows correctly (e.g., "4.5 (23)")
- [ ] "New" displays for unrated stores
- [ ] View modal shows rating details
- [ ] Stat card shows average rating
- [ ] Sorting by rating works
- [ ] No console errors
- [ ] Responsive on mobile

---

## Deployment Notes

1. **Backward Compatibility:** Stores without `rating_count` field will show 0
2. **Default Values:** Uses `|| 0` to handle missing fields
3. **Display Format:** "4.5 (23)" shows average and count
4. **No Breaking Changes:** Existing functionality remains intact

---

## Integration with Android App

These changes ensure the web dashboard mirrors the Android app:

✅ **Android App Changes:**
- Added `ratingCount` field to CoSellerStore
- Display format: "4.5⭐ (23)"
- Notifications sent to store owners
- Buyer name included in notifications

✅ **Web Dashboard Changes:**
- Add rating column to table
- Display format: "4.5 (23)"
- Show rating in view modal
- Add rating stat card

---

## Files to Update

1. `src/pages/CoSellerStores.jsx` - Main changes
2. Ensure Firestore has `rating_count` field on all stores

---

## Next Steps

1. Apply these changes to CoSellerStores.jsx
2. Test rating display
3. Verify Firestore data includes rating fields
4. Deploy to production
5. Monitor for any issues

---

**Status:** Ready for Implementation
**Priority:** High (Sync with Android app)
**Complexity:** Low (UI changes only)
