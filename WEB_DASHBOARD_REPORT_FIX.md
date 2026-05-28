# Web Dashboard Report Display Fix

## Problem
The web admin dashboard shows "Reported by: Unknown User" and doesn't clearly display which buyer reported which seller/product.

## Root Cause
The Firebase data mapping in Reports.jsx is not correctly extracting the `reporter_name` and `reported_entity_name` fields that the Android app sends.

## Android App Data Structure (Confirmed)
The Android app sends reports with these fields:
```javascript
{
  type: "product" | "seller" | "buyer" | "technical",
  reporter_id: "user123",
  reporter_name: "Ahmed Ali",           // ✅ This exists
  reported_entity_id: "product456",
  reported_entity_name: "Ceramic Vase", // ✅ This exists
  reason: "Misleading description",
  description: "Full description...",
  status: "New" | "Under Review" | "Resolved",
  created_at: 1234567890,
  updated_at: 1234567890
}
```

## Solution: Update Reports.jsx Data Mapping

### STEP 1: Fix the loadReports() function data transformation

Replace the current transformation (around line 100-120) with:

```javascript
const data = snapshot.docs.map(d => {
  const docData = d.data();
  
  return {
    id: d.id,
    type: docData.type || 'product',
    typeKey: docData.type || 'product',
    
    // ✅ FIX: Correctly map reporter information
    reporter: {
      id: docData.reporter_id || '',
      name: docData.reporter_name || 'Unknown User',
      avatar: (docData.reporter_name || 'U').substring(0, 2).toUpperCase()
    },
    
    // ✅ FIX: Correctly map reported entity
    reportedEntity: {
      id: docData.reported_entity_id || '',
      name: docData.reported_entity_name || 'Unknown Entity'
    },
    
    reason: docData.reason || 'No reason provided',
    description: docData.description || 'No description provided',
    status: docData.status || 'New',
    date: docData.created_at 
      ? new Date(docData.created_at).toLocaleDateString('en-US', { 
          year: 'numeric', 
          month: 'long', 
          day: 'numeric' 
        })
      : new Date().toLocaleDateString(),
    evidence: docData.evidence || [],
    source: docData.source || 'mobile app'
  };
});
```

### STEP 2: Update the Report Card Display

Replace the "Reported by" and "Reported Entity" sections (around line 300-330) with:

```javascript
{/* Reporter Information - Highlighted */}
<Box sx={{ 
  display: 'flex', 
  alignItems: 'center', 
  gap: 1.25, 
  mb: 1.5, 
  p: 1.5, 
  background: 'linear-gradient(135deg, #fff5f8, #ffe8f0)', 
  border: '2px solid #f8bbd0',
  borderRadius: '12px' 
}}>
  <Avatar sx={{ 
    width: 40, 
    height: 40, 
    background: 'linear-gradient(135deg, #667eea, #764ba2)', 
    fontSize: '0.9rem', 
    fontWeight: 700 
  }}>
    {report.reporter?.avatar || 'U'}
  </Avatar>
  <Box sx={{ flex: 1 }}>
    <Typography sx={{ 
      fontSize: '0.7rem', 
      fontWeight: 600, 
      color: '#999', 
      textTransform: 'uppercase',
      mb: 0.25
    }}>
      Reporter (Who Reported)
    </Typography>
    <Typography sx={{ 
      fontSize: '0.95rem', 
      fontWeight: 700, 
      color: '#e91e63' 
    }}>
      {report.reporter?.name || 'Unknown User'}
    </Typography>
    {report.reporter?.id && (
      <Typography sx={{ 
        fontSize: '0.7rem', 
        color: '#999',
        fontFamily: 'monospace'
      }}>
        ID: {report.reporter.id}
      </Typography>
    )}
  </Box>
  <Typography sx={{ fontSize: '0.7rem', color: '#999' }}>
    {report.date}
  </Typography>
</Box>

{/* Reported Entity - Highlighted */}
<Box sx={{ 
  mb: 1.5, 
  p: 1.5, 
  background: 'linear-gradient(135deg, #fff8e1, #ffecb3)', 
  border: '2px solid #ffe082',
  borderRadius: '12px' 
}}>
  <Typography sx={{ 
    fontSize: '0.7rem', 
    fontWeight: 600, 
    color: '#999', 
    textTransform: 'uppercase', 
    mb: 0.5 
  }}>
    Reported Entity (What Was Reported)
  </Typography>
  <Typography sx={{ 
    fontSize: '0.95rem', 
    fontWeight: 700, 
    color: '#f57c00',
    mb: 0.25
  }}>
    {report.reportedEntity?.name || report.reportedEntity || 'Unknown Entity'}
  </Typography>
  {report.reportedEntity?.id && (
    <Typography sx={{ 
      fontSize: '0.7rem', 
      color: '#999',
      fontFamily: 'monospace'
    }}>
      ID: {report.reportedEntity.id}
    </Typography>
  )}
  <Chip 
    label={report.type || 'Unknown Type'} 
    size="small"
    sx={{ 
      mt: 0.75,
      background: '#fff',
      border: '1px solid #ffe082',
      fontSize: '0.7rem',
      fontWeight: 600,
      textTransform: 'capitalize'
    }}
  />
</Box>
```



### STEP 3: Add Clear Summary at Top of Each Report Card

Add this right after the report header (around line 280):

```javascript
{/* Clear Summary: Who Reported What */}
<Box sx={{ 
  mb: 2, 
  p: 2, 
  background: 'linear-gradient(135deg, #e3f2fd, #f3e5f5)', 
  borderRadius: '12px',
  border: '2px solid #ce93d8'
}}>
  <Typography sx={{ 
    fontSize: '0.95rem', 
    fontWeight: 700, 
    color: '#333',
    textAlign: 'center',
    lineHeight: 1.6
  }}>
    <span style={{ color: '#e91e63' }}>
      {report.reporter?.name || 'Unknown User'}
    </span>
    {' reported '}
    <span style={{ color: '#f57c00' }}>
      {report.reportedEntity?.name || report.reportedEntity || 'Unknown Entity'}
    </span>
  </Typography>
  <Typography sx={{ 
    fontSize: '0.75rem', 
    color: '#666',
    textAlign: 'center',
    mt: 0.5
  }}>
    Report Type: <strong style={{ textTransform: 'capitalize' }}>{report.type}</strong>
  </Typography>
</Box>
```

## Visual Result

After these changes, each report card will show:

```
┌─────────────────────────────────────────────────┐
│  [Icon] Product Report              [New Badge] │
│                                                  │
│  ┌───────────────────────────────────────────┐ │
│  │ Ahmed Ali reported Ceramic Vase           │ │
│  │ Report Type: product                      │ │
│  └───────────────────────────────────────────┘ │
│                                                  │
│  ┌───────────────────────────────────────────┐ │
│  │ 👤 REPORTER (WHO REPORTED)                │ │
│  │ Ahmed Ali                                  │ │
│  │ ID: user123                                │ │
│  └───────────────────────────────────────────┘ │
│                                                  │
│  ┌───────────────────────────────────────────┐ │
│  │ 📦 REPORTED ENTITY (WHAT WAS REPORTED)    │ │
│  │ Ceramic Vase - Traditional Design         │ │
│  │ ID: product456                             │ │
│  │ [product]                                  │ │
│  └───────────────────────────────────────────┘ │
│                                                  │
│  REASON: Misleading product description        │
│  Description: The product images show...       │
│                                                  │
│  [Investigate] [Take Action] [Dismiss] [Contact]│
└─────────────────────────────────────────────────┘
```

## Complete Updated loadReports Function

```javascript
const loadReports = useCallback(async () => {
  try {
    setLoading(true);
    const snapshot = await getDocs(query(collection(db, 'reports')));
    
    if (snapshot.docs.length > 0) {
      const data = snapshot.docs.map(d => {
        const docData = d.data();
        
        // Transform Firebase data to match app schema
        return {
          id: d.id,
          type: getTypeLabel(docData.type || 'product'),
          typeKey: docData.type || 'product',
          icon: docData.type || 'product',
          
          // ✅ Correctly extract reporter information
          reporter: {
            id: docData.reporter_id || '',
            name: docData.reporter_name || 'Unknown User',
            avatar: (docData.reporter_name || 'U').substring(0, 2).toUpperCase()
          },
          
          // ✅ Correctly extract reported entity information
          reportedEntity: {
            id: docData.reported_entity_id || '',
            name: docData.reported_entity_name || 'Unknown Entity'
          },
          
          reason: docData.reason || 'No reason provided',
          description: docData.description || 'No description provided',
          status: docData.status || 'New',
          date: docData.created_at 
            ? new Date(docData.created_at).toLocaleDateString('en-US', { 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric' 
              })
            : new Date().toLocaleDateString(),
          evidence: docData.evidence || [],
          source: docData.source || 'mobile app'
        };
      });
      
      setReports(data);
      setFilteredReports(data);
    } else {
      setReports(sampleReports);
      setFilteredReports(sampleReports);
    }
  } catch (error) {
    console.error('Error loading reports:', error);
    setReports(sampleReports);
    setFilteredReports(sampleReports);
  } finally {
    setLoading(false);
  }
}, []);

// Helper function to get readable type labels
const getTypeLabel = (type) => {
  const labels = {
    'product': 'Inappropriate Products',
    'seller': 'Seller Misconduct',
    'buyer': 'Buyer Complaints',
    'technical': 'Technical Issues'
  };
  return labels[type] || 'Inappropriate Products';
};
```

## Testing Checklist

After implementing these changes:

1. ✅ Reporter name displays correctly (not "Unknown User")
2. ✅ Reported entity name displays correctly (not "Unknown")
3. ✅ Summary shows "X reported Y" clearly at top
4. ✅ Reporter ID and Entity ID visible for admin reference
5. ✅ Report type badge shows correctly
6. ✅ Date formats properly from timestamp
7. ✅ All existing functionality (filters, actions) still works

## Implementation Priority

1. **CRITICAL**: Fix data mapping in `loadReports()` function
2. **HIGH**: Add clear summary box showing "X reported Y"
3. **MEDIUM**: Update reporter and entity display sections with highlighting
4. **LOW**: Add IDs for admin reference

This will make it crystal clear which buyer reported which seller/product!
