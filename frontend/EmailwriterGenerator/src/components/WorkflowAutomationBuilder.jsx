import React, { useState, useMemo } from 'react';
import {
  Box, Typography, Paper, Grid, Card, CardContent, Button, Divider,
  Chip, TextField, IconButton, Tooltip, LinearProgress, Tab, Tabs,
  Switch, FormControlLabel, useMediaQuery, useTheme, Dialog, DialogTitle,
  DialogContent, DialogActions, Select, MenuItem, FormControl, InputLabel,
  Badge, Avatar, Table, TableBody, TableCell, TableContainer, TableHead, TableRow
} from '@mui/material';

// ─── MOCK DATA ───────────────────────────────────────────────────────────────

const WORKFLOWS = [
  {
    id: 'wf-001', name: 'Welcome Series', status: 'active', trigger: 'new_subscriber',
    description: 'Onboard new subscribers with a 3-email drip sequence',
    steps: [
      { id: 1, type: 'trigger', name: 'New Subscriber', config: { event: 'signup', list: 'main' } },
      { id: 2, type: 'delay', name: 'Wait 0 min', config: { duration: 0, unit: 'minutes' } },
      { id: 3, type: 'action', name: 'Send Welcome Email', config: { template: 'welcome_v2', subject: 'Welcome to MailGenie! ✨' } },
      { id: 4, type: 'delay', name: 'Wait 2 days', config: { duration: 2, unit: 'days' } },
      { id: 5, type: 'condition', name: 'Opened Welcome?', config: { metric: 'opened', operator: 'equals', value: 'true' } },
      { id: 6, type: 'action', name: 'Send Feature Guide', config: { template: 'feature_guide', subject: 'Discover your power features' } },
      { id: 7, type: 'action', name: 'Send Upgrade Offer', config: { template: 'upgrade_offer', subject: 'Unlock premium — 30% off' } }
    ],
    stats: { totalRuns: 3420, activeNow: 156, completed: 3102, conversions: 428 },
    createdAt: '2026-08-15', lastTriggered: '2026-08-27T09:30:00Z'
  },
  {
    id: 'wf-002', name: 'Cart Abandonment Recovery', status: 'active', trigger: 'cart_abandoned',
    description: 'Recover lost sales with timed follow-up emails',
    steps: [
      { id: 1, type: 'trigger', name: 'Cart Abandoned', config: { event: 'cart_abandon', timeout: '30min' } },
      { id: 2, type: 'delay', name: 'Wait 1 hour', config: { duration: 1, unit: 'hours' } },
      { id: 3, type: 'action', name: 'Send Reminder', config: { template: 'cart_reminder', subject: 'You left something behind! 🛒' } },
      { id: 4, type: 'delay', name: 'Wait 1 day', config: { duration: 1, unit: 'days' } },
      { id: 5, type: 'condition', name: 'Purchased?', config: { metric: 'purchased', operator: 'equals', value: 'false' } },
      { id: 6, type: 'action', name: 'Send Discount Code', config: { template: 'discount_10', subject: 'Here\'s 10% off to complete your order' } }
    ],
    stats: { totalRuns: 8920, activeNow: 342, completed: 8210, conversions: 1567 },
    createdAt: '2026-08-10', lastTriggered: '2026-08-27T10:15:00Z'
  },
  {
    id: 'wf-003', name: 'Re-engagement Campaign', status: 'active', trigger: 'inactive_user',
    description: 'Win back users who haven\'t opened emails in 30+ days',
    steps: [
      { id: 1, type: 'trigger', name: 'User Inactive 30d', config: { event: 'inactivity', days: 30 } },
      { id: 2, type: 'action', name: 'Send We Miss You', config: { template: 'miss_you', subject: 'We miss you! Here\'s what\'s new' } },
      { id: 3, type: 'delay', name: 'Wait 3 days', config: { duration: 3, unit: 'days' } },
      { id: 4, type: 'condition', name: 'Re-engaged?', config: { metric: 'opened', operator: 'equals', value: 'true' } },
      { id: 5, type: 'action', name: 'Send Special Offer', config: { template: 'comeback_offer', subject: 'Exclusive offer just for you' } },
      { id: 6, type: 'action', name: 'Tag as Churned', config: { action: 'add_tag', tag: 'churn_risk' } }
    ],
    stats: { totalRuns: 5100, activeNow: 89, completed: 4800, conversions: 234 },
    createdAt: '2026-08-12', lastTriggered: '2026-08-27T06:00:00Z'
  },
  {
    id: 'wf-004', name: 'Post-Purchase Follow-up', status: 'paused', trigger: 'purchase_complete',
    description: 'Nurture new customers with tips and upsell opportunities',
    steps: [
      { id: 1, type: 'trigger', name: 'Purchase Complete', config: { event: 'purchase' } },
      { id: 2, type: 'action', name: 'Send Receipt + Tips', config: { template: 'purchase_tips', subject: 'Thank you! Here are tips to get started' } },
      { id: 3, type: 'delay', name: 'Wait 7 days', config: { duration: 7, unit: 'days' } },
      { id: 4, type: 'condition', name: 'Used Feature X?', config: { metric: 'feature_used', operator: 'equals', value: 'true' } },
      { id: 5, type: 'action', name: 'Send Advanced Guide', config: { template: 'advanced_guide', subject: 'Level up your workflow' } },
      { id: 6, type: 'action', name: 'Send Upsell', config: { template: 'upsell_premium', subject: 'Ready for the next level?' } }
    ],
    stats: { totalRuns: 2100, activeNow: 0, completed: 2100, conversions: 312 },
    createdAt: '2026-08-08', lastTriggered: '2026-08-25T14:00:00Z'
  },
  {
    id: 'wf-005', name: 'Newsletter Drip', status: 'draft', trigger: 'weekly_subscribe',
    description: 'Weekly digest with personalized content recommendations',
    steps: [
      { id: 1, type: 'trigger', name: 'Weekly Schedule', config: { event: 'schedule', cron: '0 9 * * 1' } },
      { id: 2, type: 'condition', name: 'Day = Monday?', config: { metric: 'day_of_week', operator: 'equals', value: '1' } },
      { id: 3, type: 'action', name: 'Send Weekly Digest', config: { template: 'weekly_digest', subject: 'Your weekly email insights 📊' } }
    ],
    stats: { totalRuns: 0, activeNow: 0, completed: 0, conversions: 0 },
    createdAt: '2026-08-26', lastTriggered: null
  }
];

const EXECUTION_LOGS = [
  { id: 1, workflow: 'Welcome Series', subscriber: 'john@example.com', step: 'Send Welcome Email', status: 'delivered', timestamp: '2026-08-27T10:30:00Z', duration: '1.2s' },
  { id: 2, workflow: 'Cart Abandonment', subscriber: 'sarah@shop.com', step: 'Send Reminder', status: 'delivered', timestamp: '2026-08-27T10:15:00Z', duration: '0.8s' },
  { id: 3, workflow: 'Welcome Series', subscriber: 'mike@test.io', step: 'Opened Welcome?', status: 'condition_true', timestamp: '2026-08-27T09:45:00Z', duration: '0.1s' },
  { id: 4, workflow: 'Re-engagement', subscriber: 'lisa@old.co', step: 'Send We Miss You', status: 'delivered', timestamp: '2026-08-27T09:00:00Z', duration: '1.5s' },
  { id: 5, workflow: 'Cart Abandonment', subscriber: 'alex@buy.net', step: 'Send Discount Code', status: 'bounced', timestamp: '2026-08-27T08:30:00Z', duration: '0.3s' },
  { id: 6, workflow: 'Welcome Series', subscriber: 'emma@new.org', step: 'Send Feature Guide', status: 'delivered', timestamp: '2026-08-27T08:00:00Z', duration: '1.1s' },
  { id: 7, workflow: 'Re-engagement', subscriber: 'chris@back.com', step: 'Tag as Churned', status: 'completed', timestamp: '2026-08-27T07:30:00Z', duration: '0.05s' },
  { id: 8, workflow: 'Cart Abandonment', subscriber: 'diana@shop.mx', step: 'Send Reminder', status: 'delivered', timestamp: '2026-08-27T07:00:00Z', duration: '0.9s' },
  { id: 9, workflow: 'Welcome Series', subscriber: 'frank@hello.co', step: 'Wait 2 days', status: 'waiting', timestamp: '2026-08-27T06:00:00Z', duration: '-' },
  { id: 10, workflow: 'Re-engagement', subscriber: 'grace@miss.io', step: 'Condition check', status: 'condition_false', timestamp: '2026-08-27T05:30:00Z', duration: '0.1s' }
];

const TRIGGER_TYPES = [
  { id: 'new_subscriber', name: 'New Subscriber', icon: '👤', description: 'When someone joins a mailing list' },
  { id: 'cart_abandoned', name: 'Cart Abandoned', icon: '🛒', description: 'When a user leaves items in cart' },
  { id: 'inactive_user', name: 'User Inactive', icon: '😴', description: 'When user is inactive for X days' },
  { id: 'purchase_complete', name: 'Purchase Complete', icon: '💳', description: 'After a successful purchase' },
  { id: 'weekly_subscribe', name: 'Weekly Schedule', icon: '📅', description: 'Recurring weekly trigger' },
  { id: 'tag_added', name: 'Tag Added', icon: '🏷️', description: 'When a specific tag is applied' },
  { id: 'link_clicked', name: 'Link Clicked', icon: '🔗', description: 'When subscriber clicks a tracked link' },
  { id: 'form_submitted', name: 'Form Submitted', icon: '📋', description: 'When a landing page form is completed' }
];

const ACTION_TYPES = [
  { id: 'send_email', name: 'Send Email', icon: '✉️', description: 'Send a template email to subscriber' },
  { id: 'add_tag', name: 'Add Tag', icon: '🏷️', description: 'Apply a tag to subscriber profile' },
  { id: 'remove_tag', name: 'Remove Tag', icon: '🗑️', description: 'Remove a tag from subscriber' },
  { id: 'update_field', name: 'Update Field', icon: '📝', description: 'Modify subscriber custom field' },
  { id: 'webhook', name: 'Webhook', icon: '🔗', description: 'Call an external API endpoint' },
  { id: 'notify_team', name: 'Notify Team', icon: '🔔', description: 'Send internal team notification' }
];

// ─── HELPER COMPONENTS ───────────────────────────────────────────────────────

const StatCard = ({ label, value, subtitle, color = 'primary.main', icon }) => (
  <Paper sx={{ p: 3, textAlign: 'center', background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
    <Typography variant="caption" sx={{ fontWeight: 700, color: 'text.secondary', letterSpacing: 0.5 }}>{label}</Typography>
    <Typography variant="h4" sx={{ mt: 1, fontWeight: 800, color }}>{icon} {value}</Typography>
    {subtitle && <Typography variant="caption" sx={{ display: 'block', mt: 0.5, color: 'text.secondary' }}>{subtitle}</Typography>}
  </Paper>
);

const StepNode = ({ step, index, isLast, theme }) => {
  const isDark = theme.palette.mode === 'dark';
  const colors = {
    trigger: { bg: 'rgba(99,102,241,0.12)', border: '#6366f1', text: '#818cf8' },
    delay: { bg: 'rgba(245,158,11,0.12)', border: '#f59e0b', text: '#fbbf24' },
    condition: { bg: 'rgba(236,72,153,0.12)', border: '#ec4899', text: '#f472b6' },
    action: { bg: 'rgba(16,185,129,0.12)', border: '#10b981', text: '#34d399' }
  };
  const icons = { trigger: '⚡', delay: '⏱️', condition: '🔀', action: '🎯' };
  const c = colors[step.type];

  return (
    <Box sx={{ display: 'flex', alignItems: 'stretch', mb: 0 }}>
      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mr: 2, minWidth: 40 }}>
        <Box sx={{ width: 36, height: 36, borderRadius: '50%', bgcolor: c.bg, border: `2px solid ${c.border}`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.9rem', fontWeight: 700, zIndex: 1 }}>
          {icons[step.type]}
        </Box>
        {!isLast && <Box sx={{ width: 2, flex: 1, bgcolor: isDark ? 'rgba(255,255,255,0.1)' : '#e2e8f0', my: 0.5 }} />}
      </Box>
      <Card sx={{ flex: 1, mb: 1.5, background: c.bg, border: `1px solid ${c.border}20`, borderRadius: 2 }}>
        <CardContent sx={{ py: 2, px: 2.5, '&:last-child': { pb: 2 } }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Box>
              <Chip size="small" label={step.type} sx={{ textTransform: 'capitalize', fontWeight: 600, fontSize: '0.65rem', height: 20, bgcolor: `${c.border}25`, color: c.text, mb: 0.5 }} />
              <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>{step.name}</Typography>
            </Box>
            <IconButton size="small" sx={{ color: 'text.secondary' }}>⋮</IconButton>
          </Box>
          {step.config && (
            <Box sx={{ mt: 1, display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
              {Object.entries(step.config).map(([k, v]) => (
                <Chip key={k} size="small" label={`${k}: ${v}`} sx={{ fontSize: '0.65rem', height: 18, bgcolor: 'rgba(255,255,255,0.06)', fontFamily: 'monospace' }} />
              ))}
            </Box>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

// ─── MAIN COMPONENT ──────────────────────────────────────────────────────────

export default function WorkflowAutomationBuilder() {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [activeTab, setActiveTab] = useState(0);
  const [selectedWorkflow, setSelectedWorkflow] = useState(null);
  const [statusFilter, setStatusFilter] = useState('all');
  const [logFilter, setLogFilter] = useState('all');
  const [showBuilder, setShowBuilder] = useState(false);
  const [newWorkflowName, setNewWorkflowName] = useState('');
  const [newWorkflowTrigger, setNewWorkflowTrigger] = useState('');

  // ─── OVERVIEW METRICS ──────────────────────────────────────────────────
  const totalWorkflows = WORKFLOWS.length;
  const activeWorkflows = WORKFLOWS.filter(w => w.status === 'active').length;
  const totalRuns = WORKFLOWS.reduce((s, w) => s + w.stats.totalRuns, 0);
  const totalConversions = WORKFLOWS.reduce((s, w) => s + w.stats.conversions, 0);
  const currentlyRunning = WORKFLOWS.reduce((s, w) => s + w.stats.activeNow, 0);
  const conversionRate = totalRuns > 0 ? ((totalConversions / totalRuns) * 100).toFixed(1) : 0;

  const filteredWorkflows = useMemo(() => {
    return WORKFLOWS.filter(w => statusFilter === 'all' || w.status === statusFilter);
  }, [statusFilter]);

  const filteredLogs = useMemo(() => {
    return EXECUTION_LOGS.filter(l => logFilter === 'all' || l.status.includes(logFilter));
  }, [logFilter]);

  // ─── RENDER: OVERVIEW ──────────────────────────────────────────────────
  const renderOverview = () => (
    <Box>
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={6} md={3}><StatCard label="Total Workflows" value={totalWorkflows} subtitle={`${activeWorkflows} active`} color="primary.main" /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Total Executions" value={totalRuns.toLocaleString()} subtitle="all time" color="#10b981" /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Conversions" value={totalConversions.toLocaleString()} subtitle={`${conversionRate}% rate`} color="#f59e0b" /></Grid>
        <Grid item xs={6} md={3}><StatCard label="Currently Running" value={currentlyRunning.toLocaleString()} subtitle="active subscribers" color="#8b5cf6" /></Grid>
      </Grid>

      {/* Workflow Cards */}
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>📋 All Workflows</Typography>
          <Button variant="contained" onClick={() => setShowBuilder(true)} sx={{ borderRadius: 2, fontWeight: 700, textTransform: 'none' }}>
            ➕ New Workflow
          </Button>
        </Box>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />

        <Box sx={{ display: 'flex', gap: 1, mb: 3 }}>
          {['all', 'active', 'paused', 'draft'].map(s => (
            <Chip key={s} label={s.charAt(0).toUpperCase() + s.slice(1)} onClick={() => setStatusFilter(s)}
              color={statusFilter === s ? 'primary' : 'default'} variant={statusFilter === s ? 'filled' : 'outlined'}
              sx={{ fontWeight: 600, cursor: 'pointer' }} />
          ))}
        </Box>

        <Grid container spacing={3}>
          {filteredWorkflows.map(wf => (
            <Grid item xs={12} md={6} key={wf.id}>
              <Card sx={{ background: 'rgba(255,255,255,0.03)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.05)', cursor: 'pointer', transition: 'all 0.2s', '&:hover': { borderColor: '#6366f1', transform: 'translateY(-2px)' } }}
                onClick={() => setSelectedWorkflow(selectedWorkflow === wf.id ? null : wf.id)}>
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                    <Typography variant="h6" sx={{ fontWeight: 700, fontSize: '1rem' }}>{wf.name}</Typography>
                    <Chip size="small" label={wf.status} sx={{ textTransform: 'capitalize', fontWeight: 600, bgcolor: wf.status === 'active' ? 'rgba(16,185,129,0.15)' : wf.status === 'paused' ? 'rgba(245,158,11,0.15)' : 'rgba(148,163,184,0.15)', color: wf.status === 'active' ? '#10b981' : wf.status === 'paused' ? '#f59e0b' : '#94a3b8' }} />
                  </Box>
                  <Typography variant="body2" sx={{ color: 'text.secondary', fontSize: '0.85rem', mb: 1.5 }}>{wf.description}</Typography>

                  <Grid container spacing={1.5}>
                    {[
                      { label: 'Runs', value: wf.stats.totalRuns.toLocaleString(), color: '#6366f1' },
                      { label: 'Active', value: wf.stats.activeNow, color: '#10b981' },
                      { label: 'Conversions', value: wf.stats.conversions, color: '#f59e0b' }
                    ].map(m => (
                      <Grid item xs={4} key={m.label}>
                        <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>{m.label}</Typography>
                        <Typography variant="body2" sx={{ fontWeight: 700, color: m.color }}>{m.value}</Typography>
                      </Grid>
                    ))}
                  </Grid>

                  {selectedWorkflow === wf.id && (
                    <Box sx={{ mt: 3 }}>
                      <Divider sx={{ mb: 2, opacity: 0.2 }} />
                      <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 2 }}>Workflow Steps ({wf.steps.length})</Typography>
                      {wf.steps.map((step, idx) => (
                        <StepNode key={step.id} step={step} index={idx} isLast={idx === wf.steps.length - 1} theme={theme} />
                      ))}
                      <Box sx={{ mt: 2, display: 'flex', gap: 1 }}>
                        <Button size="small" variant="outlined" sx={{ textTransform: 'none', borderRadius: 2, fontWeight: 600 }}>
                          {wf.status === 'active' ? '⏸️ Pause' : '▶️ Activate'}
                        </Button>
                        <Button size="small" variant="outlined" color="error" sx={{ textTransform: 'none', borderRadius: 2, fontWeight: 600 }}>
                          🗑️ Delete
                        </Button>
                      </Box>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Paper>
    </Box>
  );

  // ─── RENDER: BUILDER ───────────────────────────────────────────────────
  const renderBuilder = () => (
    <Box>
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>🔧 Visual Workflow Builder</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />

        <Grid container spacing={4}>
          {/* Toolbox */}
          <Grid item xs={12} md={3}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 2 }}>⚡ Triggers</Typography>
            {TRIGGER_TYPES.map(t => (
              <Card key={t.id} sx={{ mb: 1.5, background: 'rgba(99,102,241,0.06)', border: '1px solid rgba(99,102,241,0.15)', borderRadius: 2, cursor: 'grab', transition: 'all 0.2s', '&:hover': { borderColor: '#6366f1', transform: 'translateX(4px)' } }}>
                <CardContent sx={{ py: 1.5, px: 2, '&:last-child': { pb: 1.5 } }}>
                  <Typography variant="body2" sx={{ fontWeight: 700 }}>{t.icon} {t.name}</Typography>
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>{t.description}</Typography>
                </CardContent>
              </Card>
            ))}

            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 2, mt: 3 }}>🎯 Actions</Typography>
            {ACTION_TYPES.map(a => (
              <Card key={a.id} sx={{ mb: 1.5, background: 'rgba(16,185,129,0.06)', border: '1px solid rgba(16,185,129,0.15)', borderRadius: 2, cursor: 'grab', transition: 'all 0.2s', '&:hover': { borderColor: '#10b981', transform: 'translateX(4px)' } }}>
                <CardContent sx={{ py: 1.5, px: 2, '&:last-child': { pb: 1.5 } }}>
                  <Typography variant="body2" sx={{ fontWeight: 700 }}>{a.icon} {a.name}</Typography>
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>{a.description}</Typography>
                </CardContent>
              </Card>
            ))}
          </Grid>

          {/* Canvas */}
          <Grid item xs={12} md={9}>
            <Paper sx={{ p: 4, background: isDark ? 'rgba(0,0,0,0.3)' : 'rgba(248,250,252,0.8)', borderRadius: 3, border: '2px dashed rgba(99,102,241,0.3)', minHeight: 500 }}>
              <Box sx={{ textAlign: 'center', mb: 4 }}>
                <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>🎨 Build Your Workflow</Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>Drag triggers and actions from the toolbox to create your automation sequence</Typography>
              </Box>

              {/* Sample Workflow Preview */}
              <Box sx={{ maxWidth: 500, mx: 'auto' }}>
                <StepNode step={{ type: 'trigger', name: 'New Subscriber Signs Up', config: { event: 'signup', list: 'newsletter' } }} index={0} isLast={false} theme={theme} />
                <StepNode step={{ type: 'delay', name: 'Wait 5 minutes', config: { duration: 5, unit: 'minutes' } }} index={1} isLast={false} theme={theme} />
                <StepNode step={{ type: 'action', name: 'Send Welcome Email', config: { template: 'welcome_v3', subject: 'Welcome aboard! 🎉' } }} index={2} isLast={false} theme={theme} />
                <StepNode step={{ type: 'delay', name: 'Wait 2 days', config: { duration: 2, unit: 'days' } }} index={3} isLast={false} theme={theme} />
                <StepNode step={{ type: 'condition', name: 'Did they open?', config: { metric: 'opened', operator: 'equals', value: 'true' } }} index={4} isLast={false} theme={theme} />
                <StepNode step={{ type: 'action', name: 'Send Feature Guide', config: { template: 'feature_intro', subject: 'Unlock your superpowers' } }} index={5} isLast={true} theme={theme} />
              </Box>

              <Box sx={{ textAlign: 'center', mt: 4 }}>
                <Button variant="contained" sx={{ borderRadius: 2, fontWeight: 700, textTransform: 'none', px: 4 }}>
                  💾 Save Workflow
                </Button>
              </Box>
            </Paper>
          </Grid>
        </Grid>
      </Paper>
    </Box>
  );

  // ─── RENDER: LOGS ──────────────────────────────────────────────────────
  const renderLogs = () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 1, mb: 3, flexWrap: 'wrap' }}>
        {['all', 'delivered', 'bounced', 'condition', 'waiting'].map(s => (
          <Chip key={s} label={s.charAt(0).toUpperCase() + s.slice(1)} onClick={() => setLogFilter(s)}
            color={logFilter === s ? 'primary' : 'default'} variant={logFilter === s ? 'filled' : 'outlined'}
            sx={{ fontWeight: 600, cursor: 'pointer' }} />
        ))}
      </Box>

      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📜 Execution Logs ({filteredLogs.length})</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                {['Workflow', 'Subscriber', 'Step', 'Status', 'Duration', 'Time'].map(h => (
                  <TableCell key={h} sx={{ fontWeight: 700, color: 'text.secondary', fontSize: '0.8rem' }}>{h}</TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredLogs.map(log => {
                const statusColors = {
                  delivered: { bg: 'rgba(16,185,129,0.15)', color: '#10b981' },
                  bounced: { bg: 'rgba(239,68,68,0.15)', color: '#ef4444' },
                  condition_true: { bg: 'rgba(99,102,241,0.15)', color: '#6366f1' },
                  condition_false: { bg: 'rgba(245,158,11,0.15)', color: '#f59e0b' },
                  waiting: { bg: 'rgba(148,163,184,0.15)', color: '#94a3b8' },
                  completed: { bg: 'rgba(16,185,129,0.15)', color: '#10b981' }
                };
                const sc = statusColors[log.status] || statusColors.delivered;
                return (
                  <TableRow key={log.id} sx={{ '&:hover': { bgcolor: 'rgba(255,255,255,0.03)' } }}>
                    <TableCell sx={{ fontWeight: 600, fontSize: '0.85rem' }}>{log.workflow}</TableCell>
                    <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>{log.subscriber}</TableCell>
                    <TableCell sx={{ fontSize: '0.85rem' }}>{log.step}</TableCell>
                    <TableCell>
                      <Chip size="small" label={log.status.replace('_', ' ')} sx={{ textTransform: 'capitalize', fontWeight: 600, fontSize: '0.7rem', bgcolor: sc.bg, color: sc.color }} />
                    </TableCell>
                    <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>{log.duration}</TableCell>
                    <TableCell sx={{ fontSize: '0.8rem', color: 'text.secondary' }}>{new Date(log.timestamp).toLocaleTimeString()}</TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Box>
  );

  // ─── RENDER: TEMPLATES ─────────────────────────────────────────────────
  const renderTemplates = () => (
    <Box>
      <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)', mb: 4 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📦 Workflow Templates</Typography>
        <Divider sx={{ mb: 3, opacity: 0.2 }} />
        <Grid container spacing={3}>
          {[
            { name: '📬 Welcome Series', desc: '3-email onboarding drip for new subscribers', steps: 7, category: 'Onboarding' },
            { name: '🛒 Cart Recovery', desc: 'Recover abandoned carts with timed reminders', steps: 6, category: 'E-commerce' },
            { name: '😴 Re-engagement', desc: 'Win back inactive subscribers with special offers', steps: 6, category: 'Retention' },
            { name: '💳 Post-Purchase', desc: 'Nurture buyers with tips and upsell opportunities', steps: 6, category: 'E-commerce' },
            { name: '📅 Weekly Digest', desc: 'Automated weekly newsletter with curated content', steps: 3, category: 'Content' },
            { name: '🎉 Birthday Campaign', desc: 'Send personalized birthday messages and offers', steps: 5, category: 'Engagement' },
            { name: '📋 Lead Nurture', desc: 'Score and nurture leads through the funnel', steps: 8, category: 'Sales' },
            { name: '🔄 Feedback Loop', desc: 'Collect and act on customer feedback automatically', steps: 5, category: 'Support' }
          ].map((tpl, i) => (
            <Grid item xs={12} sm={6} md={4} key={i}>
              <Card sx={{ background: 'rgba(255,255,255,0.03)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.05)', transition: 'all 0.2s', '&:hover': { borderColor: '#6366f1', transform: 'translateY(-2px)' }, height: '100%' }}>
                <CardContent>
                  <Typography variant="h6" sx={{ fontWeight: 700, fontSize: '1rem', mb: 1 }}>{tpl.name}</Typography>
                  <Typography variant="body2" sx={{ color: 'text.secondary', fontSize: '0.85rem', mb: 2 }}>{tpl.desc}</Typography>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Chip size="small" label={tpl.category} sx={{ fontWeight: 600, fontSize: '0.7rem', bgcolor: 'rgba(99,102,241,0.12)', color: '#6366f1' }} />
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>{tpl.steps} steps</Typography>
                  </Box>
                  <Button size="small" variant="outlined" fullWidth sx={{ mt: 2, textTransform: 'none', borderRadius: 2, fontWeight: 600 }}>
                    Use Template
                  </Button>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Paper>
    </Box>
  );

  // ─── RENDER: ANALYTICS ─────────────────────────────────────────────────
  const renderAnalytics = () => (
    <Box>
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
            <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>🏆 Top Performing Workflows</Typography>
            <Divider sx={{ mb: 2, opacity: 0.2 }} />
            {WORKFLOWS.filter(w => w.stats.conversions > 0).sort((a, b) => b.stats.conversions - a.stats.conversions).map((wf, i) => (
              <Box key={wf.id} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', py: 1.5, borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                  <Typography variant="h6" sx={{ fontWeight: 800, color: i === 0 ? '#f59e0b' : i === 1 ? '#94a3b8' : '#cd7f32', minWidth: 24 }}>{i + 1}</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>{wf.name}</Typography>
                </Box>
                <Typography variant="body2" sx={{ fontWeight: 700, color: '#10b981' }}>{wf.stats.conversions}</Typography>
              </Box>
            ))}
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
            <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>📊 Conversion by Trigger</Typography>
            <Divider sx={{ mb: 2, opacity: 0.2 }} />
            {[
              { trigger: 'Cart Abandoned', rate: 17.6, count: 1567 },
              { trigger: 'Purchase Complete', rate: 14.9, count: 312 },
              { trigger: 'New Subscriber', rate: 12.5, count: 428 },
              { trigger: 'User Inactive', rate: 4.6, count: 234 }
            ].map(t => (
              <Box key={t.trigger} sx={{ mb: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                  <Typography variant="caption" sx={{ fontWeight: 600 }}>{t.trigger}</Typography>
                  <Typography variant="caption" sx={{ fontWeight: 700, color: '#6366f1' }}>{t.rate}%</Typography>
                </Box>
                <LinearProgress variant="determinate" value={t.rate * 5} sx={{ height: 8, borderRadius: 4, bgcolor: isDark ? 'rgba(255,255,255,0.06)' : '#e2e8f0', '& .MuiLinearProgress-bar': { borderRadius: 4, background: 'linear-gradient(90deg, #6366f1, #818cf8)' } }} />
                <Typography variant="caption" sx={{ color: 'text.secondary' }}>{t.count.toLocaleString()} conversions</Typography>
              </Box>
            ))}
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
            <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>⚡ Performance Metrics</Typography>
            <Divider sx={{ mb: 2, opacity: 0.2 }} />
            {[
              { label: 'Avg. Execution Time', value: '0.8s', color: '#10b981' },
              { label: 'Delivery Rate', value: '98.7%', color: '#6366f1' },
              { label: 'Bounce Rate', value: '1.3%', color: '#f59e0b' },
              { label: 'Active Subscribers', value: '487', color: '#8b5cf6' },
              { label: 'Steps Executed Today', value: '2,341', color: '#ec4899' }
            ].map(m => (
              <Box key={m.label} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', py: 1.5, borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.secondary' }}>{m.label}</Typography>
                <Typography variant="body2" sx={{ fontWeight: 700, color: m.color }}>{m.value}</Typography>
              </Box>
            ))}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );

  // ─── MAIN RENDER ───────────────────────────────────────────────────────
  const tabs = ['Overview', 'Builder', 'Logs', 'Templates', 'Analytics'];

  return (
    <Box>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h3" component="h1" gutterBottom sx={{ fontWeight: 800 }}>
          ⚡ Workflow Automation Builder
        </Typography>
        <Typography variant="body1" sx={{ color: 'text.secondary' }}>
          Design, deploy, and monitor email automation workflows with visual drag-and-drop builder and real-time execution logs.
        </Typography>
      </Box>

      <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)} variant="scrollable" scrollButtons="auto"
        sx={{ mb: 4, '& .MuiTab-root': { fontWeight: 700, textTransform: 'none', fontSize: '0.9rem' } }}>
        {tabs.map((t, i) => <Tab key={i} label={t} />)}
      </Tabs>

      {activeTab === 0 && renderOverview()}
      {activeTab === 1 && renderBuilder()}
      {activeTab === 2 && renderLogs()}
      {activeTab === 3 && renderTemplates()}
      {activeTab === 4 && renderAnalytics()}

      {/* New Workflow Dialog */}
      <Dialog open={showBuilder} onClose={() => setShowBuilder(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 700 }}>➕ Create New Workflow</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, mt: 2 }}>
            <TextField fullWidth label="Workflow Name" placeholder="e.g. Welcome Drip Campaign" value={newWorkflowName} onChange={e => setNewWorkflowName(e.target.value)} />
            <FormControl fullWidth>
              <InputLabel>Trigger Type</InputLabel>
              <Select value={newWorkflowTrigger} label="Trigger Type" onChange={e => setNewWorkflowTrigger(e.target.value)}>
                {TRIGGER_TYPES.map(t => (
                  <MenuItem key={t.id} value={t.id}>{t.icon} {t.name} — {t.description}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button onClick={() => setShowBuilder(false)} sx={{ textTransform: 'none', fontWeight: 600 }}>Cancel</Button>
          <Button variant="contained" onClick={() => setShowBuilder(false)} sx={{ textTransform: 'none', fontWeight: 700, borderRadius: 2 }}>
            Create Workflow
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
