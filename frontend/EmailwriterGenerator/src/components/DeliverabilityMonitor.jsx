import React, { useState, useMemo } from 'react';
import {
  Box, Typography, Paper, Grid, Card, CardContent, Button, Divider,
  Chip, TextField, IconButton, Tooltip, LinearProgress, Tab, Tabs,
  Badge, useMediaQuery, useTheme, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Alert
} from '@mui/material';

// ─── MOCK DATA ───────────────────────────────────────────────────────────────

const DELIVERABILITY_OVERVIEW = {
  overallScore: 92,
  inboxRate: 94.2,
  spamRate: 1.8,
  bounceRate: 3.1,
  openRate: 38.7,
  clickRate: 14.2,
  unsubscribeRate: 0.12,
  reputationScore: 88,
  dailyVolume: 45200,
  avgLatency: 1.2
};

const DOMAIN_REPUTATION = [
  { domain: 'mailgenie.com', score: 95, status: 'excellent', spf: true, dkim: true, dmarc: true, blacklist: false, tls: true, lastChecked: '2026-08-27T10:30:00Z' },
  { domain: 'marketing.mailgenie.com', score: 88, status: 'good', spf: true, dkim: true, dmarc: true, blacklist: false, tls: true, lastChecked: '2026-08-27T10:25:00Z' },
  { domain: 'notifications.mailgenie.com', score: 91, status: 'excellent', spf: true, dkim: true, dmarc: false, blacklist: false, tls: true, lastChecked: '2026-08-27T10:20:00Z' },
  { domain: 'support.mailgenie.com', score: 72, status: 'needs_attention', spf: true, dkim: false, dmarc: false, blacklist: false, tls: true, lastChecked: '2026-08-27T09:15:00Z' },
  { domain: 'dev.mailgenie.com', score: 65, status: 'critical', spf: false, dkim: false, dmarc: false, blacklist: true, tls: false, lastChecked: '2026-08-27T08:00:00Z' }
];

const ISP_DELIVERABILITY = [
  { isp: 'Gmail', inboxRate: 96.1, spamRate: 1.2, bounceRate: 2.7, openRate: 42.3, volume: 18500, trend: 'up' },
  { isp: 'Outlook/Hotmail', inboxRate: 93.5, spamRate: 2.1, bounceRate: 4.4, openRate: 35.8, volume: 12000, trend: 'stable' },
  { isp: 'Yahoo Mail', inboxRate: 89.2, spamRate: 3.8, bounceRate: 7.0, openRate: 31.2, volume: 5800, trend: 'down' },
  { isp: 'Apple iCloud', inboxRate: 95.8, spamRate: 0.9, bounceRate: 3.3, openRate: 44.1, volume: 4200, trend: 'up' },
  { isp: 'ProtonMail', inboxRate: 97.2, spamRate: 0.5, bounceRate: 2.3, openRate: 38.9, volume: 1500, trend: 'stable' },
  { isp: 'AOL Mail', inboxRate: 85.4, spamRate: 5.2, bounceRate: 9.4, openRate: 28.7, volume: 2200, trend: 'down' }
];

const BOUNCE_LOGS = [
  { id: 1, email: 'john.doe@expired-domain.com', type: 'hard', code: '550', reason: 'User unknown - mailbox does not exist', date: '2026-08-27T09:12:00Z', campaign: 'Welcome Series' },
  { id: 2, email: 'sarah@full-inbox.co', type: 'soft', code: '452', reason: 'Mailbox full', date: '2026-08-27T08:45:00Z', campaign: 'Product Update' },
  { id: 3, email: 'mike@spam-blocker.io', type: 'hard', code: '554', reason: 'Transaction rejected - spam detected', date: '2026-08-27T08:30:00Z', campaign: 'Re-engagement' },
  { id: 4, email: 'lisa@temp-mail.org', type: 'hard', code: '550', reason: 'User unknown - expired address', date: '2026-08-27T07:15:00Z', campaign: 'Welcome Series' },
  { id: 5, email: 'alex@slow-server.net', type: 'soft', code: '421', reason: 'Service unavailable - try again later', date: '2026-08-26T22:10:00Z', campaign: 'Newsletter' },
  { id: 6, email: 'david@corporate-firewall.com', type: 'soft', code: '451', reason: 'Local error in processing', date: '2026-08-26T18:30:00Z', campaign: 'Product Update' },
  { id: 7, email: 'emma@nonexistent.mx', type: 'hard', code: '550', reason: 'Mailbox not found', date: '2026-08-26T14:20:00Z', campaign: 'Welcome Series' },
  { id: 8, email: 'chris@blocklist-alert.com', type: 'hard', code: '554', reason: 'Rejected - found on RBL', date: '2026-08-26T11:05:00Z', campaign: 'Re-engagement' }
];

const BLACKLIST_CHECKS = [
  { blacklist: 'Spamhaus (SBL)', status: 'clean', lastChecked: '2026-08-27T10:00:00Z' },
  { blacklist: 'Spamhaus (XBL)', status: 'clean', lastChecked: '2026-08-27T10:00:00Z' },
  { blacklist: 'Barracuda RBL', status: 'clean', lastChecked: '2026-08-27T10:00:00Z' },
  { blacklist: 'SORBS', status: 'listed', lastChecked: '2026-08-27T10:00:00Z', note: 'dev.mailgenie.com listed since 2026-08-25' },
  { blacklist: 'SpamCop', status: 'clean', lastChecked: '2026-08-27T10:00:00Z' },
  { blacklist: 'Invaluement', status: 'clean', lastChecked: '2026-08-27T10:00:00Z' },
  { blacklist: 'UCEPROTECT L3', status: 'clean', lastChecked: '2026-08-27T10:00:00Z' }
];

const AUTHENTICATION_CHECKS = [
  { protocol: 'SPF', status: 'pass', details: 'v=spf1 include:_spf.google.com ~all', domains: { 'mailgenie.com': 'pass', 'marketing.mailgenie.com': 'pass', 'notifications.mailgenie.com': 'pass', 'support.mailgenie.com': 'pass', 'dev.mailgenie.com': 'fail' } },
  { protocol: 'DKIM', status: 'partial', details: '2048-bit key, selector: mailgenie2026', domains: { 'mailgenie.com': 'pass', 'marketing.mailgenie.com': 'pass', 'notifications.mailgenie.com': 'pass', 'support.mailgenie.com': 'fail', 'dev.mailgenie.com': 'fail' } },
  { protocol: 'DMARC', status: 'partial', details: 'p=quarantine; rua=mailto:dmarc@mailgenie.com', domains: { 'mailgenie.com': 'pass', 'marketing.mailgenie.com': 'pass', 'notifications.mailgenie.com': 'fail', 'support.mailgenie.com': 'fail', 'dev.mailgenie.com': 'fail' } },
  { protocol: 'TLS', status: 'partial', details: 'TLS 1.3 supported, certificate valid until 2027-01-15', domains: { 'mailgenie.com': 'pass', 'marketing.mailgenie.com': 'pass', 'notifications.mailgenie.com': 'pass', 'support.mailgenie.com': 'pass', 'dev.mailgenie.com': 'fail' } }
];

const DAILY_TREND = [
  { date: '08-21', inbox: 94.5, spam: 1.5, bounce: 4.0 },
  { date: '08-22', inbox: 93.8, spam: 1.9, bounce: 4.3 },
  { date: '08-23', inbox: 95.1, spam: 1.2, bounce: 3.7 },
  { date: '08-24', inbox: 94.2, spam: 1.6, bounce: 4.2 },
  { date: '08-25', inbox: 93.5, spam: 2.1, bounce: 4.4 },
  { date: '08-26', inbox: 94.8, spam: 1.4, bounce: 3.8 },
  { date: '08-27', inbox: 94.2, spam: 1.8, bounce: 4.0 }
];

const ALERTS = [
  { id: 1, severity: 'critical', title: 'dev.mailgenie.com listed on SORBS blacklist', message: 'Your development subdomain has been added to the SORBS blacklist. Immediate action required to delist.', date: '2026-08-27T08:00:00Z', resolved: false },
  { id: 2, severity: 'warning', title: 'Yahoo bounce rate exceeded 5% threshold', message: 'Yahoo/AOL delivery showing elevated bounce rates (7.0%). Review Yahoo-specific blocklists.', date: '2026-08-26T18:00:00Z', resolved: false },
  { id: 3, severity: 'info', title: 'DKIM rotation recommended for support subdomain', message: 'support.mailgenie.com DKIM key will expire in 45 days. Schedule rotation.', date: '2026-08-25T09:00:00Z', resolved: false },
  { id: 4, severity: 'info', title: 'DMARC policy upgrade recommended', message: 'Consider upgrading DMARC from p=quarantine to p=reject for main domain after 30-day monitoring period.', date: '2026-08-24T14:00:00Z', resolved: true }
];

// ─── HELPER COMPONENTS ───────────────────────────────────────────────────────

const ScoreGauge = ({ score, size = 120, label }) => {
  const getColor = (s) => s >= 90 ? '#10b981' : s >= 75 ? '#6366f1' : s >= 60 ? '#f59e0b' : '#ef4444';
  const getLabel = (s) => s >= 90 ? 'Excellent' : s >= 75 ? 'Good' : s >= 60 ? 'Fair' : 'Poor';
  const color = getColor(score);
  const radius = (size - 16) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (score / 100) * circumference * 0.75;

  return (
    <Box sx={{ textAlign: 'center' }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="8"
          strokeDasharray={`${circumference * 0.75} ${circumference * 0.25}`}
          strokeDashoffset={circumference * 0.125} strokeLinecap="round" transform={`rotate(135 ${size / 2} ${size / 2})`} />
        <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke={color} strokeWidth="8"
          strokeDasharray={`${circumference * 0.75} ${circumference * 0.25}`}
          strokeDashoffset={offset} strokeLinecap="round" transform={`rotate(135 ${size / 2} ${size / 2})`}
          style={{ transition: 'stroke-dashoffset 1s ease-in-out' }} />
        <text x={size / 2} y={size / 2 - 4} textAnchor="middle" fontSize={size * 0.22} fontWeight="800" fill={color}>{score}</text>
        <text x={size / 2} y={size / 2 + 14} textAnchor="middle" fontSize={size * 0.1} fontWeight="600" fill="#94a3b8">{getLabel(score)}</text>
      </svg>
      {label && <Typography variant="caption" sx={{ fontWeight: 700, color: 'text.secondary' }}>{label}</Typography>}
    </Box>
  );
};

const StatCard = ({ label, value, subtitle, color = 'primary.main', icon, trend }) => (
  <Paper sx={{ p: 3, textAlign: 'center', background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
    <Typography variant="caption" sx={{ fontWeight: 700, color: 'text.secondary', letterSpacing: 0.5 }}>{label}</Typography>
    <Typography variant="h4" sx={{ mt: 1, fontWeight: 800, color }}>{icon} {value}</Typography>
    {subtitle && <Typography variant="caption" sx={{ display: 'block', mt: 0.5, color: 'text.secondary' }}>{subtitle}</Typography>}
    {trend && (
      <Typography variant="caption" sx={{ color: trend === 'up' ? '#10b981' : trend === 'down' ? '#ef4444' : '#94a3b8', fontWeight: 700 }}>
        {trend === 'up' ? '↑ Improving' : trend === 'down' ? '↓ Declining' : '→ Stable'}
      </Typography>
    )}
  </Paper>
);

const MiniLineChart = ({ data, height = 60, lines }) => {
  const maxVal = Math.max(...data.flatMap(d => lines.map(l => d[l.key])));
  const width = data.length * 40;
  return (
    <svg width="100%" height={height} viewBox={`0 0 ${width} ${height}`} style={{ display: 'block' }}>
      {lines.map(line => {
        const points = data.map((d, i) => `${i * 40 + 20},${height - (d[line.key] / maxVal) * (height - 16) - 4}`).join(' ');
        return (
          <polyline key={line.key} points={points} fill="none" stroke={line.color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
        );
      })}
      {data.map((d, i) => (
        <text key={i} x={i * 40 + 20} y={height - 1} textAnchor="middle" fontSize="7" fill="#64748b">{d.date.split('-')[1]}</text>
      ))}
    </svg>
  );
};

// ─── MAIN COMPONENT ──────────────────────────────────────────────────────────

export default function DeliverabilityMonitor() {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [activeTab, setActiveTab] = useState(0);
  const [bounceTypeFilter, setBounceTypeFilter] = useState('all');

  const unresolvedAlerts = ALERTS.filter(a => !a.resolved);

  const filteredBounces = useMemo(() => {
    return BOUNCE_LOGS.filter(b => bounceTypeFilter === 'all' || b.type === bounceTypeFilter);
  }, [bounceTypeFilter]);

  // ─── RENDER: OVERVIEW ──────────────────────────────────────────────────
  const renderOverview = () => (
    <Box>
      {/* Alert Banner */}
      {unresolvedAlerts.filter(a => a.severity === 'critical').map(a => (
        <Alert key={a.id} severity="error" sx={{ mb: 3, borderRadius: 2 }}>
          <strong>{a.title}</strong> — {a.message}
        </Alert>
      ))}

      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={6} md={3}><StatCard label="Inbox Rate" value={`${DELIVERABILITY_OVERVIEW.inboxRate}%`} subtitle="primary metric" color="#10b981" trend="up" /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Spam Rate" value={`${DELIVERABILITY_OVERVIEW.spamRate}%`} subtitle="should be <2%" color={DELIVERABILITY_OVERVIEW.spamRate < 2 ? '#10b981' : '#f59e0b'} trend="stable" /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Bounce Rate" value={`${DELIVERABILITY_OVERVIEW.bounceRate}%`} subtitle="target <3%" color={DELIVERABILITY_OVERVIEW.bounceRate < 3 ? '#10b981' : '#f59e0b'} trend="down" /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Daily Volume" value={DELIVERABILITY_OVERVIEW.dailyVolume.toLocaleString()} subtitle="emails/day" color="primary.main" /></Grid>
      </Grid>

      {/* Score Gauges */}
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📊 Deliverability Scores</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <Grid container spacing={4} sx={{ justifyContent: 'center' }}>
          <Grid item><ScoreGauge score={DELIVERABILITY_OVERVIEW.overallScore} label="Overall Score" /></Grid>
          <Grid item><ScoreGauge score={DELIVERABILITY_OVERVIEW.reputationScore} label="Sender Reputation" /></Grid>
          <Grid item><ScoreGauge score={Math.round(DELIVERABILITY_OVERVIEW.openRate)} label="Open Rate Score" /></Grid>
          <Grid item><ScoreGauge score={100 - Math.round(DELIVERABILITY_OVERVIEW.bounceRate * 10)} label="Bounce Health" /></Grid>
        </Grid>
      </Paper>

      {/* 7-Day Trend */}
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📈 7-Day Trend</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <MiniLineChart height={100} data={DAILY_TREND} lines={[
          { key: 'inbox', color: '#10b981' },
          { key: 'spam', color: '#ef4444' },
          { key: 'bounce', color: '#f59e0b' }
        ]} />
        <Box sx={{ display: 'flex', gap: 3, mt: 2, justifyContent: 'center' }}>
          {[{ label: 'Inbox', color: '#10b981' }, { label: 'Spam', color: '#ef4444' }, { label: 'Bounce', color: '#f59e0b' }].map(l => (
            <Box key={l.label} sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <Box sx={{ width: 12, height: 3, borderRadius: 1, bgcolor: l.color }} />
              <Typography variant="caption" sx={{ fontWeight: 600, color: 'text.secondary' }}>{l.label}</Typography>
            </Box>
          ))}
        </Box>
      </Paper>

      {/* Alerts */}
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>🔔 Active Alerts ({unresolvedAlerts.length})</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        {ALERTS.map(alert => (
          <Box key={alert.id} sx={{ mb: 2, p: 2.5, borderRadius: 2, opacity: alert.resolved ? 0.5 : 1, background: alert.severity === 'critical' ? 'rgba(239,68,68,0.08)' : alert.severity === 'warning' ? 'rgba(245,158,11,0.08)' : 'rgba(99,102,241,0.06)', borderLeft: `4px solid ${alert.severity === 'critical' ? '#ef4444' : alert.severity === 'warning' ? '#f59e0b' : '#6366f1'}` }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>{alert.title}</Typography>
              <Chip size="small" label={alert.severity} sx={{ textTransform: 'capitalize', fontWeight: 600, fontSize: '0.7rem', bgcolor: alert.severity === 'critical' ? 'rgba(239,68,68,0.2)' : alert.severity === 'warning' ? 'rgba(245,158,11,0.2)' : 'rgba(99,102,241,0.15)' }} />
            </Box>
            <Typography variant="body2" sx={{ color: 'text.secondary', fontSize: '0.85rem', mt: 0.5 }}>{alert.message}</Typography>
            <Typography variant="caption" sx={{ color: 'text.secondary', mt: 0.5, display: 'block' }}>
              {new Date(alert.date).toLocaleString()} {alert.resolved && '• ✅ Resolved'}
            </Typography>
          </Box>
        ))}
      </Paper>
    </Box>
  );

  // ─── RENDER: DOMAINS ───────────────────────────────────────────────────
  const renderDomains = () => (
    <Box>
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>🌐 Domain Reputation</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                {['Domain', 'Score', 'SPF', 'DKIM', 'DMARC', 'TLS', 'Blacklist', 'Status'].map(h => (
                  <TableCell key={h} sx={{ fontWeight: 700, color: 'text.secondary', fontSize: '0.8rem' }}>{h}</TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {DOMAIN_REPUTATION.map(d => (
                <TableRow key={d.domain} sx={{ '&:hover': { bgcolor: 'rgba(255,255,255,0.03)' } }}>
                  <TableCell sx={{ fontWeight: 600, fontFamily: 'monospace', fontSize: '0.85rem' }}>{d.domain}</TableCell>
                  <TableCell>
                    <Chip size="small" label={d.score} sx={{ fontWeight: 800, bgcolor: d.score >= 90 ? 'rgba(16,185,129,0.15)' : d.score >= 75 ? 'rgba(99,102,241,0.15)' : 'rgba(239,68,68,0.15)', color: d.score >= 90 ? '#10b981' : d.score >= 75 ? '#6366f1' : '#ef4444' }} />
                  </TableCell>
                  {['spf', 'dkim', 'dmarc', 'tls'].map(p => (
                    <TableCell key={p}>
                      <Typography variant="body2" sx={{ color: d[p] ? '#10b981' : '#ef4444', fontWeight: 700 }}>{d[p] ? '✅' : '❌'}</Typography>
                    </TableCell>
                  ))}
                  <TableCell>
                    <Typography variant="body2" sx={{ color: d.blacklist ? '#ef4444' : '#10b981', fontWeight: 700 }}>{d.blacklist ? '⚠️ Listed' : '✅ Clean'}</Typography>
                  </TableCell>
                  <TableCell>
                    <Chip size="small" label={d.status.replace('_', ' ')} sx={{ textTransform: 'capitalize', fontWeight: 600, fontSize: '0.7rem', bgcolor: d.status === 'excellent' ? 'rgba(16,185,129,0.15)' : d.status === 'good' ? 'rgba(99,102,241,0.15)' : 'rgba(239,68,68,0.15)' }} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Box>
  );

  // ─── RENDER: ISPs ──────────────────────────────────────────────────────
  const renderISPs = () => (
    <Box>
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📬 ISP-Level Deliverability</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        {ISP_DELIVERABILITY.map(isp => (
          <Box key={isp.isp} sx={{ mb: 3, p: 3, borderRadius: 2, background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.05)' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>{isp.isp}</Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Typography variant="caption" sx={{ color: 'text.secondary' }}>{isp.volume.toLocaleString()} emails</Typography>
                <Chip size="small" label={isp.trend === 'up' ? '↑' : isp.trend === 'down' ? '↓' : '→'} sx={{ fontWeight: 700, bgcolor: isp.trend === 'up' ? 'rgba(16,185,129,0.15)' : isp.trend === 'down' ? 'rgba(239,68,68,0.15)' : 'rgba(148,163,184,0.15)' }} />
              </Box>
            </Box>
            <Grid container spacing={2}>
              {[
                { label: 'Inbox', value: isp.inboxRate, color: '#10b981' },
                { label: 'Spam', value: isp.spamRate, color: '#ef4444' },
                { label: 'Bounce', value: isp.bounceRate, color: '#f59e0b' },
                { label: 'Open', value: isp.openRate, color: '#6366f1' }
              ].map(m => (
                <Grid item xs={6} md={3} key={m.label}>
                  <Typography variant="caption" sx={{ fontWeight: 600, color: 'text.secondary' }}>{m.label}</Typography>
                  <Typography variant="body1" sx={{ fontWeight: 700, color: m.color }}>{m.value}%</Typography>
                  <LinearProgress variant="determinate" value={m.value} sx={{ mt: 0.5, height: 6, borderRadius: 3, bgcolor: isDark ? 'rgba(255,255,255,0.06)' : '#e2e8f0', '& .MuiLinearProgress-bar': { borderRadius: 3, bgcolor: m.color } }} />
                </Grid>
              ))}
            </Grid>
          </Box>
        ))}
      </Paper>
    </Box>
  );

  // ─── RENDER: BOUNCES ───────────────────────────────────────────────────
  const renderBounces = () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
        {['all', 'hard', 'soft'].map(t => (
          <Chip key={t} label={`${t.charAt(0).toUpperCase() + t.slice(1)} Bounces`} onClick={() => setBounceTypeFilter(t)}
            color={bounceTypeFilter === t ? 'primary' : 'default'} variant={bounceTypeFilter === t ? 'filled' : 'outlined'}
            sx={{ fontWeight: 600, cursor: 'pointer' }} />
        ))}
      </Box>

      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>🚫 Bounce Logs ({filteredBounces.length})</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                {['Email', 'Type', 'Code', 'Reason', 'Campaign', 'Date'].map(h => (
                  <TableCell key={h} sx={{ fontWeight: 700, color: 'text.secondary', fontSize: '0.8rem' }}>{h}</TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredBounces.map(b => (
                <TableRow key={b.id} sx={{ '&:hover': { bgcolor: 'rgba(255,255,255,0.03)' } }}>
                  <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>{b.email}</TableCell>
                  <TableCell>
                    <Chip size="small" label={b.type} sx={{ fontWeight: 600, bgcolor: b.type === 'hard' ? 'rgba(239,68,68,0.15)' : 'rgba(245,158,11,0.15)', color: b.type === 'hard' ? '#ef4444' : '#f59e0b' }} />
                  </TableCell>
                  <TableCell sx={{ fontFamily: 'monospace', fontWeight: 700 }}>{b.code}</TableCell>
                  <TableCell sx={{ fontSize: '0.8rem', color: 'text.secondary', maxWidth: 250 }}>{b.reason}</TableCell>
                  <TableCell sx={{ fontSize: '0.8rem' }}>{b.campaign}</TableCell>
                  <TableCell sx={{ fontSize: '0.8rem', color: 'text.secondary' }}>{new Date(b.date).toLocaleDateString()}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Box>
  );

  // ─── RENDER: AUTHENTICATION ────────────────────────────────────────────
  const renderAuthentication = () => (
    <Box>
      {/* Blacklist */}
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>🛡️ Blacklist Status</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <Grid container spacing={2}>
          {BLACKLIST_CHECKS.map(bl => (
            <Grid item xs={12} sm={6} md={4} key={bl.blacklist}>
              <Card sx={{ background: bl.status === 'clean' ? 'rgba(16,185,129,0.06)' : 'rgba(239,68,68,0.08)', borderRadius: 2, border: `1px solid ${bl.status === 'clean' ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.2)'}` }}>
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>{bl.blacklist}</Typography>
                    <Typography variant="body2" sx={{ color: bl.status === 'clean' ? '#10b981' : '#ef4444', fontWeight: 700 }}>
                      {bl.status === 'clean' ? '✅ Clean' : '⚠️ Listed'}
                    </Typography>
                  </Box>
                  {bl.note && <Typography variant="caption" sx={{ color: '#ef4444', mt: 0.5, display: 'block', fontWeight: 600 }}>{bl.note}</Typography>}
                  <Typography variant="caption" sx={{ color: 'text.secondary', mt: 0.5, display: 'block' }}>
                    Last checked: {new Date(bl.lastChecked).toLocaleTimeString()}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Paper>

      {/* Authentication Protocols */}
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>🔐 Authentication Protocols</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        {AUTHENTICATION_CHECKS.map(auth => (
          <Box key={auth.protocol} sx={{ mb: 3, p: 3, borderRadius: 2, background: 'rgba(255,255,255,0.03)' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>{auth.protocol}</Typography>
              <Chip size="small" label={auth.status} sx={{ textTransform: 'capitalize', fontWeight: 600, bgcolor: auth.status === 'pass' ? 'rgba(16,185,129,0.15)' : 'rgba(245,158,11,0.15)', color: auth.status === 'pass' ? '#10b981' : '#f59e0b' }} />
            </Box>
            <Typography variant="caption" sx={{ color: 'text.secondary', fontFamily: 'monospace', display: 'block', mb: 1.5 }}>{auth.details}</Typography>
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              {Object.entries(auth.domains).map(([domain, status]) => (
                <Chip key={domain} size="small" label={`${domain.split('.')[0]}: ${status === 'pass' ? '✅' : '❌'}`}
                  sx={{ fontFamily: 'monospace', fontSize: '0.7rem', bgcolor: status === 'pass' ? 'rgba(16,185,129,0.1)' : 'rgba(239,68,68,0.1)' }} />
              ))}
            </Box>
          </Box>
        ))}
      </Paper>
    </Box>
  );

  // ─── MAIN RENDER ───────────────────────────────────────────────────────
  const tabs = ['Overview', 'Domains', 'ISPs', 'Bounces', 'Authentication'];

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h3" component="h1" gutterBottom sx={{ fontWeight: 800 }}>
          📧 Email Deliverability Monitor
        </Typography>
        <Typography variant="body1" sx={{ color: 'text.secondary' }}>
          Track inbox placement, sender reputation, blacklist status, and ISP-level deliverability metrics.
        </Typography>
      </Box>

      <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)} variant="scrollable" scrollButtons="auto"
        sx={{ mb: 4, '& .MuiTab-root': { fontWeight: 700, textTransform: 'none', fontSize: '0.9rem' } }}>
        {tabs.map((t, i) => <Tab key={i} label={t} />)}
      </Tabs>

      {activeTab === 0 && renderOverview()}
      {activeTab === 1 && renderDomains()}
      {activeTab === 2 && renderISPs()}
      {activeTab === 3 && renderBounces()}
      {activeTab === 4 && renderAuthentication()}
    </Box>
  );
}
