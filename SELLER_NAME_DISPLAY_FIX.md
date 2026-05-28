# Seller Name Not Showing - Quick Fix

## Problem

Seller names show as "Unknown Seller" in the Product Management table because of a field name mismatch.

**Firebase has**: `seller_name`  
**Code is looking for**: `seller`

---

## Solution

Update `ProductManagement.jsx` in **3 places** to use `seller_name` instead of `seller`:

### Fix 1: Table Display (Line ~XXX)

**Find this:**
```javascript
<TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
  {product.seller || 'Unknown Seller'}
</TableCell>
```

**Replace with:**
```javascript
<TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
  {product.seller_name || product.seller || 'Unknown Seller'}
</TableCell>
```

### Fix 2: View Modal (Line ~XXX)

**Find this:**
```javascript
<Typography sx={{ fontSize: '0.85rem', color: '#333', fontWeight: 500 }}>
  {viewModal.product.seller || '—'}
</Typography>
```

**Replace with:**
```javascript
<Typography sx={{ fontSize: '0.85rem', color: '#333', fontWeight: 500 }}>
  {viewModal.product.seller_name || viewModal.product.seller || '—'}
</Typography>
```

### Fix 3: Search Filter (Line ~XXX)

**Find this:**
```javascript
f = f.filter(p => 
  p.title?.toLowerCase().includes(q) || 
  p.seller?.toLowerCase().includes(q) ||  // ❌ Wrong field
  p.description?.toLowerCase().includes(q)
);
```

**Replace with:**
```javascript
f = f.filter(p => 
  p.title?.toLowerCase().includes(q) || 
  p.seller_name?.toLowerCase().includes(q) ||  // ✅ Correct field
  p.seller?.toLowerCase().includes(q) ||        // ✅ Fallback for old data
  p.description?.toLowerCase().includes(q)
);
```

---

## Why This Happened

Your mobile app (Kotlin) uses `seller_name`:

```kotlin
data class Product(
    val seller_name: String,  // ✅ snake_case
    // ...
)
```

But the web dashboard was looking for `seller` (without underscore).

---

## Complete Fixed Code Sections

### Section 1: Table Cell
```javascript
{/* Seller cell */}
<TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
  {product.seller_name || product.seller || 'Unknown Seller'}
</TableCell>
```

### Section 2: View Modal - Seller Display
```javascript
<Grid item xs={6}>
  <Typography sx={{ 
    fontSize: '0.75rem', 
    fontWeight: 600, 
    color: '#999', 
    textTransform: 'uppercase', 
    mb: 0.75 
  }}>
    Seller
  </Typography>
  <Typography sx={{ 
    fontSize: '0.85rem', 
    color: '#333', 
    fontWeight: 500 
  }}>
    {viewModal.product.seller_name || viewModal.product.seller || '—'}
  </Typography>
</Grid>
```

### Section 3: Search Filter Logic
```javascript
const filteredProducts = React.useMemo(() => {
  let f = [...products];
  
  if (statusFilter !== 'all') 
    f = f.filter(p => (p.status || 'active').toLowerCase() === statusFilter);
  
  if (searchQuery.trim()) {
    const q = searchQuery.toLowerCase().trim();
    f = f.filter(p => 
      p.title?.toLowerCase().includes(q) || 
      p.seller_name?.toLowerCase().includes(q) ||  // ✅ Primary field
      p.seller?.toLowerCase().includes(q) ||        // ✅ Fallback
      p.description?.toLowerCase().includes(q)
    );
  }
  
  if (categoryFilter) 
    f = f.filter(p => p.category === categoryFilter);
  
  if (priceFilter) {
    const [min, max] = priceFilter.split('-').map(Number);
    f = f.filter(p => { 
      const price = Number(p.price) || 0; 
      return max ? price >= min && price <= max : price >= min; 
    });
  }
  
  return f;
}, [statusFilter, searchQuery, categoryFilter, priceFilter, products]);
```

---

## Testing After Fix

1. **Refresh the web dashboard**
2. **Check Product Management page**
3. **Verify seller names appear** (should show "Test Seller", "Zara Ahmed", etc.)
4. **Test search** by seller name
5. **Open View modal** and verify seller name shows

---

## Field Name Reference

### Correct Field Names (Match Mobile App)

| Display | Firebase Field | Type |
|---------|---------------|------|
| Product Title | `title` | string |
| Seller Name | `seller_name` | string |
| Seller ID | `seller_id` | string |
| Price | `price` | number |
| Category | `category` | string |
| Status | `status` | string |
| Stock | `stock` | number |
| Description | `description` | string |
| Created At | `created_at` | Timestamp |
| Updated At | `updated_at` | Timestamp |

---

## Why Use Both Fields?

```javascript
product.seller_name || product.seller || 'Unknown Seller'
```

This pattern:
1. **First tries** `seller_name` (new/correct field)
2. **Falls back to** `seller` (old field, if exists)
3. **Defaults to** 'Unknown Seller' (if neither exists)

This ensures compatibility with:
- New products from mobile app (use `seller_name`)
- Old products that might have `seller`
- Products with neither field

---

## Quick Copy-Paste Fix

If you want to fix it quickly, search for these 3 patterns in your `ProductManagement.jsx`:

**Pattern 1:**
```
product.seller ||
```
Replace with:
```
product.seller_name || product.seller ||
```

**Pattern 2:**
```
viewModal.product.seller ||
```
Replace with:
```
viewModal.product.seller_name || viewModal.product.seller ||
```

**Pattern 3:**
```
p.seller?.toLowerCase().includes(q)
```
Replace with:
```
p.seller_name?.toLowerCase().includes(q) || p.seller?.toLowerCase().includes(q)
```

---

## After Applying Fix

Your Product Management table will show:
- ✅ "Test Seller" instead of "Unknown Seller"
- ✅ "Zara Ahmed" instead of "Unknown Seller"
- ✅ Actual seller names from Firebase

---

**Status**: Ready to apply  
**Impact**: Immediate - seller names will display correctly  
**Risk**: None - adds fallback for compatibility
