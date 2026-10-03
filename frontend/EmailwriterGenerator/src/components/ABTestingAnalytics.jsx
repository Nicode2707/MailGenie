import React, { useState, useMemo } from 'react';
import {
  Box, Typography, Paper, Grid, Card, CardContent, Button, Divider,
  Chip, TextField, IconButton, Tooltip, Switch, FormControlLabel,
  LinearProgress, Tab, Tabs, Badge, Avatar, useMediaQuery, useTheme
} from '@mui/material';

// ─── MOCK DATA ───────────────────────────────────────────────────────────────

const AB_CAMPAIGNS = [
  {
    id: 'camp-001', name: 'Welcome Series Launch', status: 'completed', type: 'subject_line',
    sentDate: '2026-08-20', totalSent: 12450, variants: [
      { id: 'A', name: 'Welcome to MailGenie! ✨', opens: 5230, clicks: 1820, bounces: 45, unsubscribes: 12, conversions: 340 },
      { id: 'B', name: 'Your journey starts here — let\'s write!', opens: 4890, clicks: 2100, bounces: 38, unsubscribes: 8, conversions: 410 }
    ],
    confidence: 94, winner: 'B'
  },
  {
    id: 'camp-002', name: 'Product Update August', status: 'completed', type: 'content',
    sentDate: '2026-08-18', totalSent: 8920, variants: [
      { id: 'A', name: 'Feature-focused (bullet points)', opens: 3200, clicks: 1450, bounces: 32, unsubscribes: 15, conversions: 280 },
      { id: 'B', name: 'Story-driven (narrative)', opens: 3560, clicks: 1200, bounces: 28, unsubscribes: 10, conversions: 195 }
    ],
    confidence: 87, winner: 'A'
  },
  {
    id: 'camp-003', name: 'Re-engagement Blast', status: 'running', type: 'send_time',
    sentDate: '2026-08-25', totalSent: 6300, variants: [
      { id: 'A', name: 'Tuesday 9:00 AM', opens: 2100, clicks: 890, bounces: 22, unsubscribes: 5, conversions: 120 },
      { id: 'B', name: 'Thursday 2:00 PM', opens: 1850, clicks: 760, bounces: 25, unsubscribes: 7, conversions: 95 }
    ],
    confidence: 72, winner: null
  },
  {
    id: 'camp-004', name: 'Pricing Page CTA Test', status: 'completed', type: 'cta_button',
    sentDate: '2026-08-15', totalSent: 15200, variants: [
      { id: 'A', name: '"Start Free Trial"', opens: 6100, clicks: 3200, bounces: 55, unsubscribes: 18, conversions: 580 },
      { id: 'B', name: '"Explore Features"', opens: 5980, clicks: 2450, bounces: 48, unsubscribes: 14, conversions: 320 }
    ],
    confidence: 99, winner: 'A'
  },
  {
    id: 'camp-005', name: 'Newsletter Format Test', status: 'completed', type: 'content',
    sentDate: '2026-08-10', totalSent: 22000, variants: [
      { id: 'A', name: 'Long-form digest', opens: 8800, clicks: 2640, bounces: 88, unsubscribes: 44, conversions: 520 },
      { id: 'B', name: 'Short snippet + links', opens: 10120, clicks: 3960, bounces: 66, unsubscribes: 22, conversions: 680 }
    ],
    confidence: 97, winner: 'B'
  },
  {
    id: 'camp-006', name: 'Onboarding Email Flow', status: 'draft', type: 'subject_line',
    sentDate: null, totalSent: 0, variants: [
      { id: 'A', name: 'Let\'s get you set up! 🚀', opens: 0, clicks: 0, bounces: 0, unsubscribes: 0, conversions: 0 },
      { id: 'B', name: '3 steps to email mastery', opens: 0, clicks: 0, bounces: 0, unsubscribes: 0, conversions: 0 }
    ],
    confidence: 0, winner: null
  }
];

const SUBJECT_LINE_TESTS = [
  { id: 1, subject: '🔥 Don\'t miss out — limited time offer', openRate: 42.3, clickRate: 18.7, sentiment: 'urgency' },
  { id: 2, subject: 'Your weekly digest is ready', openRate: 38.1, clickRate: 12.4, sentiment: 'neutral' },
  { id: 3, subject: 'John, we have something special for you', openRate: 45.8, clickRate: 22.1, sentiment: 'personalized' },
  { id: 4, subject: '[Important] Action required on your account', openRate: 51.2, clickRate: 28.9, sentiment: 'urgency' },
  { id: 5, subject: 'How top teams write emails (2026 guide)', openRate: 36.5, clickRate: 15.3, sentiment: 'educational' },
  { id: 6, subject: 'Quick question about your workflow', openRate: 44.1, clickRate: 19.8, sentiment: 'curiosity' },
  { id: 7, subject: 'New feature: AI-powered templates', openRate: 33.2, clickRate: 14.6, sentiment: 'announcement' },
  { id: 8, subject: 'You\'re invited: Exclusive webinar Thursday', openRate: 39.7, clickRate: 21.4, sentiment: 'exclusivity' }
];

const SEND_TIME_DATA = [
  { day: 'Monday', times: [{ time: '8:00 AM', openRate: 32.1 }, { time: '12:00 PM', openRate: 28.5 }, { time: '6:00 PM', openRate: 22.3 }] },
  { day: 'Tuesday', times: [{ time: '8:00 AM', openRate: 41.2 }, { time: '12:00 PM', openRate: 35.8 }, { time: '6:00 PM', openRate: 25.1 }] },
  { day: 'Wednesday', times: [{ time: '8:00 AM', openRate: 38.7 }, { time: '12:00 PM', openRate: 33.2 }, { time: '6:00 PM', openRate: 24.8 }] },
  { day: 'Thursday', times: [{ time: '8:00 AM', openRate: 39.5 }, { time: '12:00 PM', openRate: 37.9 }, { time: '6:00 PM', openRate: 26.4 }] },
  { day: 'Friday', times: [{ time: '8:00 AM', openRate: 35.8 }, { time: '12:00 PM', openRate: 30.1 }, { time: '6:00 PM', openRate: 18.9 }] },
  { day: 'Saturday', times: [{ time: '10:00 AM', openRate: 25.3 }, { time: '2:00 PM', openRate: 22.1 }, { time: '8:00 PM', openRate: 15.7 }] },
  { day: 'Sunday', times: [{ time: '10:00 AM', openRate: 21.8 }, { time: '2:00 PM', openRate: 19.4 }, { time: '8:00 PM', openRate: 14.2 }] }
];

const AUDIENCE_SEGMENTS = [
  { id: 'seg-1', name: 'Active Users (7d)', size: 34200, avgOpenRate: 42.1, avgClickRate: 18.3, topVariant: 'Personalized', icon: '🟢' },
  { id: 'seg-2', name: 'Dormant (30d+)', size: 12800, avgOpenRate: 15.2, avgClickRate: 4.1, topVariant: 'Re-engagement', icon: '🟡' },
  { id: 'seg-3', name: 'New Signups (<7d)', size: 5600, avgOpenRate: 58.4, avgClickRate: 32.7, topVariant: 'Welcome', icon: '🔵' },
  { id: 'seg-4', name: 'Premium Plan', size: 8400, avgOpenRate: 51.3, avgClickRate: 27.5, topVariant: 'Feature-focused', icon: '🟣' },
  { id: 'seg-5', name: 'Free Tier', size: 28000, avgOpenRate: 31.7, avgClickRate: 11.2, topVariant: 'Upgrade CTA', icon: '⚪' },
  { id: 'seg-6', name: 'Enterprise', size: 1200, avgOpenRate: 48.9, avgClickRate: 25.1, topVariant: 'ROI-focused', icon: '🟠' }
];

const INSIGHTS = [
  { id: 1, type: 'trend', title: 'Personalized subject lines outperform generic by 23%', description: 'Adding recipient name to subject lines consistently shows higher open rates across all segments.', impact: 'high' },
  { id: 2, type: 'anomaly', title: 'Tuesday 8AM is your best send window', description: 'Tuesday morning emails see 41% open rate vs. 28% average. Consider shifting more campaigns to this slot.', impact: 'high' },
  { id: 3, type: 'tip', title: 'Shorter CTAs convert better for free-tier users', description: 'Single-action CTAs ("Start Free Trial") outperform exploratory CTAs ("Learn More") by 81% for free-tier audience.', impact: 'medium' },
  { id: 4, type: 'warning', title: 'Unsubscribe rate rising on re-engagement campaigns', description: 'Dormant user re-engagement shows 0.18% unsubscribe rate — above 0.1% threshold. Consider softer approach.', impact: 'medium' },
  { id: 5, type: 'trend', title: 'Story-driven content wins for product updates', description: 'Narrative-style product emails have 25% higher open rates but 20% lower click rates vs. bullet-point format.', impact: 'low' }
];

// ─── HELPER COMPONENTS ───────────────────────────────────────────────────────

const StatCard = ({ label, value, subtitle, color = 'primary.main', icon }) => (
  <Paper sx={{ p: 3, textAlign: 'center', background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
    <Typography variant="caption" sx={{ fontWeight: 700, color: 'text.secondary', letterSpacing: 0.5 }}>{label}</Typography>
    <Typography variant="h4" sx={{ mt: 1, fontWeight: 800, color }}>{icon} {value}</Typography>
    {subtitle && <Typography variant="caption" sx={{ display: 'block', mt: 0.5, color: 'text.secondary' }}>{subtitle}</Typography>}
  </Paper>
);

const MetricBar = ({ label, valueA, valueB, labelA, labelB, unit = '%', max = 100 }) => {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  return (
    <Box sx={{ mb: 2.5 }}>
      <Typography variant="body2" sx={{ fontWeight: 700, mb: 1, color: 'text.primary' }}>{label}</Typography>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
        <Typography variant="caption" sx={{ minWidth: 60, fontWeight: 600, color: '#6366f1' }}>A: {valueA}{unit}</Typography>
        <Box sx={{ flex: 1, height: 10, bgcolor: isDark ? 'rgba(255,255,255,0.06)' : '#e2e8f0', borderRadius: 5, overflow: 'hidden' }}>
          <Box sx={{ width: `${(valueA / max) * 100}%`, height: '100%', background: 'linear-gradient(90deg, #6366f1, #818cf8)', borderRadius: 5 }} />
        </Box>
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <Typography variant="caption" sx={{ minWidth: 60, fontWeight: 600, color: '#10b981' }}>B: {valueB}{unit}</Typography>
        <Box sx={{ flex: 1, height: 10, bgcolor: isDark ? 'rgba(255,255,255,0.06)' : '#e2e8f0', borderRadius: 5, overflow: 'hidden' }}>
          <Box sx={{ width: `${(valueB / max) * 100}%`, height: '100%', background: 'linear-gradient(90deg, #10b981, #34d399)', borderRadius: 5 }} />
        </Box>
      </Box>
      {valueA > 0 && valueB > 0 && (
        <Typography variant="caption" sx={{ mt: 0.5, display: 'block', color: valueB > valueA ? '#10b981' : '#f59e0b', fontWeight: 700 }}>
          {valueB > valueA ? `↑ Variant B wins by +${(valueB - valueA).toFixed(1)}${unit}` : `↑ Variant A wins by +${(valueA - valueB).toFixed(1)}${unit}`}
        </Typography>
      )}
    </Box>
  );
};

// ─── MAIN COMPONENT ──────────────────────────────────────────────────────────

export default function ABTestingAnalytics() {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [activeTab, setActiveTab] = useState(0);
  const [selectedCampaign, setSelectedCampaign] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');

  // ─── OVERVIEW METRICS ──────────────────────────────────────────────────
  const totalCampaigns = AB_CAMPAIGNS.length;
  const completedCampaigns = AB_CAMPAIGNS.filter(c => c.status === 'completed').length;
  const totalSent = AB_CAMPAIGNS.reduce((s, c) => s + c.totalSent, 0);
  const avgConfidence = AB_CAMPAIGNS.filter(c => c.confidence > 0).reduce((s, c) => s + c.confidence, 0) / AB_CAMPAIGNS.filter(c => c.confidence > 0).length;
  const winnersDecided = AB_CAMPAIGNS.filter(c => c.winner).length;

  const filteredCampaigns = useMemo(() => {
    return AB_CAMPAIGNS.filter(c => {
      const matchSearch = c.name.toLowerCase().includes(searchTerm.toLowerCase()) || c.type.includes(searchTerm.toLowerCase());
      const matchStatus = statusFilter === 'all' || c.status === statusFilter;
      return matchSearch && matchStatus;
    });
  }, [searchTerm, statusFilter]);

  // ─── SVG MINI CHART ────────────────────────────────────────────────────
  const MiniBarChart = ({ data, height = 60, barColor = '#6366f1' }) => (
    <svg width="100%" height={height} viewBox={`0 0 ${data.length * 28} ${height}`} style={{ display: 'block' }}>
      {data.map((d, i) => {
        const maxVal = Math.max(...data.map(x => x.value));
        const barH = maxVal > 0 ? (d.value / maxVal) * (height - 12) : 0;
        return (
          <g key={i}>
            <rect x={i * 28 + 4} y={height - barH - 4} width="20" height={barH} rx="4" fill={d.color || barColor} opacity={0.85} />
            <text x={i * 28 + 14} y={height - barH - 7} textAnchor="middle" fontSize="8" fill="#94a3b8" fontWeight="700">{d.value}%</text>
            <text x={i * 28 + 14} y={height - 1} textAnchor="middle" fontSize="7" fill="#64748b">{d.label}</text>
          </g>
        );
      })}
    </svg>
  );

  // ─── RENDER: OVERVIEW TAB ──────────────────────────────────────────────
  const renderOverview = () => (
    <Box>
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={6} md={3}><StatCard label="Total Campaigns" value={totalCampaigns} subtitle={`${completedCampaigns} completed`} color="primary.main" /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Emails Sent" value={totalSent.toLocaleString()} subtitle="across all tests" color="#10b981" /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Avg Confidence" value={`${avgConfidence.toFixed(0)}%`} subtitle="statistical certainty" color="#f59e0b" /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Winners Decided" value={`${winnersDecided}/${completedCampaigns}`} subtitle="tests concluded" color="#8b5cf6" /></Grid>
      </Grid>

      {/* Campaign Performance Summary */}
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📊 Campaign Performance Overview</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <Grid container spacing={3}>
          {AB_CAMPAIGNS.filter(c => c.status === 'completed').map(camp => {
            const vA = camp.variants[0];
            const vB = camp.variants[1];
            const openRateA = camp.totalSent > 0 ? ((vA.opens / (camp.totalSent / 2)) * 100).toFixed(1) : 0;
            const openRateB = camp.totalSent > 0 ? ((vB.opens / (camp.totalSent / 2)) * 100).toFixed(1) : 0;
            return (
              <Grid item xs={12} md={6} key={camp.id}>
                <Card sx={{ background: 'rgba(255,255,255,0.03)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.05)' }}>
                  <CardContent>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                      <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>{camp.name}</Typography>
                      <Chip size="small" label={camp.type.replace('_', ' ')} sx={{ textTransform: 'capitalize', fontWeight: 600, fontSize: '0.7rem' }} />
                    </Box>
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                      {camp.totalSent.toLocaleString()} sent • Winner: Variant {camp.winner} ({camp.confidence}% confidence)
                    </Typography>
                    <Box sx={{ mt: 2 }}>
                      <MiniBarChart data={[
                        { label: `A: ${openRateA}%`, value: parseFloat(openRateA), color: '#6366f1' },
                        { label: `B: ${openRateB}%`, value: parseFloat(openRateB), color: '#10b981' }
                      ]} height={50} />
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      </Paper>

      {/* Insights */}
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>💡 AI-Powered Insights</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        {INSIGHTS.map(insight => (
          <Box key={insight.id} sx={{ mb: 2, p: 2.5, borderRadius: 2, background: insight.impact === 'high' ? 'rgba(99,102,241,0.08)' : insight.impact === 'medium' ? 'rgba(245,158,11,0.08)' : 'rgba(255,255,255,0.03)', borderLeft: `4px solid ${insight.impact === 'high' ? '#6366f1' : insight.impact === 'medium' ? '#f59e0b' : '#64748b'}` }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>{insight.title}</Typography>
              <Chip size="small" label={insight.impact} sx={{ fontSize: '0.65rem', height: 20, fontWeight: 600, bgcolor: insight.impact === 'high' ? 'rgba(99,102,241,0.2)' : insight.impact === 'medium' ? 'rgba(245,158,11,0.2)' : 'rgba(255,255,255,0.08)' }} />
            </Box>
            <Typography variant="body2" sx={{ color: 'text.secondary', fontSize: '0.85rem' }}>{insight.description}</Typography>
          </Box>
        ))}
      </Paper>
    </Box>
  );

  // ─── RENDER: CAMPAIGNS TAB ─────────────────────────────────────────────
  const renderCampaigns = () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 2, mb: 4, flexWrap: 'wrap' }}>
        <TextField size="small" placeholder="Search campaigns..." value={searchTerm} onChange={e => setSearchTerm(e.target.value)}
          sx={{ flex: 1, minWidth: 200 }} />
        {['all', 'completed', 'running', 'draft'].map(s => (
          <Chip key={s} label={s.charAt(0).toUpperCase() + s.slice(1)} onClick={() => setStatusFilter(s)}
            color={statusFilter === s ? 'primary' : 'default'} variant={statusFilter === s ? 'filled' : 'outlined'}
            sx={{ fontWeight: 600, cursor: 'pointer' }} />
        ))}
      </Box>

      <Grid container spacing={3}>
        {filteredCampaigns.map(camp => (
          <Grid item xs={12} md={6} key={camp.id}>
            <Card sx={{ background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', cursor: 'pointer', transition: 'all 0.2s', '&:hover': { borderColor: '#6366f1', transform: 'translateY(-2px)' } }}
              onClick={() => setSelectedCampaign(selectedCampaign === camp.id ? null : camp.id)}>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Typography variant="h6" sx={{ fontWeight: 700, fontSize: '1rem' }}>{camp.name}</Typography>
                  <Chip size="small" label={camp.status} sx={{ textTransform: 'capitalize', fontWeight: 600, bgcolor: camp.status === 'completed' ? 'rgba(16,185,129,0.15)' : camp.status === 'running' ? 'rgba(99,102,241,0.15)' : 'rgba(148,163,184,0.15)', color: camp.status === 'completed' ? '#10b981' : camp.status === 'running' ? '#6366f1' : '#94a3b8' }} />
                </Box>
                <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                  Type: {camp.type.replace(/_/g, ' ')} • {camp.totalSent.toLocaleString()} sent {camp.sentDate ? `• ${camp.sentDate}` : ''}
                </Typography>

                {selectedCampaign === camp.id && (
                  <Box sx={{ mt: 3 }}>
                    <Divider sx={{ mb: 2, opacity: 0.2 }} />
                    {camp.variants.map(v => {
                      const openRate = camp.totalSent > 0 ? ((v.opens / (camp.totalSent / 2)) * 100).toFixed(1) : '0.0';
                      const clickRate = camp.totalSent > 0 ? ((v.clicks / (camp.totalSent / 2)) * 100).toFixed(1) : '0.0';
                      const bounceRate = camp.totalSent > 0 ? ((v.bounces / (camp.totalSent / 2)) * 100).toFixed(2) : '0.00';
                      return (
                        <Box key={v.id} sx={{ mb: 2, p: 2, borderRadius: 2, background: camp.winner === v.id ? 'rgba(16,185,129,0.08)' : 'rgba(255,255,255,0.03)', border: camp.winner === v.id ? '1px solid rgba(16,185,129,0.3)' : '1px solid rgba(255,255,255,0.05)' }}>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                              Variant {v.id}: {v.name}
                            </Typography>
                            {camp.winner === v.id && <Chip size="small" label="🏆 Winner" sx={{ bgcolor: 'rgba(16,185,129,0.2)', color: '#10b981', fontWeight: 700, fontSize: '0.7rem' }} />}
                          </Box>
                          <Grid container spacing={1} sx={{ mt: 1 }}>
                            {[
                              { label: 'Opens', value: v.opens.toLocaleString(), rate: `${openRate}%` },
                              { label: 'Clicks', value: v.clicks.toLocaleString(), rate: `${clickRate}%` },
                              { label: 'Bounces', value: v.bounces, rate: `${bounceRate}%` },
                              { label: 'Conversions', value: v.conversions, rate: '' }
                            ].map(m => (
                              <Grid item xs={3} key={m.label}>
                                <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>{m.label}</Typography>
                                <Typography variant="body2" sx={{ fontWeight: 700 }}>{m.value}</Typography>
                                {m.rate && <Typography variant="caption" sx={{ color: '#6366f1', fontWeight: 600 }}>{m.rate}</Typography>}
                              </Grid>
                            ))}
                          </Grid>
                        </Box>
                      );
                    })}
                    {camp.confidence > 0 && (
                      <Box sx={{ mt: 1 }}>
                        <Typography variant="caption" sx={{ fontWeight: 700, color: 'text.secondary' }}>Statistical Confidence: {camp.confidence}%</Typography>
                        <LinearProgress variant="determinate" value={camp.confidence} sx={{ mt: 0.5, height: 8, borderRadius: 4, bgcolor: isDark ? 'rgba(255,255,255,0.06)' : '#e2e8f0', '& .MuiLinearProgress-bar': { borderRadius: 4, background: camp.confidence >= 95 ? '#10b981' : camp.confidence >= 80 ? '#f59e0b' : '#6366f1' } }} />
                      </Box>
                    )}
                  </Box>
                )}
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );

  // ─── RENDER: SUBJECT LINES TAB ─────────────────────────────────────────
  const renderSubjectLines = () => (
    <Box>
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📝 Subject Line Performance</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        {SUBJECT_LINE_TESTS.sort((a, b) => b.openRate - a.openRate).map((test, idx) => (
          <Box key={test.id} sx={{ mb: 2, p: 2.5, borderRadius: 2, background: idx === 0 ? 'rgba(16,185,129,0.08)' : 'rgba(255,255,255,0.03)', border: idx === 0 ? '1px solid rgba(16,185,129,0.2)' : '1px solid rgba(255,255,255,0.05)' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
              <Box sx={{ flex: 1 }}>
                <Typography variant="body1" sx={{ fontWeight: 700, mb: 0.5 }}>
                  {idx === 0 && '🏆 '}"{test.subject}"
                </Typography>
                <Chip size="small" label={test.sentiment} sx={{ textTransform: 'capitalize', fontSize: '0.7rem', height: 20, bgcolor: 'rgba(99,102,241,0.12)', color: '#6366f1', fontWeight: 600 }} />
              </Box>
              <Box sx={{ textAlign: 'right' }}>
                <Typography variant="h6" sx={{ fontWeight: 800, color: '#10b981' }}>{test.openRate}%</Typography>
                <Typography variant="caption" sx={{ color: 'text.secondary' }}>open rate</Typography>
              </Box>
            </Box>
            <Box sx={{ display: 'flex', gap: 3 }}>
              <Typography variant="caption" sx={{ color: 'text.secondary' }}>Click Rate: <strong style={{ color: '#6366f1' }}>{test.clickRate}%</strong></Typography>
              <Typography variant="caption" sx={{ color: 'text.secondary' }}>Open/Click Ratio: <strong>{(test.openRate / test.clickRate).toFixed(1)}x</strong></Typography>
            </Box>
          </Box>
        ))}
      </Paper>

      {/* Subject Line Patterns */}
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>🧠 Sentiment Analysis</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <Grid container spacing={3}>
          {Object.entries(SUBJECT_LINE_TESTS.reduce((acc, t) => { acc[t.sentiment] = acc[t.sentiment] || []; acc[t.sentiment].push(t); return acc; }, {})).map(([sentiment, tests]) => {
            const avgOpen = (tests.reduce((s, t) => s + t.openRate, 0) / tests.length).toFixed(1);
            return (
              <Grid item xs={12} sm={6} md={4} key={sentiment}>
                <Card sx={{ background: 'rgba(255,255,255,0.03)', borderRadius: 2, border: '1px solid rgba(255,255,255,0.05)' }}>
                  <CardContent sx={{ textAlign: 'center' }}>
                    <Typography variant="subtitle2" sx={{ fontWeight: 700, textTransform: 'capitalize', mb: 1 }}>{sentiment}</Typography>
                    <Typography variant="h4" sx={{ fontWeight: 800, color: '#6366f1' }}>{avgOpen}%</Typography>
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>avg open rate ({tests.length} tests)</Typography>
                  </CardContent>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      </Paper>
    </Box>
  );

  // ─── RENDER: SEND TIME TAB ─────────────────────────────────────────────
  const renderSendTime = () => (
    <Box>
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>⏰ Send Time Heatmap</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <Box sx={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'separate', borderSpacing: 4 }}>
            <thead>
              <tr>
                <th style={{ padding: '8px 12px', textAlign: 'left', color: '#94a3b8', fontSize: '0.8rem', fontWeight: 700 }}>Day</th>
                {SEND_TIME_DATA[0].times.map(t => (
                  <th key={t.time} style={{ padding: '8px 12px', textAlign: 'center', color: '#94a3b8', fontSize: '0.8rem', fontWeight: 700 }}>{t.time}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {SEND_TIME_DATA.map(day => (
                <tr key={day.day}>
                  <td style={{ padding: '8px 12px', fontWeight: 700, fontSize: '0.85rem' }}>{day.day}</td>
                  {day.times.map(t => {
                    const intensity = t.openRate / 60;
                    const bg = t.openRate >= 38 ? 'rgba(16,185,129,0.25)' : t.openRate >= 30 ? 'rgba(99,102,241,0.2)' : t.openRate >= 22 ? 'rgba(245,158,11,0.15)' : 'rgba(255,255,255,0.03)';
                    const textColor = t.openRate >= 38 ? '#10b981' : t.openRate >= 30 ? '#6366f1' : t.openRate >= 22 ? '#f59e0b' : '#64748b';
                    return (
                      <td key={t.time} style={{ padding: '10px 12px', textAlign: 'center', background: bg, borderRadius: 8, transition: 'all 0.2s' }}>
                        <span style={{ fontWeight: 700, color: textColor, fontSize: '0.9rem' }}>{t.openRate}%</span>
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </Box>
        <Box sx={{ mt: 3, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
          {[
            { label: '🟢 Best (38%+)', color: 'rgba(16,185,129,0.25)' },
            { label: '🔵 Good (30-38%)', color: 'rgba(99,102,241,0.2)' },
            { label: '🟡 Average (22-30%)', color: 'rgba(245,158,11,0.15)' },
            { label: '⚪ Below (<22%)', color: 'rgba(255,255,255,0.03)' }
          ].map(l => (
            <Box key={l.label} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Box sx={{ width: 16, height: 16, borderRadius: 1, bgcolor: l.color, border: '1px solid rgba(255,255,255,0.1)' }} />
              <Typography variant="caption" sx={{ fontWeight: 600, color: 'text.secondary' }}>{l.label}</Typography>
            </Box>
          ))}
        </Box>
      </Paper>

      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📈 Hourly Open Rate Distribution</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <MiniBarChart
          data={SEND_TIME_DATA.flatMap(d => d.times.map(t => ({ label: t.time.replace(' ', ''), value: t.openRate, color: t.openRate >= 38 ? '#10b981' : t.openRate >= 30 ? '#6366f1' : t.openRate >= 22 ? '#f59e0b' : '#475569' })))}
          height={80}
        />
      </Paper>
    </Box>
  );

  // ─── RENDER: AUDIENCE TAB ──────────────────────────────────────────────
  const renderAudience = () => (
    <Box>
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>👥 Audience Segment Performance</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <Grid container spacing={3}>
          {AUDIENCE_SEGMENTS.map(seg => (
            <Grid item xs={12} sm={6} md={4} key={seg.id}>
              <Card sx={{ background: 'rgba(255,255,255,0.03)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.05)', transition: 'all 0.2s', '&:hover': { borderColor: '#6366f1', transform: 'translateY(-2px)' } }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2 }}>
                    <Typography variant="h5">{seg.icon}</Typography>
                    <Box>
                      <Typography variant="subtitle1" sx={{ fontWeight: 700, fontSize: '0.95rem' }}>{seg.name}</Typography>
                      <Typography variant="caption" sx={{ color: 'text.secondary' }}>{seg.size.toLocaleString()} contacts</Typography>
                    </Box>
                  </Box>
                  <Box sx={{ mb: 1.5 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                      <Typography variant="caption" sx={{ fontWeight: 600 }}>Open Rate</Typography>
                      <Typography variant="caption" sx={{ fontWeight: 700, color: '#6366f1' }}>{seg.avgOpenRate}%</Typography>
                    </Box>
                    <LinearProgress variant="determinate" value={seg.avgOpenRate} sx={{ height: 8, borderRadius: 4, bgcolor: isDark ? 'rgba(255,255,255,0.06)' : '#e2e8f0', '& .MuiLinearProgress-bar': { borderRadius: 4, background: 'linear-gradient(90deg, #6366f1, #818cf8)' } }} />
                  </Box>
                  <Box sx={{ mb: 1.5 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                      <Typography variant="caption" sx={{ fontWeight: 600 }}>Click Rate</Typography>
                      <Typography variant="caption" sx={{ fontWeight: 700, color: '#10b981' }}>{seg.avgClickRate}%</Typography>
                    </Box>
                    <LinearProgress variant="determinate" value={seg.avgClickRate} sx={{ height: 8, borderRadius: 4, bgcolor: isDark ? 'rgba(255,255,255,0.06)' : '#e2e8f0', '& .MuiLinearProgress-bar': { borderRadius: 4, background: 'linear-gradient(90deg, #10b981, #34d399)' } }} />
                  </Box>
                  <Chip size="small" label={`Best: ${seg.topVariant}`} sx={{ mt: 1, fontSize: '0.7rem', height: 22, fontWeight: 600, bgcolor: 'rgba(245,158,11,0.12)', color: '#f59e0b' }} />
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Paper>
    </Box>
  );

  // ─── RENDER: GUIDES TAB ────────────────────────────────────────────────
  const renderGuides = () => (
    <Box>
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📚 A/B Testing Best Practices</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        {[
          { title: '1. Test One Variable at a Time', desc: 'Isolate subject lines, send times, or content — never mix variables in a single test to ensure clean, actionable results.' },
          { title: '2. Reach Statistical Significance', desc: 'Wait for at least 95% confidence before declaring a winner. Premature decisions lead to false conclusions.' },
          { title: '3. Segment Your Audience', desc: 'Different segments respond differently. Run separate tests for active users, new signups, and dormant accounts.' },
          { title: '4. Test Send Times Regularly', desc: 'Optimal send times shift with seasons, holidays, and audience behavior changes. Re-test quarterly.' },
          { title: '5. Document Everything', desc: 'Log every test with hypothesis, variants, results, and learnings. Build an institutional knowledge base.' },
          { title: '6. Personalize When Possible', desc: 'Personalized subject lines consistently outperform generic ones by 20-30% across most industries.' }
        ].map((tip, i) => (
          <Box key={i} sx={{ mb: 3, p: 3, borderRadius: 2, background: 'rgba(255,255,255,0.03)', borderLeft: '4px solid #6366f1' }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 0.5 }}>{tip.title}</Typography>
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>{tip.desc}</Typography>
          </Box>
        ))}
      </Paper>
    </Box>
  );

  // ─── MAIN RENDER ───────────────────────────────────────────────────────
  const tabs = ['Overview', 'Campaigns', 'Subject Lines', 'Send Time', 'Audience', 'Best Practices'];

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h3" component="h1" gutterBottom sx={{ fontWeight: 800 }}>
          🧪 Email A/B Testing Analytics
        </Typography>
        <Typography variant="body1" sx={{ color: 'text.secondary' }}>
          Run, analyze, and optimize email experiments with statistical confidence tracking and AI-powered insights.
        </Typography>
      </Box>

      <Tabs
        value={activeTab}
        onChange={(_, v) => setActiveTab(v)}
        variant="scrollable"
        scrollButtons="auto"
        sx={{ mb: 4, '& .MuiTab-root': { fontWeight: 700, textTransform: 'none', fontSize: '0.9rem' } }}
      >
        {tabs.map((t, i) => <Tab key={i} label={t} />)}
      </Tabs>

      {activeTab === 0 && renderOverview()}
      {activeTab === 1 && renderCampaigns()}
      {activeTab === 2 && renderSubjectLines()}
      {activeTab === 3 && renderSendTime()}
      {activeTab === 4 && renderAudience()}
      {activeTab === 5 && renderGuides()}
    </Box>
  );
}
