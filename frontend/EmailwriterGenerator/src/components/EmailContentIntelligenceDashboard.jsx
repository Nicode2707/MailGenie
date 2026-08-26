import React, { useState, useCallback } from 'react';
import {
  Box, Typography, CircularProgress, Card, CardContent, Grid,
  Paper, Button, TextField, Divider, Chip, Snackbar, Alert,
  Tabs, Tab, LinearProgress, Tooltip
} from '@mui/material';

/**
 * EmailContentIntelligenceDashboard
 *
 * Analyzes email content for CTA effectiveness, category classification,
 * engagement prediction, and provides signature generation and placeholder
 * auto-fill tools. Connects to EmailContentIntelligenceController.
 */
const EmailContentIntelligenceDashboard = ({ backendUrl = 'http://localhost:8080' }) => {
  const [content, setContent] = useState('');
  const [subject, setSubject] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState(0);
  const [toast, setToast] = useState({ open: false, message: '', severity: 'info' });

  // Signature state
  const [sigBody, setSigBody] = useState('');
  const [sigName, setSigName] = useState('');
  const [sigTitle, setSigTitle] = useState('');
  const [sigCompany, setSigCompany] = useState('');
  const [sigResult, setSigResult] = useState('');
  const [sigLoading, setSigLoading] = useState(false);

  // Placeholder state
  const [phTemplate, setPhTemplate] = useState('');
  const [phValues, setPhValues] = useState('');
  const [phResult, setPhResult] = useState('');
  const [phLoading, setPhLoading] = useState(false);

  const api = useCallback(async (method, path, body = null) => {
    const opts = { method, headers: { 'Content-Type': 'application/json' } };
    if (body) opts.body = JSON.stringify(body);
    const res = await fetch(`${backendUrl}${path}`, opts);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const text = await res.text();
    return text ? JSON.parse(text) : {};
  }, [backendUrl]);

  const analyze = useCallback(async () => {
    if (!content.trim()) return;
    setLoading(true); setResult(null);
    try {
      const data = await api('POST', '/api/email/intelligence/analyze', { content, subject });
      setResult(data);
      setToast({ open: true, message: 'Analysis complete!', severity: 'success' });
    } catch (err) {
      setToast({ open: true, message: 'Analysis failed: ' + err.message, severity: 'error' });
    } finally { setLoading(false); }
  }, [api, content, subject]);

  const generateSignature = async () => {
    setSigLoading(true);
    try {
      const data = await api('POST', '/api/email/intelligence/signature', {
        body: sigBody, senderName: sigName, senderTitle: sigTitle, company: sigCompany
      });
      setSigResult(data.bodyWithSignature);
      setToast({ open: true, message: 'Signature generated!', severity: 'success' });
    } catch { setToast({ open: true, message: 'Signature generation failed', severity: 'error' }); }
    finally { setSigLoading(false); }
  };

  const fillPlaceholders = async () => {
    setPhLoading(true);
    try {
      const valuesMap = JSON.parse(phValues || '{}');
      const data = await api('POST', '/api/email/intelligence/fill-placeholders', { template: phTemplate, values: valuesMap });
      setPhResult(data.filledContent);
      setToast({ open: true, message: 'Placeholders filled!', severity: 'success' });
    } catch { setToast({ open: true, message: 'Fill failed. Check JSON format.', severity: 'error' }); }
    finally { setPhLoading(false); }
  };

  const glassCard = { background: 'rgba(255,255,255,0.05)', backdropFilter: 'blur(12px)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '16px', color: '#e0e0e0', padding: '24px' };
  const scoreColor = (s) => s >= 80 ? '#34d399' : s >= 60 ? '#fbbf24' : s >= 40 ? '#fb923c' : '#f87171';

  const MetricCard = ({ label, value, color = '#818cf8', sublabel }) => (
    <Card sx={{ ...glassCard, textAlign: 'center', py: 3 }}>
      <Typography variant="h3" sx={{ fontWeight: 800, color }}>{value}</Typography>
      <Typography variant="caption" sx={{ color: '#94a3b8', fontWeight: 600 }}>{label}</Typography>
      {sublabel && <Typography variant="caption" sx={{ color: '#64748b', display: 'block', mt: 0.5 }}>{sublabel}</Typography>}
    </Card>
  );

  return (
    <Box sx={{ minHeight: '100vh' }}>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h3" sx={{
          fontWeight: 800, mb: 1,
          background: 'linear-gradient(135deg, #06b6d4 0%, #8b5cf6 100%)',
          WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
        }}>
          🧠 Content Intelligence
        </Typography>
        <Typography variant="body1" sx={{ color: '#94a3b8' }}>
          Deep analysis of email content — CTA detection, category classification, engagement prediction, and more.
        </Typography>
      </Box>

      {/* Tabs */}
      <Paper sx={{ ...glassCard, p: 0, mb: 3 }}>
        <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)} variant="scrollable" scrollButtons="auto"
          sx={{ borderBottom: '1px solid rgba(255,255,255,0.08)', '& .MuiTab-root': { color: '#94a3b8', fontWeight: 600, textTransform: 'none', minHeight: 52 }, '& .Mui-selected': { color: '#06b6d4 !important' } }}>
          <Tab label="🔬 Content Analysis" />
          <Tab label="✍️ Signature Builder" />
          <Tab label="🏷️ Placeholder Fill" />
        </Tabs>
      </Paper>

      {/* ═══ TAB 0: Content Analysis ═══ */}
      {activeTab === 0 && (
        <Box>
          <Grid container spacing={3}>
            <Grid item xs={12} md={5}>
              <Paper sx={glassCard}>
                <Typography variant="h6" sx={{ fontWeight: 700, color: '#06b6d4', mb: 2 }}>📝 Email Input</Typography>
                <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)', mb: 2 }} />
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  <TextField fullWidth size="small" label="Subject Line" placeholder="e.g. Quick follow-up on proposal"
                    value={subject} onChange={(e) => setSubject(e.target.value)}
                    sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2, color: '#e0e0e0' } }} />
                  <TextField fullWidth multiline rows={8} label="Email Body Content"
                    placeholder="Paste your email content here..."
                    value={content} onChange={(e) => setContent(e.target.value)}
                    sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2, color: '#e0e0e0' } }} />
                  <Button fullWidth variant="contained" onClick={analyze} disabled={loading || !content.trim()}
                    sx={{ py: 1.5, borderRadius: 2, fontWeight: 700, textTransform: 'none', background: 'linear-gradient(135deg, #06b6d4 0%, #8b5cf6 100%)', '&:hover': { filter: 'brightness(1.1)' } }}>
                    {loading ? <CircularProgress size={22} color="inherit" /> : '🔬 Run Intelligence Analysis'}
                  </Button>
                </Box>
              </Paper>
            </Grid>

            <Grid item xs={12} md={7}>
              {!result ? (
                <Paper sx={{ ...glassCard, display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
                  <Typography variant="h1" sx={{ fontSize: '3rem', mb: 2 }}>🧠</Typography>
                  <Typography variant="h6" sx={{ color: '#64748b', fontWeight: 600, textAlign: 'center' }}>
                    Enter email content and click "Run Intelligence Analysis" to get detailed insights.
                  </Typography>
                </Paper>
              ) : (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                  {/* Intelligence Score */}
                  <Paper sx={glassCard}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 4, flexWrap: 'wrap', justifyContent: 'center' }}>
                      <Box sx={{ width: 120, height: 120, borderRadius: '50%', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', background: `conic-gradient(${scoreColor(result.intelligenceScore)} ${result.intelligenceScore * 3.6}deg, rgba(255,255,255,0.06) 0deg)`, position: 'relative' }}>
                        <Box sx={{ width: 100, height: 100, borderRadius: '50%', background: '#090d16', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', position: 'absolute' }}>
                          <Typography variant="h3" sx={{ fontWeight: 800, color: scoreColor(result.intelligenceScore) }}>{result.intelligenceScore}</Typography>
                          <Typography variant="caption" sx={{ color: '#94a3b8' }}>/ 100</Typography>
                        </Box>
                      </Box>
                      <Box>
                        <Typography variant="h5" sx={{ fontWeight: 700, color: scoreColor(result.intelligenceScore) }}>{result.intelligenceGrade}</Typography>
                        <Typography variant="body2" sx={{ color: '#94a3b8', mt: 0.5 }}>Combined content intelligence score.</Typography>
                      </Box>
                    </Box>
                  </Paper>

                  {/* CTA Analysis */}
                  <Paper sx={glassCard}>
                    <Typography variant="h6" sx={{ fontWeight: 700, color: '#06b6d4', mb: 1 }}>🎯 Call-To-Action Analysis</Typography>
                    <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)', mb: 2 }} />
                    <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap' }}>
                      <Chip label={result.ctaAnalysis.hasClearCTA ? '✅ CTA Detected' : '⚠️ No CTA Found'} size="small"
                        sx={{ fontWeight: 700, bgcolor: result.ctaAnalysis.hasClearCTA ? 'rgba(52,211,153,0.15)' : 'rgba(251,191,36,0.15)', color: result.ctaAnalysis.hasClearCTA ? '#34d399' : '#fbbf24', border: `1px solid ${result.ctaAnalysis.hasClearCTA ? 'rgba(52,211,153,0.3)' : 'rgba(251,191,36,0.3)'}` }} />
                    </Box>
                    {result.ctaAnalysis.detectedActionVerbs?.length > 0 && (
                      <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
                        {result.ctaAnalysis.detectedActionVerbs.map((verb, i) => (
                          <Chip key={i} label={verb} size="small" sx={{ bgcolor: 'rgba(6,182,212,0.12)', color: '#06b6d4', fontWeight: 600 }} />
                        ))}
                      </Box>
                    )}
                    <Typography variant="body2" sx={{ color: '#94a3b8', fontSize: '0.85rem' }}>{result.ctaAnalysis.ctaRecommendation}</Typography>
                  </Paper>

                  {/* Category Classification */}
                  <Paper sx={glassCard}>
                    <Typography variant="h6" sx={{ fontWeight: 700, color: '#8b5cf6', mb: 1 }}>📂 Category Classification</Typography>
                    <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)', mb: 2 }} />
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                      <Typography variant="h5" sx={{ fontWeight: 700, color: '#8b5cf6' }}>{result.categoryClassification.detectedCategory}</Typography>
                      <Chip label={`${Math.round(result.categoryClassification.confidenceScore * 100)}% confidence`} size="small"
                        sx={{ bgcolor: 'rgba(139,92,246,0.12)', color: '#8b5cf6', fontWeight: 600 }} />
                    </Box>
                    <LinearProgress variant="determinate" value={result.categoryClassification.confidenceScore * 100}
                      sx={{ height: 8, borderRadius: 4, bgcolor: 'rgba(139,92,246,0.1)', '& .MuiLinearProgress-bar': { background: 'linear-gradient(90deg, #8b5cf6, #c084fc)', borderRadius: 4 } }} />
                  </Paper>

                  {/* Engagement Prediction */}
                  <Paper sx={glassCard}>
                    <Typography variant="h6" sx={{ fontWeight: 700, color: '#34d399', mb: 1 }}>📈 Engagement Prediction</Typography>
                    <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)', mb: 2 }} />
                    <Grid container spacing={2}>
                      <Grid item xs={6}>
                        <Box sx={{ textAlign: 'center', p: 2, borderRadius: 2, bgcolor: 'rgba(52,211,153,0.06)' }}>
                          <Typography variant="h4" sx={{ fontWeight: 800, color: '#34d399' }}>{result.engagementPrediction.predictedOpenRate}</Typography>
                          <Typography variant="caption" sx={{ color: '#94a3b8' }}>Predicted Open Rate</Typography>
                        </Box>
                      </Grid>
                      <Grid item xs={6}>
                        <Box sx={{ textAlign: 'center', p: 2, borderRadius: 2, bgcolor: 'rgba(52,211,153,0.06)' }}>
                          <Typography variant="h4" sx={{ fontWeight: 800, color: '#06b6d4' }}>{result.engagementPrediction.predictedResponseRate}</Typography>
                          <Typography variant="caption" sx={{ color: '#94a3b8' }}>Predicted Response Rate</Typography>
                        </Box>
                      </Grid>
                    </Grid>
                  </Paper>
                </Box>
              )}
            </Grid>
          </Grid>
        </Box>
      )}

      {/* ═══ TAB 1: Signature Builder ═══ */}
      {activeTab === 1 && (
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Paper sx={glassCard}>
              <Typography variant="h6" sx={{ fontWeight: 700, color: '#06b6d4', mb: 2 }}>✍️ Signature Builder</Typography>
              <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)', mb: 2 }} />
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <TextField fullWidth size="small" label="Email Body" multiline rows={4} value={sigBody} onChange={(e) => setSigBody(e.target.value)} placeholder="Your email body text..." sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2, color: '#e0e0e0' } }} />
                <TextField fullWidth size="small" label="Full Name" value={sigName} onChange={(e) => setSigName(e.target.value)} placeholder="e.g. Jane Smith" sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2, color: '#e0e0e0' } }} />
                <TextField fullWidth size="small" label="Job Title" value={sigTitle} onChange={(e) => setSigTitle(e.target.value)} placeholder="e.g. Senior Engineer" sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2, color: '#e0e0e0' } }} />
                <TextField fullWidth size="small" label="Company" value={sigCompany} onChange={(e) => setSigCompany(e.target.value)} placeholder="e.g. Acme Corp" sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2, color: '#e0e0e0' } }} />
                <Button fullWidth variant="contained" onClick={generateSignature} disabled={sigLoading}
                  sx={{ py: 1.5, borderRadius: 2, fontWeight: 700, textTransform: 'none', background: 'linear-gradient(135deg, #06b6d4 0%, #8b5cf6 100%)' }}>
                  {sigLoading ? <CircularProgress size={20} color="inherit" /> : '✍️ Generate Signature'}
                </Button>
              </Box>
            </Paper>
          </Grid>
          <Grid item xs={12} md={6}>
            <Paper sx={{ ...glassCard, minHeight: 300 }}>
              <Typography variant="h6" sx={{ fontWeight: 700, color: '#8b5cf6', mb: 2 }}>📄 Preview</Typography>
              <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)', mb: 2 }} />
              {sigResult ? (
                <Paper sx={{ p: 3, bgcolor: 'rgba(0,0,0,0.2)', borderRadius: 2, whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: '0.85rem' }}>{sigResult}</Paper>
              ) : (
                <Typography variant="body2" sx={{ color: '#64748b', fontStyle: 'italic', textAlign: 'center', mt: 6 }}>Fill in the fields and click "Generate Signature" to preview.</Typography>
              )}
            </Paper>
          </Grid>
        </Grid>
      )}

      {/* ═══ TAB 2: Placeholder Fill ═══ */}
      {activeTab === 2 && (
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Paper sx={glassCard}>
              <Typography variant="h6" sx={{ fontWeight: 700, color: '#fbbf24', mb: 2 }}>🏷️ Placeholder Auto-Fill</Typography>
              <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)', mb: 2 }} />
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <TextField fullWidth multiline rows={4} size="small" label="Template Text" placeholder='Hi {{Name}},\n\nYour order {{OrderId}} is ready.'
                  value={phTemplate} onChange={(e) => setPhTemplate(e.target.value)} sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2, color: '#e0e0e0' } }} />
                <TextField fullWidth size="small" label="Values (JSON)" placeholder='{"Name": "John", "OrderId": "12345"}'
                  value={phValues} onChange={(e) => setPhValues(e.target.value)} sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2, color: '#e0e0e0' } }} />
                <Button fullWidth variant="contained" onClick={fillPlaceholders} disabled={phLoading}
                  sx={{ py: 1.5, borderRadius: 2, fontWeight: 700, textTransform: 'none', background: 'linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%)' }}>
                  {phLoading ? <CircularProgress size={20} color="inherit" /> : '🏷️ Fill Placeholders'}
                </Button>
              </Box>
            </Paper>
          </Grid>
          <Grid item xs={12} md={6}>
            <Paper sx={{ ...glassCard, minHeight: 300 }}>
              <Typography variant="h6" sx={{ fontWeight: 700, color: '#fbbf24', mb: 2 }}>📄 Filled Result</Typography>
              <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)', mb: 2 }} />
              {phResult ? (
                <Paper sx={{ p: 3, bgcolor: 'rgba(0,0,0,0.2)', borderRadius: 2, whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: '0.85rem' }}>{phResult}</Paper>
              ) : (
                <Typography variant="body2" sx={{ color: '#64748b', fontStyle: 'italic', textAlign: 'center', mt: 6 }}>Enter a template with {{placeholder}} tags and values JSON.</Typography>
              )}
            </Paper>
          </Grid>
        </Grid>
      )}

      {/* Toast */}
      <Snackbar open={toast.open} autoHideDuration={4000} onClose={() => setToast(p => ({ ...p, open: false }))} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert onClose={() => setToast(p => ({ ...p, open: false }))} severity={toast.severity} variant="filled" sx={{ borderRadius: 3, fontWeight: 600 }}>{toast.message}</Alert>
      </Snackbar>
    </Box>
  );
};

export default EmailContentIntelligenceDashboard;
