import React, { useState, useMemo } from 'react';
import {
  Box, Typography, Paper, Grid, Card, CardContent, Button, Divider,
  Chip, TextField, IconButton, Tooltip, LinearProgress, Tab, Tabs,
  useMediaQuery, useTheme, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Select, MenuItem, FormControl, InputLabel
} from '@mui/material';

// ─── MOCK DATA ───────────────────────────────────────────────────────────────

const CAMPAIGNS = [
  {
    id: 'c001', name: 'Summer Sale Blitz', type: 'promotional', status: 'completed',
    sentDate: '2026-08-01', endDate: '2026-08-15', segment: 'active_buyers',
    metrics: { sent: 45200, delivered: 44100, opened: 18520, clicked: 7230, converted: 1810, unsubscribed: 89, bounced: 1100 },
    financial: { cost: 450, revenue: 27150, roi: 5933, costPerEmail: 0.01, revenuePerEmail: 0.60, costPerConversion: 0.25 },
    abTest: { variant: 'Subject line urgency vs curiosity', winner: 'Urgency (+18% open rate)' }
  },
  {
    id: 'c002', name: 'Product Launch Announcement', type: 'transactional', status: 'completed',
    sentDate: '2026-08-10', endDate: '2026-08-12', segment: 'all_subscribers',
    metrics: { sent: 62000, delivered: 60740, opened: 30370, clicked: 12150, converted: 2430, unsubscribed: 124, bounced: 1260 },
    financial: { cost: 620, revenue: 48600, roi: 7739, costPerEmail: 0.01, revenuePerEmail: 0.78, costPerConversion: 0.26 },
    abTest: { variant: 'Launch video vs text announcement', winner: 'Video (+35% click rate)' }
  },
  {
    id: 'c003', name: 'Weekly Newsletter #34', type: 'newsletter', status: 'completed',
    sentDate: '2026-08-18', endDate: '2026-08-20', segment: 'engaged_subscribers',
    metrics: { sent: 28400, delivered: 27832, opened: 11133, clicked: 3340, converted: 267, unsubscribed: 42, bounced: 568 },
    financial: { cost: 180, revenue: 2670, roi: 1383, costPerEmail: 0.006, revenuePerEmail: 0.09, costPerConversion: 0.67 },
    abTest: { variant: 'Long-form vs short-form digest', winner: 'Short-form (+22% open rate)' }
  },
  {
    id: 'c004', name: 'Win-Back Campaign Q3', type: 'promotional', status: 'completed',
    sentDate: '2026-08-05', endDate: '2026-08-20', segment: 'dormant_90d',
    metrics: { sent: 15800, delivered: 14220, opened: 4266, clicked: 1280, converted: 192, unsubscribed: 210, bounced: 1580 },
    financial: { cost: 220, revenue: 3840, roi: 1645, costPerEmail: 0.014, revenuePerEmail: 0.24, costPerConversion: 1.15 },
    abTest: { variant: 'Discount vs free trial offer', winner: 'Free trial (+45% conversion)' }
  },
  {
    id: 'c005', name: 'Onboarding Drip - Week 1', type: 'transactional', status: 'completed',
    sentDate: '2026-08-01', endDate: '2026-08-07', segment: 'new_signups',
    metrics: { sent: 8900, delivered: 8722, opened: 5233, clicked: 2617, converted: 523, unsubscribed: 18, bounced: 178 },
    financial: { cost: 89, revenue: 10460, roi: 11641, costPerEmail: 0.01, revenuePerEmail: 1.18, costPerConversion: 0.17 },
    abTest: { variant: 'Personalized vs generic welcome', winner: 'Personalized (+28% open rate)' }
  },
  {
    id: 'c006', name: 'Re-engagement Flash Sale', type: 'promotional', status: 'running',
    sentDate: '2026-08-25', endDate: '2026-08-30', segment: 'inactive_30d',
    metrics: { sent: 12400, delivered: 11780, opened: 4712, clicked: 1885, converted: 377, unsubscribed: 56, bounced: 620 },
    financial: { cost: 150, revenue: 7540, roi: 4927, costPerEmail: 0.012, revenuePerEmail: 0.61, costPerConversion: 0.40 },
    abTest: { variant: '24hr countdown vs 48hr window', winner: '24hr countdown (+32% urgency clicks)' }
  },
  {
    id: 'c007', name: 'Premium Upgrade Push', type: 'promotional', status: 'draft',
    sentDate: null, endDate: null, segment: 'free_tier_active',
    metrics: { sent: 0, delivered: 0, opened: 0, clicked: 0, converted: 0, unsubscribed: 0, bounced: 0 },
    financial: { cost: 0, revenue: 0, roi: 0, costPerEmail: 0, revenuePerEmail: 0, costPerConversion: 0 },
    abTest: { variant: 'ROI calculator vs feature comparison', winner: null }
  }
];

const MONTHLY_TRENDS = [
  { month: 'Apr', revenue: 32000, cost: 1200, campaigns: 4 },
  { month: 'May', revenue: 41000, cost: 1450, campaigns: 5 },
  { month: 'Jun', revenue: 38000, cost: 1300, campaigns: 4 },
  { month: 'Jul', revenue: 52000, cost: 1800, campaigns: 6 },
  { month: 'Aug', revenue: 99860, cost: 1709, campaigns: 6 }
];

const REVENUE_BY_TYPE = [
  { type: 'Promotional', revenue: 38530, percentage: 38.6, campaigns: 3 },
  { type: 'Transactional', revenue: 59060, percentage: 59.1, campaigns: 2 },
  { type: 'Newsletter', revenue: 2670, percentage: 2.7, campaigns: 1 }
];

const TOP_SEGMENTS = [
  { name: 'New Signups', roi: 11641, revenue: 10460, conversionRate: 5.9, openRate: 58.8 },
  { name: 'Active Buyers', roi: 5933, revenue: 27150, conversionRate: 4.0, openRate: 41.0 },
  { name: 'All Subscribers', roi: 7739, revenue: 48600, conversionRate: 4.0, openRate: 49.0 },
  { name: 'Engaged Subscribers', roi: 1383, revenue: 2670, conversionRate: 0.9, openRate: 39.2 },
  { name: 'Inactive 90d', roi: 1645, revenue: 3840, conversionRate: 1.2, openRate: 27.0 },
  { name: 'Free Tier Active', roi: 0, revenue: 0, conversionRate: 0, openRate: 0 }
];

// ─── HELPER COMPONENTS ───────────────────────────────────────────────────────

const StatCard = ({ label, value, subtitle, color = 'primary.main', icon, trend }) => (
  <Paper sx={{ p: 3, textAlign: 'center', background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
    <Typography variant="caption" sx={{ fontWeight: 700, color: 'text.secondary', letterSpacing: 0.5 }}>{label}</Typography>
    <Typography variant="h4" sx={{ mt: 1, fontWeight: 800, color }}>{icon} {value}</Typography>
    {subtitle && <Typography variant="caption" sx={{ display: 'block', mt: 0.5, color: 'text.secondary' }}>{subtitle}</Typography>}
    {trend && (
      <Chip size="small" label={trend} sx={{ mt: 1, fontSize: '0.65rem', height: 18, fontWeight: 700, bgcolor: trend.startsWith('+') ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)', color: trend.startsWith('+') ? '#10b981' : '#ef4444' }} />
    )}
  </Paper>
);

const FunnelBar = ({ label, value, max, color, rate }) => (
  <Box sx={{ mb: 2 }}>
    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
      <Typography variant="body2" sx={{ fontWeight: 600 }}>{label}</Typography>
      <Typography variant="body2" sx={{ fontWeight: 700, color }}>{value.toLocaleString()} {rate && `(${rate})`}</Typography>
    </Box>
    <Box sx={{ width: '100%', height: 24, bgcolor: 'rgba(255,255,255,0.04)', borderRadius: 2, overflow: 'hidden', position: 'relative' }}>
      <Box sx={{ width: `${(value / max) * 100}%`, height: '100%', background: `linear-gradient(90deg, ${color}, ${color}88)`, borderRadius: 2, transition: 'width 0.8s ease', display: 'flex', alignItems: 'center', justifyContent: 'flex-end', pr: 1 }}>
        {value / max > 0.15 && <Typography variant="caption" sx={{ color: 'white', fontWeight: 700, fontSize: '0.7rem' }}>{((value / max) * 100).toFixed(0)}%</Typography>}
      </Box>
    </Box>
  </Box>
);

const HorizontalBarChart = ({ data, height = 200 }) => {
  const maxVal = Math.max(...data.map(d => d.value));
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
      {data.map((d, i) => (
        <Box key={i}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
            <Typography variant="caption" sx={{ fontWeight: 600 }}>{d.label}</Typography>
            <Typography variant="caption" sx={{ fontWeight: 700, color: d.color }}>{d.display || d.value.toLocaleString()}</Typography>
          </Box>
          <Box sx={{ width: '100%', height: 14, bgcolor: 'rgba(255,255,255,0.04)', borderRadius: 7, overflow: 'hidden' }}>
            <Box sx={{ width: `${(d.value / maxVal) * 100}%`, height: '100%', background: `linear-gradient(90deg, ${d.color}, ${d.color}88)`, borderRadius: 7, transition: 'width 0.8s ease' }} />
          </Box>
        </Box>
      ))}
    </Box>
  );
};

// ─── MAIN COMPONENT ──────────────────────────────────────────────────────────

export default function CampaignROIDashboard() {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [activeTab, setActiveTab] = useState(0);
  const [selectedCampaign, setSelectedCampaign] = useState(null);
  const [periodFilter, setPeriodFilter] = useState('all');

  // ─── AGGREGATE METRICS ─────────────────────────────────────────────────
  const completedCampaigns = CAMPAIGNS.filter(c => c.status === 'completed' || c.status === 'running');
  const totalSent = completedCampaigns.reduce((s, c) => s + c.metrics.sent, 0);
  const totalRevenue = completedCampaigns.reduce((s, c) => s + c.financial.revenue, 0);
  const totalCost = completedCampaigns.reduce((s, c) => s + c.financial.cost, 0);
  const totalConversions = completedCampaigns.reduce((s, c) => s + c.metrics.converted, 0);
  const avgROI = totalCost > 0 ? Math.round(((totalRevenue - totalCost) / totalCost) * 100) : 0;
  const avgOpenRate = totalSent > 0 ? ((completedCampaigns.reduce((s, c) => s + c.metrics.opened, 0) / totalSent) * 100).toFixed(1) : 0;
  const avgConversionRate = totalSent > 0 ? ((totalConversions / totalSent) * 100).toFixed(2) : 0;
  const avgRevenuePerEmail = totalSent > 0 ? (totalRevenue / totalSent).toFixed(3) : 0;

  const filteredCampaigns = useMemo(() => {
    return CAMPAIGNS.filter(c => {
      const matchPeriod = periodFilter === 'all' ||
        (periodFilter === 'this_month' && c.sentDate?.startsWith('2026-08')) ||
        (periodFilter === 'last_month' && c.sentDate?.startsWith('2026-07'));
      return matchPeriod;
    });
  }, [periodFilter]);

  // ─── RENDER: OVERVIEW ──────────────────────────────────────────────────
  const renderOverview = () => (
    <Box>
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={6} md={3}><StatCard label="Total Revenue" value={`$${totalRevenue.toLocaleString()}`} subtitle="across all campaigns" color="#10b981" trend="+47% vs Jul" /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Total Spend" value={`$${totalCost.toLocaleString()}`} subtitle="email platform costs" color="#f59e0b" /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Average ROI" value={`${avgROI}%`} subtitle="return on investment" color="#6366f1" trend="+12% vs Jul" /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Conversions" value={totalConversions.toLocaleString()} subtitle={`${avgConversionRate}% conv. rate`} color="#8b5cf6" /></Grid>
      </Grid>

      {/* Revenue Funnel */}
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📊 Campaign Funnel (All Campaigns)</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <FunnelBar label="Sent" value={totalSent} max={totalSent} color="#6366f1" rate="100%" />
        <FunnelBar label="Delivered" value={completedCampaigns.reduce((s, c) => s + c.metrics.delivered, 0)} max={totalSent} color="#818cf8" rate={`${((completedCampaigns.reduce((s, c) => s + c.metrics.delivered, 0) / totalSent) * 100).toFixed(1)}%`} />
        <FunnelBar label="Opened" value={completedCampaigns.reduce((s, c) => s + c.metrics.opened, 0)} max={totalSent} color="#10b981" rate={`${avgOpenRate}%`} />
        <FunnelBar label="Clicked" value={completedCampaigns.reduce((s, c) => s + c.metrics.clicked, 0)} max={totalSent} color="#f59e0b" rate={`${((completedCampaigns.reduce((s, c) => s + c.metrics.clicked, 0) / totalSent) * 100).toFixed(1)}%`} />
        <FunnelBar label="Converted" value={totalConversions} max={totalSent} color="#ec4899" rate={`${avgConversionRate}%`} />
      </Paper>

      {/* Monthly Revenue Trend */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
            <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📈 Monthly Revenue Trend</Typography>
            <Divider sx={{ mb: 3, opacity: 0.2 }} />
            <svg width="100%" height={160} viewBox="0 0 500 160">
              {MONTHLY_TRENDS.map((m, i) => {
                const x = i * 100 + 50;
                const maxRev = Math.max(...MONTHLY_TRENDS.map(t => t.revenue));
                const barH = (m.revenue / maxRev) * 120;
                const costH = (m.cost / maxRev) * 120;
                return (
                  <g key={i}>
                    <rect x={x - 25} y={140 - barH} width="22" height={barH} rx="4" fill="#6366f1" opacity="0.8" />
                    <rect x={x + 3} y={140 - costH} width="22" height={costH} rx="4" fill="#f59e0b" opacity="0.7" />
                    <text x={x - 12} y={135 - barH} textAnchor="middle" fontSize="8" fill="#818cf8" fontWeight="700">${(m.revenue / 1000).toFixed(0)}k</text>
                    <text x={x} y={155} textAnchor="middle" fontSize="9" fill="#94a3b8" fontWeight="600">{m.month}</text>
                  </g>
                );
              })}
            </svg>
            <Box sx={{ display: 'flex', gap: 3, mt: 1, justifyContent: 'center' }}>
              {[{ label: 'Revenue', color: '#6366f1' }, { label: 'Cost', color: '#f59e0b' }].map(l => (
                <Box key={l.label} sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  <Box sx={{ width: 10, height: 10, borderRadius: 1, bgcolor: l.color }} />
                  <Typography variant="caption" sx={{ fontWeight: 600, color: 'text.secondary' }}>{l.label}</Typography>
                </Box>
              ))}
            </Box>
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
            <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>💰 Revenue by Campaign Type</Typography>
            <Divider sx={{ mb: 3, opacity: 0.2 }} />
            {REVENUE_BY_TYPE.map(r => (
              <Box key={r.type} sx={{ mb: 3 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                  <Typography variant="body2" sx={{ fontWeight: 700 }}>{r.type}</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 700, color: '#10b981' }}>${r.revenue.toLocaleString()} ({r.percentage}%)</Typography>
                </Box>
                <LinearProgress variant="determinate" value={r.percentage} sx={{ height: 12, borderRadius: 6, bgcolor: 'rgba(255,255,255,0.04)', '& .MuiLinearProgress-bar': { borderRadius: 6, background: `linear-gradient(90deg, ${r.type === 'Transactional' ? '#6366f1' : r.type === 'Promotional' ? '#10b981' : '#f59e0b'}, ${r.type === 'Transactional' ? '#818cf8' : r.type === 'Promotional' ? '#34d399' : '#fbbf24'})` } }} />
                <Typography variant="caption" sx={{ color: 'text.secondary' }}>{r.campaigns} campaigns</Typography>
              </Box>
            ))}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );

  // ─── RENDER: CAMPAIGNS ─────────────────────────────────────────────────
  const renderCampaigns = () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
        {['all', 'this_month', 'last_month'].map(p => (
          <Chip key={p} label={p === 'all' ? 'All Time' : p === 'this_month' ? 'This Month' : 'Last Month'}
            onClick={() => setPeriodFilter(p)} color={periodFilter === p ? 'primary' : 'default'}
            variant={periodFilter === p ? 'filled' : 'outlined'} sx={{ fontWeight: 600, cursor: 'pointer' }} />
        ))}
      </Box>

      <Grid container spacing={3}>
        {filteredCampaigns.map(camp => (
          <Grid item xs={12} key={camp.id}>
            <Card sx={{ background: 'rgba(255,255,255,0.03)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.05)', cursor: 'pointer', transition: 'all 0.2s', '&:hover': { borderColor: '#6366f1' } }}
              onClick={() => setSelectedCampaign(selectedCampaign === camp.id ? null : camp.id)}>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <Typography variant="h6" sx={{ fontWeight: 700, fontSize: '1rem' }}>{camp.name}</Typography>
                    <Chip size="small" label={camp.type} sx={{ textTransform: 'capitalize', fontSize: '0.65rem', height: 20, fontWeight: 600, bgcolor: 'rgba(99,102,241,0.12)', color: '#6366f1' }} />
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Chip size="small" label={camp.status} sx={{ textTransform: 'capitalize', fontWeight: 600, bgcolor: camp.status === 'completed' ? 'rgba(16,185,129,0.15)' : camp.status === 'running' ? 'rgba(99,102,241,0.15)' : 'rgba(148,163,184,0.15)', color: camp.status === 'completed' ? '#10b981' : camp.status === 'running' ? '#6366f1' : '#94a3b8' }} />
                    <Typography variant="h6" sx={{ fontWeight: 800, color: '#10b981' }}>${camp.financial.revenue.toLocaleString()}</Typography>
                  </Box>
                </Box>

                <Grid container spacing={2} sx={{ mt: 1 }}>
                  {[
                    { label: 'ROI', value: `${camp.financial.roi}%`, color: '#6366f1' },
                    { label: 'Sent', value: camp.metrics.sent.toLocaleString(), color: '#94a3b8' },
                    { label: 'Open Rate', value: camp.metrics.sent > 0 ? `${((camp.metrics.opened / camp.metrics.sent) * 100).toFixed(1)}%` : '0%', color: '#10b981' },
                    { label: 'Conv.', value: camp.metrics.converted.toLocaleString(), color: '#f59e0b' },
                    { label: 'Cost', value: `$${camp.financial.cost}`, color: '#ef4444' },
                    { label: 'Rev/Email', value: `$${camp.financial.revenuePerEmail}`, color: '#8b5cf6' }
                  ].map(m => (
                    <Grid item xs={4} md={2} key={m.label}>
                      <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>{m.label}</Typography>
                      <Typography variant="body2" sx={{ fontWeight: 700, color: m.color }}>{m.value}</Typography>
                    </Grid>
                  ))}
                </Grid>

                {selectedCampaign === camp.id && (
                  <Box sx={{ mt: 3 }}>
                    <Divider sx={{ mb: 3, opacity: 0.2 }} />
                    <Grid container spacing={3}>
                      <Grid item xs={12} md={6}>
                        <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 2 }}>📊 Detailed Metrics</Typography>
                        {[
                          { label: 'Delivered', value: camp.metrics.delivered, rate: camp.metrics.sent > 0 ? `${((camp.metrics.delivered / camp.metrics.sent) * 100).toFixed(1)}%` : '' },
                          { label: 'Opened', value: camp.metrics.opened, rate: camp.metrics.delivered > 0 ? `${((camp.metrics.opened / camp.metrics.delivered) * 100).toFixed(1)}%` : '' },
                          { label: 'Clicked', value: camp.metrics.clicked, rate: camp.metrics.opened > 0 ? `${((camp.metrics.clicked / camp.metrics.opened) * 100).toFixed(1)}%` : '' },
                          { label: 'Converted', value: camp.metrics.converted, rate: camp.metrics.clicked > 0 ? `${((camp.metrics.converted / camp.metrics.clicked) * 100).toFixed(1)}%` : '' },
                          { label: 'Unsubscribed', value: camp.metrics.unsubscribed, rate: '' },
                          { label: 'Bounced', value: camp.metrics.bounced, rate: '' }
                        ].map(m => (
                          <Box key={m.label} sx={{ display: 'flex', justifyContent: 'space-between', py: 0.5, borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                            <Typography variant="body2" sx={{ color: 'text.secondary' }}>{m.label}</Typography>
                            <Typography variant="body2" sx={{ fontWeight: 700 }}>{m.value.toLocaleString()} {m.rate && <span style={{ color: '#6366f1', fontSize: '0.8rem' }}>({m.rate})</span>}</Typography>
                          </Box>
                        ))}
                      </Grid>
                      <Grid item xs={12} md={6}>
                        <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 2 }}>💰 Financial Breakdown</Typography>
                        {[
                          { label: 'Total Cost', value: `$${camp.financial.cost}`, color: '#ef4444' },
                          { label: 'Total Revenue', value: `$${camp.financial.revenue.toLocaleString()}`, color: '#10b981' },
                          { label: 'Net Profit', value: `$${(camp.financial.revenue - camp.financial.cost).toLocaleString()}`, color: '#6366f1' },
                          { label: 'ROI', value: `${camp.financial.roi}%`, color: '#f59e0b' },
                          { label: 'Cost per Email', value: `$${camp.financial.costPerEmail}`, color: '#94a3b8' },
                          { label: 'Cost per Conversion', value: `$${camp.financial.costPerConversion}`, color: '#8b5cf6' }
                        ].map(m => (
                          <Box key={m.label} sx={{ display: 'flex', justifyContent: 'space-between', py: 0.5, borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                            <Typography variant="body2" sx={{ color: 'text.secondary' }}>{m.label}</Typography>
                            <Typography variant="body2" sx={{ fontWeight: 700, color: m.color }}>{m.value}</Typography>
                          </Box>
                        ))}
                        {camp.abTest.winner && (
                          <Box sx={{ mt: 2, p: 2, borderRadius: 2, background: 'rgba(16,185,129,0.08)', border: '1px solid rgba(16,185,129,0.15)' }}>
                            <Typography variant="caption" sx={{ fontWeight: 700, color: '#10b981' }}>🧪 A/B Test Winner: {camp.abTest.winner}</Typography>
                          </Box>
                        )}
                      </Grid>
                    </Grid>
                  </Box>
                )}
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );

  // ─── RENDER: SEGMENTS ──────────────────────────────────────────────────
  const renderSegments = () => (
    <Box>
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>👥 ROI by Audience Segment</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <HorizontalBarChart
          data={TOP_SEGMENTS.filter(s => s.revenue > 0).map(s => ({
            label: s.name,
            value: s.roi,
            color: s.roi > 5000 ? '#10b981' : s.roi > 1000 ? '#6366f1' : '#f59e0b',
            display: `${s.roi}% ROI ($${s.revenue.toLocaleString()})`
          }))}
        />
      </Paper>

      <Grid container spacing={3} sx={{ mt: 4 }}>
        {TOP_SEGMENTS.filter(s => s.revenue > 0).map(seg => (
          <Grid item xs={12} sm={6} md={4} key={seg.name}>
            <Card sx={{ background: 'rgba(255,255,255,0.03)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.05)' }}>
              <CardContent>
                <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 1 }}>{seg.name}</Typography>
                <Grid container spacing={1.5}>
                  {[
                    { label: 'ROI', value: `${seg.roi}%`, color: '#6366f1' },
                    { label: 'Revenue', value: `$${seg.revenue.toLocaleString()}`, color: '#10b981' },
                    { label: 'Conv. Rate', value: `${seg.conversionRate}%`, color: '#f59e0b' },
                    { label: 'Open Rate', value: `${seg.openRate}%`, color: '#8b5cf6' }
                  ].map(m => (
                    <Grid item xs={6} key={m.label}>
                      <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>{m.label}</Typography>
                      <Typography variant="body2" sx={{ fontWeight: 700, color: m.color }}>{m.value}</Typography>
                    </Grid>
                  ))}
                </Grid>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );

  // ─── RENDER: OPTIMIZATION ──────────────────────────────────────────────
  const renderOptimization = () => (
    <Box>
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>🎯 ROI Optimization Recommendations</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        {[
          { title: 'Double down on onboarding drips', description: 'Onboarding campaigns have the highest ROI (11,641%). Consider extending to a 14-day drip with more touchpoints.', impact: 'high', potential: '+$15,000/mo' },
          { title: 'Shift promotional budget to transactional', description: 'Transactional emails generate 59% of revenue with only 35% of send volume. Increase product launch emails.', impact: 'high', potential: '+$22,000/mo' },
          { title: 'Reduce dormant re-engagement frequency', description: '90-day dormant segment shows 27% open rate and $1.15 cost per conversion. Consider 60-day window instead.', impact: 'medium', potential: '+$2,400/mo' },
          { title: 'A/B test send times for newsletters', description: 'Newsletter open rates (39.2%) are below average. Test Tuesday 9AM sends based on ISP data.', impact: 'medium', potential: '+$800/mo' },
          { title: 'Implement dynamic content blocks', description: 'Personalized onboarding outperforms generic by 28%. Apply personalization to all promotional campaigns.', impact: 'high', potential: '+$8,500/mo' }
        ].map((rec, i) => (
          <Box key={i} sx={{ mb: 3, p: 3, borderRadius: 2, background: rec.impact === 'high' ? 'rgba(16,185,129,0.06)' : 'rgba(245,158,11,0.06)', borderLeft: `4px solid ${rec.impact === 'high' ? '#10b981' : '#f59e0b'}` }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>{rec.title}</Typography>
              <Chip size="small" label={rec.potential} sx={{ fontWeight: 700, bgcolor: 'rgba(16,185,129,0.15)', color: '#10b981', fontSize: '0.7rem' }} />
            </Box>
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>{rec.description}</Typography>
            <Chip size="small" label={`${rec.impact} impact`} sx={{ mt: 1, fontSize: '0.65rem', height: 18, fontWeight: 600, bgcolor: rec.impact === 'high' ? 'rgba(16,185,129,0.12)' : 'rgba(245,158,11,0.12)', color: rec.impact === 'high' ? '#10b981' : '#f59e0b' }} />
          </Box>
        ))}
      </Paper>

      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📊 Cost Efficiency Benchmarking</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <Grid container spacing={3}>
          {[
            { metric: 'Cost per Email', yours: '$0.010', industry: '$0.015', status: 'better' },
            { metric: 'Cost per Open', yours: '$0.026', industry: '$0.042', status: 'better' },
            { metric: 'Cost per Click', yours: '$0.067', industry: '$0.098', status: 'better' },
            { metric: 'Cost per Conversion', yours: '$0.34', industry: '$0.52', status: 'better' },
            { metric: 'Revenue per Email', yours: '$0.44', industry: '$0.38', status: 'better' },
            { metric: 'Avg. ROI', yours: `${avgROI}%`, industry: '2,800%', status: 'better' }
          ].map(b => (
            <Grid item xs={12} sm={6} md={4} key={b.metric}>
              <Card sx={{ background: 'rgba(16,185,129,0.04)', borderRadius: 2, border: '1px solid rgba(16,185,129,0.1)' }}>
                <CardContent sx={{ textAlign: 'center' }}>
                  <Typography variant="caption" sx={{ fontWeight: 700, color: 'text.secondary' }}>{b.metric}</Typography>
                  <Box sx={{ display: 'flex', justifyContent: 'center', gap: 3, mt: 1 }}>
                    <Box>
                      <Typography variant="caption" sx={{ color: 'text.secondary' }}>You</Typography>
                      <Typography variant="h6" sx={{ fontWeight: 800, color: '#10b981' }}>{b.yours}</Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" sx={{ color: 'text.secondary' }}>Industry</Typography>
                      <Typography variant="h6" sx={{ fontWeight: 800, color: '#94a3b8' }}>{b.industry}</Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Paper>
    </Box>
  );

  // ─── MAIN RENDER ───────────────────────────────────────────────────────
  const tabs = ['Overview', 'Campaigns', 'Segments', 'Optimization'];

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h3" component="h1" gutterBottom sx={{ fontWeight: 800 }}>
          💰 Campaign ROI Dashboard
        </Typography>
        <Typography variant="body1" sx={{ color: 'text.secondary' }}>
          Track revenue, costs, and ROI across all email campaigns with financial analytics and optimization recommendations.
        </Typography>
      </Box>

      <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)} variant="scrollable" scrollButtons="auto"
        sx={{ mb: 4, '& .MuiTab-root': { fontWeight: 700, textTransform: 'none', fontSize: '0.9rem' } }}>
        {tabs.map((t, i) => <Tab key={i} label={t} />)}
      </Tabs>

      {activeTab === 0 && renderOverview()}
      {activeTab === 1 && renderCampaigns()}
      {activeTab === 2 && renderSegments()}
      {activeTab === 3 && renderOptimization()}
    </Box>
  );
}
