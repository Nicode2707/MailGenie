import React, { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, CircularProgress, Card, CardContent, Grid,
  Paper, Button, TextField, Divider, Chip, IconButton, Tooltip,
  Dialog, DialogTitle, DialogContent, DialogActions, Snackbar,
  Alert, FormControl, InputLabel, Select, MenuItem
} from '@mui/material';

/**
 * EmailABTestDashboard — Compare email variants with automated quality scoring.
 * Connects to EmailABTestController on the Spring Boot backend.
 */
const EmailABTestDashboard = ({ backendUrl = 'http://localhost:8080' }) => {
  const [campaigns, setCampaigns] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [selectedCampaign, setSelectedCampaign] = useState(null);
  const [toast, setToast] = useState({ open: false, message: '', severity: 'info' });
  const [campaignName, setCampaignName] = useState('');
  const [testType, setTestType] = useState('full_email');
  const [variants, setVariants] = useState([
    { subjectLine: '', bodyContent: '' },
    { subjectLine: '', bodyContent: '' },
  ]);

  const api = useCallback(async (method, path, body = null) => {
    const opts = { method, headers: { 'Content-Type': 'application/json' } };
    if (body) opts.body = JSON.stringify(body);
    const res = await fetch(`${backendUrl}${path}`, opts);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const text = await res.text();
    return text ? JSON.parse(text) : {};
  }, [backendUrl]);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [c, s] = await Promise.all([api('GET', '/api/ab-test/campaigns'), api('GET', '/api/ab-test/stats')]);
      setCampaigns(c);
      setStats(s);
    } catch (err) {
      setToast({ open: true, message: 'Failed to load A/B test data', severity: 'error' });
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => { loadData(); }, [loadData]);

  const updateVariant = (i, field, val) => setVariants(prev => prev.map((v, idx) => idx === i ? { ...v, [field]: val } : v));
  const addVariant = () => { if (variants.length < 6) setVariants(prev => [...prev, { subjectLine: '', bodyContent: '' }]); };
  const removeVariant = (i) => { if (variants.length > 2) setVariants(prev => prev.filter((_, idx) => idx !== i)); };

  const handleCreateCampaign = async () => {
    if (!campaignName.trim()) return;
    const valid = variants.filter(v => v.bodyContent.trim());
    if (valid.length < 2) { setToast({ open: true, message: 'At least 2 variants required', severity: 'warning' }); return; }
    setSubmitting(true);
    try {
      const result = await api('POST', '/api/ab-test/campaigns', { campaignName, testType, variants: valid });
      setCreateDialogOpen(false); setCampaignName('');
      setVariants([{ subjectLine: '', bodyContent: '' }, { subjectLine: '', bodyContent: '' }]);
      setSelectedCampaign(result); setDetailOpen(true); loadData();
      setToast({ open: true, message: `Campaign "${result.campaignName}" created!`, severity: 'success' });
    } catch (err) {
      setToast({ open: true, message: 'Failed to create campaign', severity: 'error' });
    } finally { setSubmitting(false); }
  };

  const handleDeleteCampaign = async (id) => {
    try { await api('DELETE', `/api/ab-test/campaigns/${id}`); loadData(); setToast({ open: true, message: 'Deleted', severity: 'info' }); }
    catch { setToast({ open: true, message: 'Delete failed', severity: 'error' }); }
  };

  const glassCard = { background: 'rgba(255,255,255,0.05)', backdropFilter: 'blur(12px)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '16px', color: '#e0e0e0', padding: '24px' };
  const scoreColor = (s) => s >= 80 ? '#34d399' : s >= 60 ? '#fbbf24' : s >= 40 ? '#fb923c' : '#f87171';
  const verdictStyle = (v) => v === 'WINNER' ? { bgcolor: 'rgba(52,211,153,0.15)', color: '#34d399', border: '1px solid rgba(52,211,153,0.3)' }
    : v === 'RUNNER_UP' ? { bgcolor: 'rgba(129,140,248,0.15)', color: '#818cf8', border: '1px solid rgba(129,140,248,0.3)' }
    : { bgcolor: 'rgba(248,113,113,0.15)', color: '#f87171', border: '1px solid rgba(248,113,113,0.3)' };
  const testTypeLabel = (t) => t === 'subject_line' ? '📝 Subject' : t === 'body_content' ? '📄 Body' : '📧 Full Email';

  if (loading) return <Box display="flex" justifyContent="center" mt={10}><CircularProgress sx={{ color: '#818cf8' }} /></Box>;

  const VariantDetailCard = ({ v }) => (
    <Card sx={{ ...glassCard, mb: 2, border: v.verdict === 'WINNER' ? '2px solid rgba(52,211,153,0.4)' : '1px solid rgba(255,255,255,0.1)' }}>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <Typography variant="h5" sx={{ fontWeight: 800, color: '#818cf8' }}>Variant {v.label}</Typography>
            {v.verdict && <Chip label={v.verdict} size="small" sx={{ ...verdictStyle(v.verdict), fontWeight: 700 }} />}
          </Box>
          <Box sx={{ textAlign: 'right' }}>
            <Typography variant="h4" sx={{ fontWeight: 800, color: scoreColor(v.compositeScore) }}>{v.compositeScore}</Typography>
            <Typography variant="caption" sx={{ color: '#94a3b8' }}>/100</Typography>
          </Box>
        </Box>
        {v.subjectLine && <Typography variant="body2" sx={{ color: '#fbbf24', mb: 1 }}>Subject: {v.subjectLine}</Typography>}
        <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)', my: 1.5 }} />
        <Grid container spacing={1.5}>
          {[
            { label: 'Readability', value: v.readabilityMetrics?.fleschKincaidScore ?? '—' },
            { label: 'Spam Score', value: v.spamMetrics?.spamScore ?? '—' },
            { label: 'Subject Score', value: v.subjectMetrics?.subjectScore ?? '—' },
            { label: 'Tone', value: v.toneMetrics?.tone ?? '—' },
          ].map((m, i) => (
            <Grid item xs={6} sm={3} key={i}>
              <Box sx={{ textAlign: 'center', p: 1, borderRadius: 2, bgcolor: 'rgba(255,255,255,0.03)' }}>
                <Typography variant="h6" sx={{ fontWeight: 700, color: typeof m.value === 'number' ? scoreColor(m.value) : '#e0e0e0' }}>{m.value}</Typography>
                <Typography variant="caption" sx={{ color: '#94a3b8' }}>{m.label}</Typography>
              </Box>
            </Grid>
          ))}
        </Grid>
      </CardContent>
    </Card>
  );

  return (
    <Box sx={{ minHeight: '100vh' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4, flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h3" sx={{ fontWeight: 800, mb: 1, background: 'linear-gradient(135deg, #f87171 0%, #fbbf24 100%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
            ⚡ A/B Split Test Dashboard
          </Typography>
          <Typography variant="body1" sx={{ color: '#94a3b8' }}>Compare email variants side-by-side with automated quality scoring.</Typography>
        </Box>
        <Button variant="contained" onClick={() => setCreateDialogOpen(true)}
          sx={{ py: 1.4, px: 3, borderRadius: 2, fontWeight: 700, textTransform: 'none', background: 'linear-gradient(135deg, #f87171 0%, #fbbf24 100%)', '&:hover': { filter: 'brightness(1.1)' } }}>
          ➕ New A/B Test
        </Button>
      </Box>

      {stats && stats.totalCampaigns > 0 && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          {[{ label: 'Campaigns', value: stats.totalCampaigns, color: '#f87171' },
            { label: 'Variants Tested', value: stats.totalVariantsAnalyzed, color: '#fbbf24' },
            { label: 'Avg Score', value: stats.averageCompositeScore, color: '#34d399' }
          ].map((s, i) => (
            <Grid item xs={12} sm={4} key={i}>
              <Card sx={{ ...glassCard, textAlign: 'center', py: 3 }}>
                <Typography variant="h3" sx={{ fontWeight: 800, color: s.color }}>{s.value}</Typography>
                <Typography variant="caption" sx={{ color: '#94a3b8', fontWeight: 600 }}>{s.label}</Typography>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <Paper sx={glassCard}>
        <Typography variant="h6" sx={{ fontWeight: 700, color: '#fbbf24', mb: 2 }}>📋 Campaign History</Typography>
        <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)', mb: 2 }} />
        {campaigns.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 8 }}>
            <Typography variant="body1" sx={{ color: '#64748b', fontStyle: 'italic', mb: 2 }}>No campaigns yet. Click "New A/B Test" to get started.</Typography>
          </Box>
        ) : (
          <Grid container spacing={2}>
            {campaigns.map((c) => {
              const winner = (c.variants || []).find(v => v.verdict === 'WINNER');
              return (
                <Grid item xs={12} sm={6} md={4} key={c.campaignId}>
                  <Card sx={{ ...glassCard, cursor: 'pointer', '&:hover': { borderColor: 'rgba(248,113,113,0.3)' } }}
                    onClick={() => { setSelectedCampaign(c); setDetailOpen(true); }}>
                    <CardContent>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                        <Typography variant="subtitle1" sx={{ fontWeight: 700, color: '#e0e0e0' }}>{c.campaignName}</Typography>
                        <IconButton size="small" onClick={(e) => { e.stopPropagation(); handleDeleteCampaign(c.campaignId); }} sx={{ color: '#f87171' }}>🗑️</IconButton>
                      </Box>
                      <Box sx={{ display: 'flex', gap: 1, mb: 1.5, flexWrap: 'wrap' }}>
                        <Chip label={testTypeLabel(c.testType)} size="small" sx={{ bgcolor: 'rgba(248,113,113,0.12)', color: '#f87171', fontWeight: 600 }} />
                        <Chip label={`${c.variantCount} variants`} size="small" sx={{ bgcolor: 'rgba(129,140,248,0.12)', color: '#818cf8', fontWeight: 600 }} />
                      </Box>
                      {winner && (
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Chip label={`🏆 Variant ${winner.label}`} size="small" sx={{ bgcolor: 'rgba(52,211,153,0.15)', color: '#34d399', fontWeight: 700 }} />
                          <Typography variant="h6" sx={{ fontWeight: 800, color: scoreColor(winner.compositeScore) }}>{winner.compositeScore}</Typography>
                        </Box>
                      )}
                      <Typography variant="caption" sx={{ color: '#64748b', mt: 1, display: 'block' }}>{new Date(c.createdAt).toLocaleDateString()}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
              );
            })}
          </Grid>
        )}
      </Paper>

      {/* Create Dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle sx={{ fontWeight: 700 }}>➕ Create A/B Split Test</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth size="small" label="Campaign Name" placeholder="e.g. Q4 Outreach Test"
                value={campaignName} onChange={(e) => setCampaignName(e.target.value)} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth size="small">
                <InputLabel>Test Type</InputLabel>
                <Select value={testType} label="Test Type" onChange={(e) => setTestType(e.target.value)}>
                  <MenuItem value="full_email">📧 Full Email</MenuItem>
                  <MenuItem value="subject_line">📝 Subject Line</MenuItem>
                  <MenuItem value="body_content">📄 Body Content</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
          <Divider sx={{ my: 2 }} />
          {variants.map((v, idx) => (
            <Paper key={idx} sx={{ p: 2, mb: 2, bgcolor: 'rgba(99,102,241,0.04)', border: '1px solid rgba(99,102,241,0.12)', borderRadius: 2 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, color: '#818cf8' }}>Variant {String.fromCharCode(65 + idx)}</Typography>
                {variants.length > 2 && <IconButton size="small" onClick={() => removeVariant(idx)} sx={{ color: '#f87171' }}>✕</IconButton>}
              </Box>
              <TextField fullWidth size="small" label="Subject Line" value={v.subjectLine} onChange={(e) => updateVariant(idx, 'subjectLine', e.target.value)} sx={{ mb: 1.5 }} />
              <TextField fullWidth multiline rows={3} size="small" label="Email Body" value={v.bodyContent} onChange={(e) => updateVariant(idx, 'bodyContent', e.target.value)} />
            </Paper>
          ))}
          {variants.length < 6 && (
            <Button fullWidth variant="outlined" onClick={addVariant} sx={{ borderStyle: 'dashed', color: '#818cf8', borderColor: 'rgba(129,140,248,0.3)', borderRadius: 2, py: 1.2 }}>
              ➕ Add Variant ({6 - variants.length} remaining)
            </Button>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)} sx={{ color: '#94a3b8' }}>Cancel</Button>
          <Button variant="contained" onClick={handleCreateCampaign} disabled={submitting || !campaignName.trim()}
            sx={{ bgcolor: '#f87171', fontWeight: 700, textTransform: 'none' }}>
            {submitting ? <CircularProgress size={20} color="inherit" /> : '🚀 Launch Test'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Detail Dialog */}
      <Dialog open={detailOpen} onClose={() => setDetailOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle sx={{ fontWeight: 700 }}>📊 {selectedCampaign?.campaignName}</DialogTitle>
        <DialogContent>
          {selectedCampaign && (
            <Box>
              <Box sx={{ display: 'flex', gap: 1.5, mb: 2, flexWrap: 'wrap' }}>
                <Chip label={testTypeLabel(selectedCampaign.testType)} size="small" sx={{ bgcolor: 'rgba(248,113,113,0.12)', color: '#f87171', fontWeight: 600 }} />
                <Chip label={selectedCampaign.status} size="small" sx={{ bgcolor: 'rgba(52,211,153,0.12)', color: '#34d399', fontWeight: 600 }} />
              </Box>
              {(selectedCampaign.variants || []).map((v) => <VariantDetailCard key={v.variantId} v={v} />)}
            </Box>
          )}
        </DialogContent>
        <DialogActions><Button onClick={() => setDetailOpen(false)} sx={{ color: '#94a3b8' }}>Close</Button></DialogActions>
      </Dialog>

      <Snackbar open={toast.open} autoHideDuration={4000} onClose={() => setToast(p => ({ ...p, open: false }))} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert onClose={() => setToast(p => ({ ...p, open: false }))} severity={toast.severity} variant="filled" sx={{ borderRadius: 3, fontWeight: 600 }}>{toast.message}</Alert>
      </Snackbar>
    </Box>
  );
};

export default EmailABTestDashboard;
