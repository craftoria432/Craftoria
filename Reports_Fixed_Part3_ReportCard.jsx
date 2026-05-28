// PART 3: Updated Report Card Display
// Replace the report card content section (inside the map function) with this:

{/* ✅ NEW: Clear Summary - Who Reported What */}
<Box sx={{ 
  mb: 2, 
  p: 2, 
  background: 'linear-gradient(135deg, #e3f2fd, #f3e5f5)', 
  borderRadius: '12px',
  border: '2px solid #ce93d8'
}}>
  <Typography sx={{ 
    fontSize: '1rem', 
    fontWeight: 700, 
    color: '#333',
    textAlign: 'center',
    lineHeight: 1.6
  }}>
    <span style={{ color: '#e91e63', fontWeight: 800 }}>
      {report.reporter?.name || 'Unknown User'}
    </span>
    {' reported '}
    <span style={{ color: '#f57c00', fontWeight: 800 }}>
      {report.reportedEntity?.name || 'Unknown Entity'}
    </span>
  </Typography>
  <Typography sx={{ 
    fontSize: '0.75rem', 
    color: '#666',
    textAlign: 'center',
    mt: 0.5
  }}>
    Report Type: <strong style={{ textTransform: 'capitalize' }}>{report.typeKey}</strong>
  </Typography>
</Box>

{/* ✅ UPDATED: Reporter Information - Highlighted */}
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

{/* ✅ UPDATED: Reported Entity - Highlighted */}
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
    {report.reportedEntity?.name || 'Unknown Entity'}
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
    label={report.typeKey} 
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
