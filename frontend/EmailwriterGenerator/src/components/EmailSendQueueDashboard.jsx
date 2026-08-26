import React, { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, CircularProgress, Card, CardContent, Grid,
  Paper, Button, TextField, Divider, Chip, IconButton, Tooltip,
  Dialog, DialogTitle, DialogContent, DialogActions, Snackbar,
  Alert, FormControl, InputLabel, Select, MenuItem
} from '@mui/material';

/**
 * EmailSendQueueDashboard — Queue emails for scheduled delivery with priority management.
 * Connects to EmailSendQueueController on the Spring Boot backend.
 */
const EmailSendQueueDashboard = ({ backendUrl = 'http://localhost:8080' }) => {
  const [items, setItems] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [enqueueOpen, setEnqueueOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [filterStatus, setFilterStatus] = useState('');
  const [toast, setToast] = useState({ open: false, message: '', severity: 'info' });
  const [form, setForm] = useState({ recipientEmail: '', recipientName: '', subjectLine: '', bodyContent: '', tone: 'professional', provider: 'groq', priority: 'NORMAL', scheduledFor: '' });

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
      const url = filterStatus ? `/api/send-queue?status=${filterStatus}` : '/api/send-queue';
      const [q, s] = await Promise.all([api('GET', url), api('GET', '/api/send-queue/stats')]);
      setItems(q); setStats(s);
    } catch { setToast({ open: true, message: 'Failed to load queue', severity: 'error' }); }
    finally { setLoading(false); }
  }, [api, filterStatus]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleEnqueue = async () => {
    if (!form.recipientEmail.trim() || !form.bodyContent.trim()) return;
    setSubmitting(true);
    try {
      await api('POST', '/api/send-queue/enqueue', form);
      setEnqueueOpen(false);
      setForm({ recipientEmail: '', recipientName: '', subjectLine: '', bodyContent: '', tone: 'professional', provider: 'groq', priority: 'NORMAL', scheduledFor: '' });
      loadData(); setToast({ open: true, message: 'Email queued!', severity: 'success' });
    } catch { setToast({ open: true, message: 'Enqueue failed', severity: 'error' }); }
    finally { setSubmitting(false); }
  };

  const handleProcess = async () => {
    setProcessing(true);
    try {
      const r = await api('POST', '/api/send-queue/process');
      loadData(); setToast({ open: true, message: `Processed ${r.processedCount} — ${r.sentCount} sent, ${r.failedCount} failed`, severity: 'success' });
    } catch { setToast({ open: true, message: 'Processing failed', severity: 'error' }); }
    finally { setProcessing(false); }
  };

  const handleCancel = async (id) => {
    try { await api('POST', `/api/send-queue/${id}/cancel`); loadData(); setToast({ open: true, message: 'Cancelled', severity: 'info' }); }
    catch { setToast({ open: true, message: 'Cancel failed', severity: 'error' }); }
  };

  const handleDelete = async (id) => {
    try { await api('DELETE', `/api/send-queue/${id}`); loadData(); setToast({ open: true, message: 'Deleted', severity: 'info' }); }
    catch { setToast({ open: true, message: 'Delete failed', severity: 'error' }); }
  };

  const glassCard = { background: 'rgba(255,255,255,0.05)', backdropFilter: 'blur(12px)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '16px', color: '#e0e0e0', padding: '24px' };
  const statusColor = (s) => ({ QUEUED: '#fbbf24', SENDING: '#818cf8', SENT: '#34d399', FAILED: '#f87171', CANCELLED: '#94a3b8' }[s] || '#94a3b8');
  const priorityColor = (p) => ({ URGENT: '#f87171', HIGH: '#fb923c', NORMAL: '#818cf8', LOW: '#94a3b8' }[p] || '#818cf8');
  const priorityIcon = (p) => ({ URGENT: '🔴', HIGH: '🟠', NORMAL: '🔵', LOW: '⚪' }[p] || '🔵');

  if (loading) return <Box display="flex" justifyContent="center" mt={10}><CircularProgress sx={{ color: '#818cf8' }} /></Box>;

  return (
    <Box sx={{ minHeight: '100vh' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4, flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h3" sx={{ fontWeight: 800, mb: 1, background: 'linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
            📬 Send Queue
          </Typography>
          <Typography variant="body1" sx={{ color: '#94a3b8' }}>Queue emails for scheduled delivery with priority management.</Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1.5 }}>
          <Button variant="contained" onClick={handleProcess} disabled={processing}
            sx={{ py: 1.2, borderRadius: 2, fontWeight: 700, textTransform: 'none', background: 'linear-gradient(135deg, #34d399 0%, #06b6d4 100%)' }}>
            {processing ? <CircularProgress size={20} color="inherit" /> : '🚀 Process'}
          </Button>
          <Button variant="contained" onClick={() => setEnqueueOpen(true)}
            sx={{ py: 1.2, borderRadius: 2, fontWeight: 700, textTransform: 'none', background: 'linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%)' }}>
            ➕ Enqueue
          </Button>
        </Box>
      </Box>

      {stats && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          {[{ label: 'Total', value: stats.totalItems, color: '#e0e0e0' }, { label: 'Queued', value: stats.queuedCount, color: '#fbbf24' }, { label: 'Sent', value: stats.sentCount, color: '#34d399' }, { label: 'Failed', value: stats.failedCount, color: '#f87171' }].map((s, i) => (
            <Grid item xs={6} sm={3} key={i}>
              <Card sx={{ ...glassCard, textAlign: 'center', py: 2 }}>
                <Typography variant="h4" sx={{ fontWeight: 800, color: s.color }}>{s.value}</Typography>
                <Typography variant="caption" sx={{ color: '#94a3b8', fontWeight: 600 }}>{s.label}</Typography>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>Filter</InputLabel>
          <Select value={filterStatus} label="Filter" onChange={(e) => setFilterStatus(e.target.value)}>
            <MenuItem value="">All</MenuItem>
            <MenuItem value="QUEUED">🟡 Queued</MenuItem>
            <MenuItem value="SENT">🟢 Sent</MenuItem>
            <MenuItem value="FAILED">🔴 Failed</MenuItem>
            <MenuItem value="CANCELLED">⚪ Cancelled</MenuItem>
          </Select>
        </FormControl>
      </Box>

      {items.length === 0 ? (
        <Paper sx={{ ...glassCard, textAlign: 'center', py: 8 }}>
          <Typography variant="body1" sx={{ color: '#64748b', fontStyle: 'italic', mb: 2 }}>No items in queue. Click "Enqueue" to start.</Typography>
        </Paper>
      ) : (
        items.map((item) => (
          <Card key={item.queueId} sx={{ ...glassCard, mb: 2, borderLeft: `4px solid ${priorityColor(item.priority)}` }}>
            <CardContent sx={{ p: 2.5 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1.5, flexWrap: 'wrap', gap: 1 }}>
                <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>
                  <Chip label={item.status} size="small" sx={{ bgcolor: `${statusColor(item.status)}18`, color: statusColor(item.status), fontWeight: 700 }} />
                  <Chip label={`${priorityIcon(item.priority)} ${item.priority}`} size="small" sx={{ bgcolor: `${priorityColor(item.priority)}15`, color: priorityColor(item.priority), fontWeight: 600 }} />
                  {item.retryCount > 0 && <Chip label={`Retry ${item.retryCount}/3`} size="small" sx={{ bgcolor: 'rgba(251,191,36,0.12)', color: '#fbbf24' }} />}
                </Box>
                <Box sx={{ display: 'flex', gap: 0.5 }}>
                  {item.status === 'QUEUED' && <Tooltip title="Cancel"><IconButton size="small" onClick={() => handleCancel(item.queueId)} sx={{ color: '#f87171' }}>⛔</IconButton></Tooltip>}
                  <Tooltip title="Delete"><IconButton size="small" onClick={() => handleDelete(item.queueId)} sx={{ color: '#94a3b8' }}>🗑️</IconButton></Tooltip>
                </Box>
              </Box>
              <Typography variant="subtitle1" sx={{ fontWeight: 700, color: '#e0e0e0', mb: 0.5 }}>
                To: {item.recipientName ? `${item.recipientName} <${item.recipientEmail}>` : item.recipientEmail}
              </Typography>
              <Typography variant="body2" sx={{ color: '#818cf8', fontWeight: 600, mb: 0.5 }}>Subject: {item.subjectLine || '(none)'}</Typography>
              <Typography variant="body2" sx={{ color: '#94a3b8', fontSize: '0.85rem', mb: 1, maxHeight: 50, overflow: 'hidden' }}>
                {item.bodyContent?.substring(0, 150)}{item.bodyContent?.length > 150 ? '...' : ''}
              </Typography>
              {item.errorMessage && (
                <Box sx={{ p: 1, borderRadius: 1.5, bgcolor: 'rgba(248,113,113,0.08)', mb: 1 }}>
                  <Typography variant="caption" sx={{ color: '#f87171', fontWeight: 600 }}>⚠️ {item.errorMessage}</Typography>
                </Box>
              )}
              <Box sx={{ display: 'flex', gap: 2 }}>
                <Typography variant="caption" sx={{ color: '#64748b' }}>Created: {item.createdAt ? new Date(item.createdAt).toLocaleString() : '—'}</Typography>
                <Typography variant="caption" sx={{ color: '#64748b' }}>Scheduled: {item.scheduledFor ? new Date(item.scheduledFor).toLocaleString() : '—'}</Typography>
                {item.sentAt && <Typography variant="caption" sx={{ color: '#34d399' }}>Sent: {new Date(item.sentAt).toLocaleString()}</Typography>}
              </Box>
            </CardContent>
          </Card>
        ))
      )}

      <Dialog open={enqueueOpen} onClose={() => setEnqueueOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 700 }}>📬 Enqueue Email</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <TextField fullWidth size="small" label="Recipient Email *" value={form.recipientEmail} onChange={(e) => setForm(p => ({ ...p, recipientEmail: e.target.value }))} />
            <TextField fullWidth size="small" label="Recipient Name" value={form.recipientName} onChange={(e) => setForm(p => ({ ...p, recipientName: e.target.value }))} />
            <TextField fullWidth size="small" label="Subject Line" value={form.subjectLine} onChange={(e) => setForm(p => ({ ...p, subjectLine: e.target.value }))} />
            <TextField fullWidth multiline rows={3} size="small" label="Body *" value={form.bodyContent} onChange={(e) => setForm(p => ({ ...p, bodyContent: e.target.value }))} />
            <Grid container spacing={2}>
              <Grid item xs={6}>
                <FormControl fullWidth size="small">
                  <InputLabel>Priority</InputLabel>
                  <Select value={form.priority} label="Priority" onChange={(e) => setForm(p => ({ ...p, priority: e.target.value }))}>
                    <MenuItem value="LOW">⚪ Low</MenuItem><MenuItem value="NORMAL">🔵 Normal</MenuItem><MenuItem value="HIGH">🟠 High</MenuItem><MenuItem value="URGENT">🔴 Urgent</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={6}>
                <TextField fullWidth size="small" type="datetime-local" label="Schedule (optional)" value={form.scheduledFor} onChange={(e) => setForm(p => ({ ...p, scheduledFor: e.target.value }))} InputLabelProps={{ shrink: true }} />
              </Grid>
            </Grid>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEnqueueOpen(false)} sx={{ color: '#94a3b8' }}>Cancel</Button>
          <Button variant="contained" onClick={handleEnqueue} disabled={submitting || !form.recipientEmail.trim() || !form.bodyContent.trim()}
            sx={{ bgcolor: '#f59e0b', fontWeight: 700, textTransform: 'none' }}>{submitting ? <CircularProgress size={20} color="inherit" /> : '📬 Enqueue'}</Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={toast.open} autoHideDuration={4000} onClose={() => setToast(p => ({ ...p, open: false }))} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert onClose={() => setToast(p => ({ ...p, open: false }))} severity={toast.severity} variant="filled" sx={{ borderRadius: 3, fontWeight: 600 }}>{toast.message}</Alert>
      </Snackbar>
    </Box>
  );
};

export default EmailSendQueueDashboard;
