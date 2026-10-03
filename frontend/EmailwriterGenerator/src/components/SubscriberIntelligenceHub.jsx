import React, { useState, useMemo } from 'react';
import {
  Box, Typography, Paper, Grid, Card, CardContent, Button, Divider,
  Chip, TextField, IconButton, Tooltip, LinearProgress, Tab, Tabs,
  useMediaQuery, useTheme, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Avatar, Badge, Switch, FormControlLabel
} from '@mui/material';

// ─── MOCK DATA ───────────────────────────────────────────────────────────────

const SUBSCRIBERS = [
  { id: 1, name: 'Sarah Chen', email: 'sarah.chen@techcorp.io', status: 'active', plan: 'premium', score: 92, openRate: 78.5, clickRate: 42.3, ltv: 2840, joined: '2025-12-15', lastActive: '2026-08-27', tags: ['power-user', 'beta-tester', 'enterprise'], engagement: 'champion', avatar: '👩‍💻' },
  { id: 2, name: 'Marcus Johnson', email: 'marcus@startupxyz.com', status: 'active', plan: 'pro', score: 87, openRate: 65.2, clickRate: 38.1, ltv: 1920, joined: '2026-01-20', lastActive: '2026-08-26', tags: ['early-adopter', 'referral-source'], engagement: 'loyal', avatar: '👨‍💼' },
  { id: 3, name: 'Aisha Patel', email: 'aisha.p@digitalagency.co', status: 'active', plan: 'enterprise', score: 95, openRate: 82.1, clickRate: 51.7, ltv: 5680, joined: '2025-09-01', lastActive: '2026-08-27', tags: ['enterprise', 'high-value', 'advocate'], engagement: 'champion', avatar: '👩‍🔬' },
  { id: 4, name: 'David Kim', email: 'dkim@bigcorp.com', status: 'active', plan: 'pro', score: 71, openRate: 45.3, clickRate: 22.8, ltv: 1340, joined: '2026-03-10', lastActive: '2026-08-20', tags: ['mid-market'], engagement: 'average', avatar: '👨‍🎨' },
  { id: 5, name: 'Elena Rodriguez', email: 'elena@freelance.dev', status: 'active', plan: 'free', score: 34, openRate: 12.1, clickRate: 3.2, ltv: 0, joined: '2026-06-01', lastActive: '2026-07-15', tags: ['free-tier', 'at-risk'], engagement: 'at-risk', avatar: '👩‍🎤' },
  { id: 6, name: 'James Wright', email: 'j.wright@agency.io', status: 'active', plan: 'pro', score: 78, openRate: 55.8, clickRate: 31.2, ltv: 1680, joined: '2026-02-14', lastActive: '2026-08-25', tags: ['agency', 'consistent'], engagement: 'loyal', avatar: '🧑‍💼' },
  { id: 7, name: 'Priya Sharma', email: 'priya@saasstartup.com', status: 'active', plan: 'premium', score: 89, openRate: 72.4, clickRate: 39.8, ltv: 2560, joined: '2025-11-08', lastActive: '2026-08-27', tags: ['power-user', 'content-creator'], engagement: 'champion', avatar: '👩‍🏫' },
  { id: 8, name: 'Tom Bradley', email: 'tom.b@midsize.co', status: 'unsubscribed', plan: 'free', score: 8, openRate: 2.1, clickRate: 0.3, ltv: 120, joined: '2026-04-22', lastActive: '2026-06-10', tags: ['churned', 'cost-sensitive'], engagement: 'churned', avatar: '🧑‍🔧' },
  { id: 9, name: 'Lisa Chang', email: 'lisa@designstudio.com', status: 'active', plan: 'pro', score: 82, openRate: 61.7, clickRate: 35.4, ltv: 1780, joined: '2026-01-05', lastActive: '2026-08-26', tags: ['creative', 'engaged'], engagement: 'loyal', avatar: '👩‍🎤' },
  { id: 10, name: 'Raj Mehta', email: 'raj@enterprise.co', status: 'active', plan: 'enterprise', score: 91, openRate: 76.8, clickRate: 44.2, ltv: 4920, joined: '2025-10-20', lastActive: '2026-08-27', tags: ['enterprise', 'decision-maker'], engagement: 'champion', avatar: '👨‍🏫' }
];

const ENGAGEMENT_SEGMENTS = [
  { name: 'Champions', count: 4, percentage: 40, color: '#10b981', description: 'High open rate, high LTV, active daily', icon: '🏆' },
  { name: 'Loyal', count: 3, percentage: 30, color: '#6366f1', description: 'Consistent engagement, regular opens', icon: '💙' },
  { name: 'Average', count: 1, percentage: 10, color: '#f59e0b', description: 'Moderate engagement, some activity', icon: '📊' },
  { name: 'At Risk', count: 1, percentage: 10, color: '#f97316', description: 'Declining engagement, low activity', icon: '⚠️' },
  { name: 'Churned', count: 1, percentage: 10, color: '#ef4444', description: 'Unsubscribed or inactive 60+ days', icon: '💤' }
];

const JOURNEY_TOUCHPOINTS = [
  { id: 1, type: 'signup', label: 'Signed Up', date: '2025-12-15', channel: 'Website' },
  { id: 2, type: 'email', label: 'Opened Welcome Email', date: '2025-12-15', channel: 'Email' },
  { id: 3, type: 'click', label: 'Clicked Feature Guide', date: '2025-12-17', channel: 'Email' },
  { id: 4, type: 'upgrade', label: 'Upgraded to Pro', date: '2026-01-10', channel: 'In-App' },
  { id: 5, type: 'email', label: 'Opened Newsletter #28', date: '2026-02-05', channel: 'Email' },
  { id: 6, type: 'referral', label: 'Referred 3 Users', date: '2026-03-20', channel: 'Referral' },
  { id: 7, type: 'support', label: 'Contacted Support', date: '2026-04-12', channel: 'Chat' },
  { id: 8, type: 'upgrade', label: 'Upgraded to Premium', date: '2026-05-01', channel: 'In-App' },
  { id: 9, type: 'email', label: 'Opened Campaign #12', date: '2026-07-15', channel: 'Email' },
  { id: 10, type: 'purchase', label: 'Annual Renewal', date: '2026-08-15', channel: 'In-App' }
];

const HEALTH_METRICS = {
  totalSubscribers: 45200,
  activeSubscribers: 38400,
  growthRate: 8.2,
  churnRate: 2.1,
  avgOpenRate: 42.3,
  avgClickRate: 18.7,
  avgScore: 72,
  nudgesPending: 890,
  segmentsActive: 12,
  automationsRunning: 6
};

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

const ScoreRing = ({ score, size = 80, label }) => {
  const getColor = (s) => s >= 80 ? '#10b981' : s >= 60 ? '#6366f1' : s >= 40 ? '#f59e0b' : '#ef4444';
  const radius = (size - 12) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (score / 100) * circumference;
  const color = getColor(score);

  return (
    <Box sx={{ textAlign: 'center' }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="6" />
        <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke={color} strokeWidth="6"
          strokeDasharray={circumference} strokeDashoffset={offset} strokeLinecap="round"
          transform={`rotate(-90 ${size / 2} ${size / 2})`} style={{ transition: 'stroke-dashoffset 1s ease' }} />
        <text x={size / 2} y={size / 2 + 2} textAnchor="middle" fontSize={size * 0.22} fontWeight="800" fill={color}>{score}</text>
      </svg>
      {label && <Typography variant="caption" sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.7rem' }}>{label}</Typography>}
    </Box>
  );
};

const EngagementBar = ({ label, percentage, color }) => (
  <Box sx={{ mb: 2 }}>
    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
      <Typography variant="body2" sx={{ fontWeight: 600 }}>{label}</Typography>
      <Typography variant="body2" sx={{ fontWeight: 700, color }}>{percentage}%</Typography>
    </Box>
    <Box sx={{ width: '100%', height: 10, bgcolor: 'rgba(255,255,255,0.04)', borderRadius: 5, overflow: 'hidden' }}>
      <Box sx={{ width: `${percentage}%`, height: '100%', background: `linear-gradient(90deg, ${color}, ${color}88)`, borderRadius: 5, transition: 'width 0.8s ease' }} />
    </Box>
  </Box>
);

// ─── MAIN COMPONENT ──────────────────────────────────────────────────────────

export default function SubscriberIntelligenceHub() {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [activeTab, setActiveTab] = useState(0);
  const [selectedSubscriber, setSelectedSubscriber] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [engagementFilter, setEngagementFilter] = useState('all');

  const filteredSubscribers = useMemo(() => {
    return SUBSCRIBERS.filter(s => {
      const matchSearch = s.name.toLowerCase().includes(searchTerm.toLowerCase()) || s.email.toLowerCase().includes(searchTerm.toLowerCase());
      const matchEngagement = engagementFilter === 'all' || s.engagement === engagementFilter;
      return matchSearch && matchEngagement;
    });
  }, [searchTerm, engagementFilter]);

  // ─── RENDER: OVERVIEW ──────────────────────────────────────────────────
  const renderOverview = () => (
    <Box>
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={6} md={3}><StatCard label="Total Subscribers" value={HEALTH_METRICS.totalSubscribers.toLocaleString()} subtitle={`${HEALTH_METRICS.activeSubscribers.toLocaleString()} active`} color="primary.main" trend={`+${HEALTH_METRICS.growthRate}% growth`} /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Avg. Open Rate" value={`${HEALTH_METRICS.avgOpenRate}%`} subtitle="across all campaigns" color="#10b981" /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Avg. Score" value={HEALTH_METRICS.avgScore} subtitle="engagement score" color="#6366f1" /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Churn Rate" value={`${HEALTH_METRICS.churnRate}%`} subtitle="monthly churn" color="#ef4444" /></Grid>
      </Grid>

      {/* Engagement Distribution */}
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📊 Engagement Distribution</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <Grid container spacing={3}>
          {ENGAGEMENT_SEGMENTS.map(seg => (
            <Grid item xs={12} sm={6} md={2.4} key={seg.name}>
              <Card sx={{ background: `${seg.color}08`, borderRadius: 3, border: `1px solid ${seg.color}20`, textAlign: 'center', transition: 'all 0.2s', '&:hover': { borderColor: seg.color, transform: 'translateY(-2px)' } }}>
                <CardContent sx={{ py: 3 }}>
                  <Typography variant="h4" sx={{ mb: 1 }}>{seg.icon}</Typography>
                  <Typography variant="h5" sx={{ fontWeight: 800, color: seg.color }}>{seg.percentage}%</Typography>
                  <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>{seg.name}</Typography>
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>{seg.count} subscribers</Typography>
                  <Typography variant="caption" sx={{ display: 'block', mt: 1, color: 'text.secondary', fontSize: '0.7rem' }}>{seg.description}</Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Paper>

      {/* Quick Health Metrics */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
            <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📈 Growth Metrics</Typography>
            <Divider sx={{ mb: 3, opacity: 0.2 }} />
            {[
              { label: 'Monthly Growth Rate', value: `${HEALTH_METRICS.growthRate}%`, color: '#10b981' },
              { label: 'Monthly Churn Rate', value: `${HEALTH_METRICS.churnRate}%`, color: '#ef4444' },
              { label: 'Net Growth', value: `${(HEALTH_METRICS.growthRate - HEALTH_METRICS.churnRate).toFixed(1)}%`, color: '#6366f1' },
              { label: 'Active Segments', value: HEALTH_METRICS.segmentsActive, color: '#f59e0b' },
              { label: 'Running Automations', value: HEALTH_METRICS.automationsRunning, color: '#8b5cf6' }
            ].map(m => (
              <Box key={m.label} sx={{ display: 'flex', justifyContent: 'space-between', py: 1.5, borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.secondary' }}>{m.label}</Typography>
                <Typography variant="body2" sx={{ fontWeight: 700, color: m.color }}>{m.value}</Typography>
              </Box>
            ))}
          </Paper>
        </Grid>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
            <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>🔔 Pending Actions</Typography>
            <Divider sx={{ mb: 3, opacity: 0.2 }} />
            {[
              { label: 'Re-engagement Nudges', count: 890, color: '#f59e0b', icon: '📧' },
              { label: 'At-Risk Interventions', count: 234, color: '#f97316', icon: '⚠️' },
              { label: 'Win-Back Campaigns', count: 156, color: '#ef4444', icon: '🔄' },
              { label: 'Upgrade Opportunities', count: 420, color: '#6366f1', icon: '⬆️' },
              { label: 'Advocate Invitations', count: 89, color: '#10b981', icon: '🏆' }
            ].map(a => (
              <Box key={a.label} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', py: 1.5, borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Typography variant="body2">{a.icon}</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.secondary' }}>{a.label}</Typography>
                </Box>
                <Chip size="small" label={a.count} sx={{ fontWeight: 700, bgcolor: `${a.color}18`, color: a.color }} />
              </Box>
            ))}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );

  // ─── RENDER: SUBSCRIBERS ───────────────────────────────────────────────
  const renderSubscribers = () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
        <TextField size="small" placeholder="Search by name or email..." value={searchTerm} onChange={e => setSearchTerm(e.target.value)}
          sx={{ flex: 1, minWidth: 250 }} />
        {['all', 'champion', 'loyal', 'average', 'at-risk', 'churned'].map(e => (
          <Chip key={e} label={e.charAt(0).toUpperCase() + e.slice(1).replace('-', ' ')} onClick={() => setEngagementFilter(e)}
            color={engagementFilter === e ? 'primary' : 'default'} variant={engagementFilter === e ? 'filled' : 'outlined'}
            sx={{ fontWeight: 600, cursor: 'pointer' }} />
        ))}
      </Box>

      <Grid container spacing={3}>
        {filteredSubscribers.map(sub => (
          <Grid item xs={12} md={6} key={sub.id}>
            <Card sx={{ background: 'rgba(255,255,255,0.03)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.05)', cursor: 'pointer', transition: 'all 0.2s', '&:hover': { borderColor: '#6366f1', transform: 'translateY(-2px)' } }}
              onClick={() => setSelectedSubscriber(selectedSubscriber === sub.id ? null : sub.id)}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                  <Avatar sx={{ width: 48, height: 48, bgcolor: 'rgba(99,102,241,0.12)', fontSize: '1.5rem' }}>{sub.avatar}</Avatar>
                  <Box sx={{ flex: 1 }}>
                    <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>{sub.name}</Typography>
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>{sub.email}</Typography>
                  </Box>
                  <ScoreRing score={sub.score} size={60} />
                </Box>

                <Box sx={{ display: 'flex', gap: 0.5, mb: 1.5, flexWrap: 'wrap' }}>
                  <Chip size="small" label={sub.plan} sx={{ fontWeight: 600, bgcolor: sub.plan === 'enterprise' ? 'rgba(139,92,246,0.15)' : sub.plan === 'premium' ? 'rgba(99,102,241,0.15)' : sub.plan === 'pro' ? 'rgba(16,185,129,0.15)' : 'rgba(148,163,184,0.15)', textTransform: 'capitalize', fontSize: '0.7rem', height: 22 }} />
                  <Chip size="small" label={sub.engagement} sx={{ fontWeight: 600, textTransform: 'capitalize', fontSize: '0.7rem', height: 22, bgcolor: sub.engagement === 'champion' ? 'rgba(16,185,129,0.15)' : sub.engagement === 'loyal' ? 'rgba(99,102,241,0.15)' : sub.engagement === 'at-risk' ? 'rgba(249,115,22,0.15)' : sub.engagement === 'churned' ? 'rgba(239,68,68,0.15)' : 'rgba(245,158,11,0.15)' }} />
                  <Chip size="small" label={sub.status} sx={{ fontWeight: 600, textTransform: 'capitalize', fontSize: '0.7rem', height: 22, bgcolor: sub.status === 'active' ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)' }} />
                </Box>

                <Grid container spacing={1.5}>
                  {[
                    { label: 'Open Rate', value: `${sub.openRate}%`, color: '#10b981' },
                    { label: 'Click Rate', value: `${sub.clickRate}%`, color: '#6366f1' },
                    { label: 'LTV', value: `$${sub.ltv.toLocaleString()}`, color: '#f59e0b' }
                  ].map(m => (
                    <Grid item xs={4} key={m.label}>
                      <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>{m.label}</Typography>
                      <Typography variant="body2" sx={{ fontWeight: 700, color: m.color }}>{m.value}</Typography>
                    </Grid>
                  ))}
                </Grid>

                {selectedSubscriber === sub.id && (
                  <Box sx={{ mt: 2 }}>
                    <Divider sx={{ mb: 2, opacity: 0.2 }} />
                    <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>🏷️ Tags</Typography>
                    <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mb: 2 }}>
                      {sub.tags.map(tag => (
                        <Chip key={tag} size="small" label={tag} sx={{ fontSize: '0.65rem', height: 18, fontWeight: 600, bgcolor: 'rgba(99,102,241,0.1)', color: '#818cf8' }} />
                      ))}
                    </Box>
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                      Joined: {sub.joined} • Last Active: {sub.lastActive}
                    </Typography>
                  </Box>
                )}
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );

  // ─── RENDER: JOURNEY ───────────────────────────────────────────────────
  const renderJourney = () => (
    <Box>
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>🛤️ Subscriber Journey Timeline</Typography>
        <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>Example: Sarah Chen's engagement journey from signup to premium renewal</Typography>
        <Divider sx={{ mb: 4, opacity: 0.2 }} />

        <Box sx={{ position: 'relative', pl: 4 }}>
          {/* Vertical line */}
          <Box sx={{ position: 'absolute', left: 15, top: 0, bottom: 0, width: 2, bgcolor: 'rgba(99,102,241,0.2)' }} />

          {JOURNEY_TOUCHPOINTS.map((tp, i) => {
            const typeColors = { signup: '#6366f1', email: '#10b981', click: '#3b82f6', upgrade: '#f59e0b', referral: '#8b5cf6', support: '#ef4444', purchase: '#10b981' };
            const color = typeColors[tp.type] || '#94a3b8';
            return (
              <Box key={tp.id} sx={{ mb: 3, position: 'relative' }}>
                {/* Dot on timeline */}
                <Box sx={{ position: 'absolute', left: -29, top: 6, width: 12, height: 12, borderRadius: '50%', bgcolor: color, border: `2px solid ${color}40`, zIndex: 1 }} />
                <Card sx={{ background: `${color}08`, border: `1px solid ${color}15`, borderRadius: 2 }}>
                  <CardContent sx={{ py: 2, px: 2.5, '&:last-child': { pb: 2 } }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Box>
                        <Chip size="small" label={tp.type} sx={{ textTransform: 'capitalize', fontWeight: 600, fontSize: '0.65rem', height: 18, bgcolor: `${color}18`, color, mb: 0.5 }} />
                        <Typography variant="body2" sx={{ fontWeight: 700 }}>{tp.label}</Typography>
                      </Box>
                      <Box sx={{ textAlign: 'right' }}>
                        <Typography variant="caption" sx={{ color: 'text.secondary' }}>{tp.date}</Typography>
                        <Typography variant="caption" sx={{ display: 'block', color, fontWeight: 600 }}>{tp.channel}</Typography>
                      </Box>
                    </Box>
                  </CardContent>
                </Card>
              </Box>
            );
          })}
        </Box>
      </Paper>
    </Box>
  );

  // ─── RENDER: INSIGHTS ──────────────────────────────────────────────────
  const renderInsights = () => (
    <Box>
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {[
          { title: 'Churn Prediction', description: '234 subscribers show early churn signals (declining open rates, fewer clicks)', action: 'Trigger re-engagement sequence', severity: 'warning', color: '#f59e0b' },
          { title: 'Upgrade Candidates', description: '420 free-tier users have usage patterns matching pro plan thresholds', action: 'Send upgrade offer', severity: 'opportunity', color: '#6366f1' },
          { title: 'Advocate Program', description: '89 subscribers have NPS 9+ and 3+ referrals — invite to VIP program', action: 'Send advocate invitation', severity: 'positive', color: '#10b981' }
        ].map((insight, i) => (
          <Grid item xs={12} md={4} key={i}>
            <Card sx={{ background: `${insight.color}06`, borderRadius: 3, border: `1px solid ${insight.color}20`, height: '100%' }}>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1, color: insight.color }}>{insight.title}</Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>{insight.description}</Typography>
                <Button size="small" variant="outlined" sx={{ textTransform: 'none', fontWeight: 600, borderColor: insight.color, color: insight.color }}>
                  {insight.action}
                </Button>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📊 Score Distribution</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <Grid container spacing={2}>
          {[
            { range: '90-100', label: 'Elite', count: 3, color: '#10b981' },
            { range: '80-89', label: 'Strong', count: 4, color: '#6366f1' },
            { range: '70-79', label: 'Good', count: 2, color: '#3b82f6' },
            { range: '60-69', label: 'Average', count: 0, color: '#f59e0b' },
            { range: '40-59', label: 'Below Avg', count: 0, color: '#f97316' },
            { range: '0-39', label: 'Low', count: 1, color: '#ef4444' }
          ].map(b => (
            <Grid item xs={6} sm={4} md={2} key={b.range}>
              <Card sx={{ background: `${b.color}06`, borderRadius: 2, border: `1px solid ${b.color}15`, textAlign: 'center' }}>
                <CardContent sx={{ py: 2 }}>
                  <Typography variant="h4" sx={{ fontWeight: 800, color: b.color }}>{b.count}</Typography>
                  <Typography variant="caption" sx={{ fontWeight: 700, color: 'text.secondary' }}>{b.range}</Typography>
                  <Typography variant="caption" sx={{ display: 'block', color: b.color, fontWeight: 600, fontSize: '0.7rem' }}>{b.label}</Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Paper>
    </Box>
  );

  // ─── MAIN RENDER ───────────────────────────────────────────────────────
  const tabs = ['Overview', 'Subscribers', 'Journey', 'Insights'];

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h3" component="h1" gutterBottom sx={{ fontWeight: 800 }}>
          🧠 Subscriber Intelligence Hub
        </Typography>
        <Typography variant="body1" sx={{ color: 'text.secondary' }}>
          Deep subscriber analytics with engagement scoring, lifecycle journey mapping, and AI-powered retention insights.
        </Typography>
      </Box>

      <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)} variant="scrollable" scrollButtons="auto"
        sx={{ mb: 4, '& .MuiTab-root': { fontWeight: 700, textTransform: 'none', fontSize: '0.9rem' } }}>
        {tabs.map((t, i) => <Tab key={i} label={t} />)}
      </Tabs>

      {activeTab === 0 && renderOverview()}
      {activeTab === 1 && renderSubscribers()}
      {activeTab === 2 && renderJourney()}
      {activeTab === 3 && renderInsights()}
    </Box>
  );
}
