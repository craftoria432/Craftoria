// src/components/dashboard/MLKitStatsPanel.jsx
// ✅ PRODUCTION-READY: ML Kit statistics and quality metrics dashboard
import { Box, Card, CardContent, Typography, Grid } from '@mui/material';
import { Psychology as PsychologyIcon } from '@mui/icons-material';

const MLKitStatsPanel = ({ verifications }) => {
  if (!verifications || verifications.length === 0) return null;

  // Calculate ML Kit statistics
  const stats = {
    totalVerifications: verifications.length,
    avgConfidence: (
      verifications.reduce((sum, v) => sum + (v.mlKitResult?.confidence || 0), 0) /
      verifications.length
    ).toFixed(1),
    highQuality: verifications.filter(v => (v.mlKitResult?.confidence || 0) >= 80).length,
    mediumQuality: verifications.filter(
      v => (v.mlKitResult?.confidence || 0) >= 50 && (v.mlKitResult?.confidence || 0) < 80
    ).length,
    lowQuality: verifications.filter(v => (v.mlKitResult?.confidence || 0) < 50).length,
    validFaces: verifications.filter(v => v.mlKitResult?.isValid).length,
    invalidFaces: verifications.filter(v => !v.mlKitResult?.isValid).length,
  };

  const highQualityPercent = ((stats.highQuality / stats.totalVerifications) * 100).toFixed(1);
  const validFacePercent = ((stats.validFaces / stats.totalVerifications) * 100).toFixed(1);

  return (
    <Card
      sx={{
        borderRadius: '12px',
        border: '1px solid #eef0f4',
        boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
        background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
        mb: '20px',
      }}
    >
      <CardContent sx={{ p: '20px !important' }}>
        {/* Header */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: '10px', mb: '16px' }}>
          <PsychologyIcon sx={{ fontSize: 22, color: '#E91E63' }} />
          <Typography sx={{ fontSize: '1rem', fontWeight: 700, color: '#1a1d23' }}>
            ML Kit Quality Metrics
          </Typography>
        </Box>

        {/* Stats Grid */}
        <Grid container spacing={2}>
          <StatItem
            label="Avg Confidence"
            value={`${stats.avgConfidence}%`}
            color="#E91E63"
            subtext="Overall quality score"
          />
          <StatItem
            label="High Quality (≥80%)"
            value={stats.highQuality}
            color="#4CAF50"
            subtext={`${highQualityPercent}% of total`}
          />
          <StatItem
            label="Medium Quality (50-80%)"
            value={stats.mediumQuality}
            color="#FF9800"
            subtext="Needs review"
          />
          <StatItem
            label="Low Quality (<50%)"
            value={stats.lowQuality}
            color="#F44336"
            subtext="Recommend rejection"
          />
          <StatItem
            label="Valid Faces"
            value={stats.validFaces}
            color="#8BC34A"
            subtext={`${validFacePercent}% passed validation`}
          />
          <StatItem
            label="Invalid Faces"
            value={stats.invalidFaces}
            color="#FF5722"
            subtext="Failed validation"
          />
        </Grid>

        {/* Summary */}
        <Box
          sx={{
            background: '#fff',
            border: '1px solid #eef0f4',
            borderRadius: '8px',
            p: '12px',
            mt: '16px',
          }}
        >
          <Typography sx={{ fontSize: '0.75rem', color: '#8b919e', fontWeight: 600, mb: '6px' }}>
            Summary
          </Typography>
          <Typography sx={{ fontSize: '0.8rem', color: '#64748b' }}>
            {stats.totalVerifications} total verifications • {stats.highQualityPercent}% high quality •{' '}
            {validFacePercent}% valid faces
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};

const StatItem = ({ label, value, color, subtext }) => (
  <Grid item xs={6} sm={4} md={2}>
    <Box sx={{ textAlign: 'center' }}>
      <Typography sx={{ fontSize: '0.7rem', color: '#8b919e', mb: '6px', fontWeight: 600 }}>
        {label}
      </Typography>
      <Typography sx={{ fontSize: '1.3rem', fontWeight: 700, color }}>
        {value}
      </Typography>
      {subtext && (
        <Typography sx={{ fontSize: '0.65rem', color: '#8b919e', mt: '4px' }}>
          {subtext}
        </Typography>
      )}
    </Box>
  </Grid>
);

export default MLKitStatsPanel;
