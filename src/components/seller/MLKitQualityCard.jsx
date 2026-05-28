// src/components/seller/MLKitQualityCard.jsx
// ✅ PRODUCTION-READY: ML Kit face quality assessment visualization
import { Box, Typography, LinearProgress, Chip, Tooltip } from '@mui/material';
import {
  Psychology as PsychologyIcon,
  CheckCircle as CheckCircleIcon,
  Warning as WarningIcon,
  Error as ErrorIcon,
} from '@mui/icons-material';

const MLKitQualityCard = ({ mlKitResult }) => {
  if (!mlKitResult) return null;

  const getQualityLevel = (confidence) => {
    if (confidence >= 85) {
      return {
        level: 'Excellent',
        color: '#4CAF50',
        icon: CheckCircleIcon,
        bgColor: '#E8F5E9',
        borderColor: '#A5D6A7',
      };
    }
    if (confidence >= 70) {
      return {
        level: 'Good',
        color: '#8BC34A',
        icon: CheckCircleIcon,
        bgColor: '#F1F8E9',
        borderColor: '#C5E1A5',
      };
    }
    if (confidence >= 50) {
      return {
        level: 'Fair',
        color: '#FF9800',
        icon: WarningIcon,
        bgColor: '#FFF3E0',
        borderColor: '#FFCC80',
      };
    }
    return {
      level: 'Poor',
      color: '#F44336',
      icon: ErrorIcon,
      bgColor: '#FFEBEE',
      borderColor: '#EF9A9A',
    };
  };

  const quality = getQualityLevel(mlKitResult.confidence);
  const QualityIcon = quality.icon;

  return (
    <Box
      sx={{
        background: quality.bgColor,
        border: `2px solid ${quality.borderColor}`,
        borderRadius: '12px',
        p: '16px',
        mb: '16px',
      }}
    >
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: '10px', mb: '14px' }}>
        <PsychologyIcon sx={{ fontSize: 20, color: '#E91E63' }} />
        <Typography sx={{ fontSize: '0.9rem', fontWeight: 700, color: '#1a1d23' }}>
          ML Kit Face Quality Assessment
        </Typography>
      </Box>

      {/* Quality Level */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px', mb: '12px' }}>
        <QualityIcon sx={{ fontSize: 18, color: quality.color }} />
        <Typography sx={{ fontSize: '0.85rem', fontWeight: 700, color: quality.color }}>
          {quality.level} Quality
        </Typography>
        <Chip
          label={`${mlKitResult.confidence.toFixed(1)}%`}
          size="small"
          sx={{
            fontSize: '0.7rem',
            fontWeight: 700,
            background: quality.color,
            color: '#fff',
            height: '22px',
          }}
        />
      </Box>

      {/* Confidence Progress Bar */}
      <Box sx={{ mb: '12px' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: '4px' }}>
          <Typography sx={{ fontSize: '0.7rem', color: '#8b919e', fontWeight: 600 }}>
            Confidence Score
          </Typography>
          <Typography sx={{ fontSize: '0.7rem', color: '#8b919e', fontWeight: 600 }}>
            {mlKitResult.confidence.toFixed(1)}%
          </Typography>
        </Box>
        <LinearProgress
          variant="determinate"
          value={mlKitResult.confidence}
          sx={{
            height: 8,
            borderRadius: '4px',
            background: '#e0e0e0',
            '& .MuiLinearProgress-bar': {
              background: quality.color,
              borderRadius: '4px',
            },
          }}
        />
      </Box>

      {/* Face Detection Metrics */}
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '10px', mb: '12px' }}>
        <MetricBox label="Faces Detected" value={mlKitResult.faceCount} />
        <MetricBox label="Validation" value={mlKitResult.isValid ? 'Valid' : 'Invalid'} />
        <MetricBox
          label="Status"
          value={mlKitResult.isValid ? '✓ Pass' : '✗ Fail'}
          color={mlKitResult.isValid ? '#4CAF50' : '#F44336'}
        />
      </Box>

      {/* ML Kit Message */}
      {mlKitResult.message && (
        <Box
          sx={{
            background: '#fff',
            border: '1px solid #eef0f4',
            borderRadius: '8px',
            p: '10px',
            mt: '12px',
          }}
        >
          <Typography sx={{ fontSize: '0.72rem', color: '#64748b', fontStyle: 'italic' }}>
            "{mlKitResult.message}"
          </Typography>
        </Box>
      )}

      {/* Recommendation */}
      <Box
        sx={{
          background: quality.color + '15',
          border: `1px solid ${quality.color}`,
          borderRadius: '8px',
          p: '10px',
          mt: '12px',
        }}
      >
        <Typography sx={{ fontSize: '0.72rem', color: quality.color, fontWeight: 600 }}>
          {getRecommendation(mlKitResult.confidence, mlKitResult.isValid)}
        </Typography>
      </Box>
    </Box>
  );
};

const MetricBox = ({ label, value, color = '#1a1d23' }) => (
  <Box sx={{ textAlign: 'center' }}>
    <Typography sx={{ fontSize: '0.68rem', color: '#8b919e', mb: '4px', fontWeight: 600 }}>
      {label}
    </Typography>
    <Typography sx={{ fontSize: '0.85rem', fontWeight: 700, color }}>
      {value}
    </Typography>
  </Box>
);

const getRecommendation = (confidence, isValid) => {
  if (!isValid) return '⚠️ Face validation failed. Request resubmission.';
  if (confidence >= 85) return '✅ Excellent quality. Safe to approve.';
  if (confidence >= 70) return '✅ Good quality. Can approve.';
  if (confidence >= 50) return '⚠️ Fair quality. Review carefully before approving.';
  return '❌ Poor quality. Recommend rejection.';
};

export default MLKitQualityCard;
