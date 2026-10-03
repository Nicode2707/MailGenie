import React, { useState, useCallback } from 'react';
import {
  Box, Typography, CircularProgress, Card, CardContent, Grid,
  Paper, Button, TextField, Divider, LinearProgress, Chip,
  Snackbar, Alert, Tooltip, IconButton
} from '@mui/material';

/**
 * EmailQualityAnalyzerDashboard
 *
 * Analyzes email content for readability (Flesch-Kincaid), spam compliance
 * (trigger-word detection), and subject line optimization.
 * Connects to EmailQualityAnalyzerController on the Spring Boot backend.
 */
const EmailQualityAnalyzerDashboard = ({ backendUrl = 'http://localhost:8080' }) => {
  // ── State ─────────────────────────────────────────────────────
  const [content, setContent] = useState('');
  const [subjectLine, setSubjectLine] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState({ open: false, message: '', severity: 'info' });

  // ── API ───────────────────────────────────────────────────────
  const analyzeEmail = useCallback(async () => {
    if (!content.trim() && !subjectLine.trim()) return;
    setLoading(true);
    setResult(null);
    try {
      const res = await fetch(`${backendUrl}/api/email/quality/analyze`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content, subjectLine }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setResult(data);
      setToast({ open: true, message: 'Quality analysis complete!', severity: 'success' });
    } catch (err) {
      setToast({ open: true, message: 'Analysis failed: ' + err.message, severity: 'error' });
    } finally {
      setLoading(false);
    }
  }, [backendUrl, content, subjectLine]);

  // ── Helpers ───────────────────────────────────────────────────
  const scoreColor = (score) => {
    if (score >= 80) return '#34d399';
    if (score >= 60) return '#fbbf24';
    if (score >= 40) return '#fb923c';
    return '#f87171';
  };

  const scoreLabel = (score) => {
    if (score >= 80) return 'Excellent';
    if (score >= 60) return 'Good';
    if (score >= 40) return 'Fair';
    return 'Poor';
  };

  const wordCount = content.trim() ? content.trim().split(/\s+/).length : 0;
  const charCount = content.length;
  const sentenceCount = content.trim() ? content.split(/[.!?]+/).filter(s => s.trim()).length : 0;

  // ── Styling ───────────────────────────────────────────────────
  const glassCard = {
    background: 'rgba(255, 255, 255, 0.05)',
    backdropFilter: 'blur(12px)',
    border: '1px solid rgba(255, 255, 255, 0.1)',
    borderRadius: '16px',
    color: '#e0e0e0',
    padding: '24px',
  };

  const scoreRingStyle = (score, size = 120) => ({
    width: size, height: size, borderRadius: '50%',
    display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center',
    background: `conic-gradient(${scoreColor(score)} ${score * 3.6}deg, rgba(255,255,255,0.06) 0deg)`,
    position: 'relative',
  });

  const scoreRingInner = (size = 100) => ({
    width: size, height: size, borderRadius: '50%',
    background: '#090d16',
    display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center',
    position: 'absolute',
  });

  // ── Render ────────────────────────────────────────────────────
  return (
    <Box sx={{ minHeight: '100vh' }}>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h3" sx={{
          fontWeight: 800, mb: 1,
          background: 'linear-gradient(135deg, #34d399 0%, #06b6d4 100%)',
          WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
        }}>
          📊 Email Quality Analyzer
        </Typography>
        <Typography variant="body1" sx={{ color: '#94a3b8' }}>
          Analyze readability, spam compliance, and subject line effectiveness before sending.
        </Typography>
      </Box>

      <Grid container spacing={3}>
        {/* ── Input Panel ──────────────────────────────────────── */}
        <Grid item xs={12} md={5}>
          <Paper sx={glassCard}>
            <Typography variant="h6" sx={{ fontWeight: 700, color: '#34d399', mb: 2 }}>✍️ Compose & Analyze</Typography>
            <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)', mb: 2 }} />

            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
              <TextField
                fullWidth size="small" label="Subject Line (optional)"
                placeholder="e.g. Quick question about tomorrow's meeting"
                value={subjectLine}
                onChange={(e) => setSubjectLine(e.target.value)}
                sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2, color: '#e0e0e0' } }}
              />

              <TextField
                fullWidth multiline rows={8} label="Email Body Content"
                placeholder="Paste or type your email content here to analyze quality..."
                value={content}
                onChange={(e) => setContent(e.target.value)}
                sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2, color: '#e0e0e0' } }}
              />

              <Box sx={{ display: 'flex', justifyContent: 'space-between', px: 0.5 }}>
                <Typography variant="caption" sx={{ color: '#64748b', fontWeight: 600 }}>
                  {charCount} chars · {wordCount} words · {sentenceCount} sentences
                </Typography>
                <Typography variant="caption" sx={{ color: '#64748b', fontWeight: 600 }}>
                  Est. read: ~{Math.ceil(wordCount / 200)} min
                </Typography>
              </Box>

              <Button
                fullWidth variant="contained" onClick={analyzeEmail}
                disabled={loading || (!content.trim() && !subjectLine.trim())}
                sx={{
                  py: 1.6, borderRadius: 2, fontWeight: 700, textTransform: 'none',
                  background: 'linear-gradient(135deg, #34d399 0%, #06b6d4 100%)',
                  '&:hover': { filter: 'brightness(1.1)' },
                }}
              >
                {loading ? <CircularProgress size={22} color="inherit" /> : '🔍 Analyze Quality'}
              </Button>
            </Box>

            {/* Analysis Tips */}
            <Box sx={{ mt: 3, p: 2, borderRadius: 2, bgcolor: 'rgba(6,182,212,0.06)', border: '1px solid rgba(6,182,212,0.15)' }}>
              <Typography variant="body2" sx={{ color: '#06b6d4', fontWeight: 600, fontSize: '0.82rem' }}>
                💡 Tip: Paste your full email content and subject line for a comprehensive quality report with actionable suggestions.
              </Typography>
            </Box>
          </Paper>
        </Grid>

        {/* ── Results Panel ────────────────────────────────────── */}
        <Grid item xs={12} md={7}>
          {!result ? (
            <Paper sx={{ ...glassCard, display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
              <Typography variant="h1" sx={{ fontSize: '3rem', mb: 2 }}>📊</Typography>
              <Typography variant="h6" sx={{ color: '#64748b', fontWeight: 600, textAlign: 'center' }}>
                Enter email content on the left and click "Analyze Quality" to see detailed scoring.
              </Typography>
            </Paper>
          ) : (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
              {/* Overall Score Card */}
              <Paper sx={glassCard}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 4, flexWrap: 'wrap', justifyContent: 'center' }}>
                  <Box sx={scoreRingStyle(result.overallScore)}>
                    <Box sx={scoreRingInner()}>
                      <Typography variant="h3" sx={{ fontWeight: 800, color: scoreColor(result.overallScore) }}>
                        {result.overallScore}
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#94a3b8', fontWeight: 600 }}>/ 100</Typography>
                    </Box>
                  </Box>
                  <Box sx={{ textAlign: { xs: 'center', sm: 'left' } }}>
                    <Typography variant="h5" sx={{ fontWeight: 700, color: scoreColor(result.overallScore) }}>
                      {result.overallGrade}
                    </Typography>
                    <Typography variant="body2" sx={{ color: '#94a3b8', mt: 0.5 }}>
                      Combined quality score across readability, spam safety, and subject effectiveness.
                    </Typography>
                  </Box>
                </Box>
              </Paper>

              {/* Readability Card */}
              <Paper sx={glassCard}>
                <Typography variant="h6" sx={{ fontWeight: 700, color: '#818cf8', mb: 1 }}>📖 Readability Analysis</Typography>
                <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)', mb: 2 }} />
                <Grid container spacing={3}>
                  <Grid item xs={6} sm={3}>
                    <Box sx={{ textAlign: 'center' }}>
                      <Typography variant="h4" sx={{ fontWeight: 800, color: scoreColor(result.readability.fleschKincaidScore) }}>
                        {result.readability.fleschKincaidScore}
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#94a3b8' }}>Flesch Score</Typography>
                    </Box>
                  </Grid>
                  <Grid item xs={6} sm={3}>
                    <Box sx={{ textAlign: 'center' }}>
                      <Typography variant="h6" sx={{ fontWeight: 700, color: '#e0e0e0' }}>
                        {result.readability.readingGradeLevel}
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#94a3b8' }}>Grade Level</Typography>
                    </Box>
                  </Grid>
                  <Grid item xs={6} sm={3}>
                    <Box sx={{ textAlign: 'center' }}>
                      <Typography variant="h4" sx={{ fontWeight: 800, color: '#e0e0e0' }}>
                        {result.readability.sentenceCount}
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#94a3b8' }}>Sentences</Typography>
                    </Box>
                  </Grid>
                  <Grid item xs={6} sm={3}>
                    <Box sx={{ textAlign: 'center' }}>
                      <Typography variant="h4" sx={{ fontWeight: 800, color: '#e0e0e0' }}>
                        {result.readability.averageSentenceLength}
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#94a3b8' }}>Avg Words/Sent.</Typography>
                    </Box>
                  </Grid>
                </Grid>
                <Box sx={{ mt: 2 }}>
                  <LinearProgress
                    variant="determinate"
                    value={result.readability.fleschKincaidScore}
                    sx={{
                      height: 8, borderRadius: 4,
                      bgcolor: 'rgba(129,140,248,0.1)',
                      '& .MuiLinearProgress-bar': {
                        background: `linear-gradient(90deg, ${scoreColor(result.readability.fleschKincaidScore)}, ${scoreColor(result.readability.fleschKincaidScore)}aa)`,
                        borderRadius: 4,
                      },
                    }}
                  />
                </Box>
                <Typography variant="body2" sx={{ color: '#64748b', mt: 1, fontSize: '0.82rem' }}>
                  {result.readability.fleschKincaidScore >= 70
                    ? '✅ Your email is easy to read and understand.'
                    : result.readability.fleschKincaidScore >= 50
                    ? '⚠️ Consider simplifying sentences for better clarity.'
                    : '❌ Content is difficult to read. Use shorter sentences and simpler words.'}
                </Typography>
              </Paper>

              {/* Spam Compliance Card */}
              <Paper sx={glassCard}>
                <Typography variant="h6" sx={{ fontWeight: 700, color: '#f87171', mb: 1 }}>🛡️ Spam Compliance</Typography>
                <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)', mb: 2 }} />
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 3, mb: 2, flexWrap: 'wrap' }}>
                  <Box sx={{ textAlign: 'center', minWidth: 100 }}>
                    <Typography variant="h3" sx={{ fontWeight: 800, color: scoreColor(100 - result.spamCompliance.spamScore) }}>
                      {result.spamCompliance.spamScore}
                    </Typography>
                    <Typography variant="caption" sx={{ color: '#94a3b8' }}>Spam Score</Typography>
                  </Box>
                  <Chip
                    label={result.spamCompliance.isHighRiskSpam ? '⚠️ HIGH RISK' : '✅ LOW RISK'}
                    sx={{
                      fontWeight: 700, fontSize: '0.85rem',
                      bgcolor: result.spamCompliance.isHighRiskSpam ? 'rgba(248,113,113,0.15)' : 'rgba(52,211,153,0.15)',
                      color: result.spamCompliance.isHighRiskSpam ? '#f87171' : '#34d399',
                      border: `1px solid ${result.spamCompliance.isHighRiskSpam ? 'rgba(248,113,113,0.3)' : 'rgba(52,211,153,0.3)'}`,
                    }}
                  />
                </Box>

                {result.spamCompliance.flaggedTriggerWords && result.spamCompliance.flaggedTriggerWords.length > 0 && (
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="body2" sx={{ color: '#f87171', fontWeight: 600, mb: 1 }}>
                      Flagged Trigger Words:
                    </Typography>
                    <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                      {result.spamCompliance.flaggedTriggerWords.map((word, i) => (
                        <Chip
                          key={i} label={word} size="small"
                          sx={{ bgcolor: 'rgba(248,113,113,0.12)', color: '#f87171', fontWeight: 600, border: '1px solid rgba(248,113,113,0.2)' }}
                        />
                      ))}
                    </Box>
                  </Box>
                )}

                <Box sx={{ p: 2, borderRadius: 2, bgcolor: result.spamCompliance.isHighRiskSpam ? 'rgba(248,113,113,0.06)' : 'rgba(52,211,153,0.06)', border: `1px solid ${result.spamCompliance.isHighRiskSpam ? 'rgba(248,113,113,0.15)' : 'rgba(52,211,153,0.15)'}` }}>
                  <Typography variant="body2" sx={{ color: result.spamCompliance.isHighRiskSpam ? '#f87171' : '#34d399', fontWeight: 600, fontSize: '0.85rem' }}>
                    {result.spamCompliance.recommendation}
                  </Typography>
                </Box>
              </Paper>

              {/* Subject Line Card */}
              <Paper sx={glassCard}>
                <Typography variant="h6" sx={{ fontWeight: 700, color: '#fbbf24', mb: 1 }}>📝 Subject Line Optimization</Typography>
                <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)', mb: 2 }} />
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 3, mb: 2, flexWrap: 'wrap' }}>
                  <Box sx={{ textAlign: 'center', minWidth: 100 }}>
                    <Typography variant="h3" sx={{ fontWeight: 800, color: scoreColor(result.subjectLine.subjectScore) }}>
                      {result.subjectLine.subjectScore}
                    </Typography>
                    <Typography variant="caption" sx={{ color: '#94a3b8' }}>Subject Score</Typography>
                  </Box>
                  <Box>
                    <Typography variant="body2" sx={{ color: '#e0e0e0' }}>
                      {result.subjectLine.characterCount} chars · {result.subjectLine.wordCount} words
                    </Typography>
                  </Box>
                </Box>

                {result.subjectLine.suggestions && result.subjectLine.suggestions.length > 0 && (
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    {result.subjectLine.suggestions.map((sug, i) => (
                      <Box key={i} sx={{ p: 1.5, borderRadius: 2, bgcolor: 'rgba(251,191,36,0.06)', border: '1px solid rgba(251,191,36,0.15)' }}>
                        <Typography variant="body2" sx={{ color: '#fbbf24', fontWeight: 600, fontSize: '0.85rem' }}>
                          💡 {sug}
                        </Typography>
                      </Box>
                    ))}
                  </Box>
                )}

                {result.subjectLine.subjectScore >= 70 && result.subjectLine.suggestions.length === 0 && (
                  <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: 'rgba(52,211,153,0.06)', border: '1px solid rgba(52,211,153,0.15)' }}>
                    <Typography variant="body2" sx={{ color: '#34d399', fontWeight: 600, fontSize: '0.85rem' }}>
                      ✅ Your subject line looks great!
                    </Typography>
                  </Box>
                )}
              </Paper>
            </Box>
          )}
        </Grid>
      </Grid>

      {/* Toast */}
      <Snackbar
        open={toast.open} autoHideDuration={4000}
        onClose={() => setToast(prev => ({ ...prev, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert
          onClose={() => setToast(prev => ({ ...prev, open: false }))}
          severity={toast.severity} variant="filled"
          sx={{ borderRadius: 3, fontWeight: 600 }}
        >
          {toast.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default EmailQualityAnalyzerDashboard;
