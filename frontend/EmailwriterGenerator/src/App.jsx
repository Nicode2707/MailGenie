import { useState, useEffect } from 'react';
import './App.css';
import Footer from './components/Footer';
import AboutPage from './pages/AboutPage';
import ContactPage from './pages/ContactPage';
import GuidelinesPage from './pages/GuidelinesPage';
import HelpPage from './pages/HelpPage';
import TermsPage from './pages/TermsPage';
import PrivacyPage from './pages/PrivacyPage';
import SecurityPage from './pages/SecurityPage';
import DatabaseMigrationDashboard from './components/DatabaseMigrationDashboard';
import QuotaManagementDashboard from './components/QuotaManagementDashboard';
import EnterpriseTelemetryDashboard from './components/EnterpriseTelemetryDashboard';
import ABTestingAnalytics from './components/ABTestingAnalytics';
import ContextAwareTemplateList from './components/ContextAwareTemplateList';
import CampaignROIDashboard from './components/CampaignROIDashboard';
import WorkflowAutomationBuilder from './components/WorkflowAutomationBuilder';
import EmailQualityAnalyzerDashboard from './components/EmailQualityAnalyzerDashboard';

import {
  Box,
  Button,
  CircularProgress,
  Container,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
  Paper,
  Card,
  CardContent,
  Divider,
  IconButton,
  Switch,
  FormControlLabel,
  Grid,
  InputAdornment,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  useMediaQuery,
  Slider,
  Chip,
  Tooltip,
  Snackbar,
  Alert
} from '@mui/material';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import axios from 'axios';
import useStore from './store/useStore';

// Sidebar Drawer Width
const drawerWidth = 270;

function App() {
  const {
    isDarkMode,
    toggleTheme,
    setIsDarkMode,
    activeTab,
    setActiveTab,
    mobileOpen,
    setMobileOpen,
    toast,
    showToast,
    closeToast,
    backendUrl,
    setBackendUrl,
    backendOnline,
    setBackendOnline,
    provider,
    setProvider,
    model,
    setModel,
    tone,
    setTone,
    language,
    setLanguage,
    providerConfig,
    setProviderConfig,
    customTemplates,
    setCustomTemplates,
    historyList,
    setHistoryList,
    searchTerm,
    setSearchTerm,
    compareMode,
    setCompareMode,
    compareReplies,
    setCompareReplies,
    compareLoading,
    setCompareLoading,
    studioFormality,
    setStudioFormality,
    studioLength,
    setStudioLength,
    studioSignature,
    setStudioSignature,
    studioSalutation,
    setStudioSalutation,
    studioCustomInstruction,
    setStudioCustomInstruction
  } = useStore();

  // Local generation states
  const [emailContent, setEmailContent] = useState('');
  const [generatedReply, setGeneratedReply] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);

  // Local edit states
  const [editingId, setEditingId] = useState(null);
  const [tempComment, setTempComment] = useState('');
  const [newTemplateTitle, setNewTemplateTitle] = useState('');
  const [newTemplateBody, setNewTemplateBody] = useState('');

  // Mobile responsiveness
  const isMobile = useMediaQuery('(max-width: 900px)');

  // Material UI Custom Theme
  const theme = createTheme({
    palette: {
      mode: isDarkMode ? 'dark' : 'light',
      primary: {
        main: '#6366f1',
      },
      secondary: {
        main: '#c084fc',
      },
      background: {
        default: isDarkMode ? '#090d16' : '#f8fafc',
        paper: isDarkMode ? 'rgba(17, 24, 39, 0.75)' : 'rgba(255, 255, 255, 0.92)',
      },
      text: {
        primary: isDarkMode ? '#f8fafc' : '#0f172a',
        secondary: isDarkMode ? '#94a3b8' : '#64748b',
      },
    },
    typography: {
      fontFamily: "'Inter', sans-serif",
      h3: { fontFamily: "'Outfit', sans-serif", fontWeight: 800 },
      h4: { fontFamily: "'Outfit', sans-serif", fontWeight: 700 },
      h5: { fontFamily: "'Outfit', sans-serif", fontWeight: 650 },
      h6: { fontFamily: "'Outfit', sans-serif", fontWeight: 600 },
    },
  });

  // Sync theme and data on mount / backendUrl change
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', isDarkMode ? 'dark' : 'light');
    checkBackendHealth();
  }, [backendUrl]);

  // Check health and load data
  const checkBackendHealth = async () => {
    try {
      const response = await axios.get(`${backendUrl}/api/email/config`);
      setProviderConfig(response.data);
      setBackendOnline(true);
      // Auto select first configured provider
      if (response.data) {
        if (response.data.groq) setProvider('groq');
        else if (response.data.openai) setProvider('openai');
        else if (response.data.gemini) setProvider('gemini');
        else if (response.data.claude) setProvider('claude');
      }

      const historyResponse = await axios.get(`${backendUrl}/api/history`);
      setHistoryList(historyResponse.data);

      // Fetch templates from backend
      try {
        const templateResponse = await axios.get(`${backendUrl}/api/templates`);
        if (templateResponse.data && templateResponse.data.length > 0) {
          setCustomTemplates(templateResponse.data);
          localStorage.setItem('mailgenie_custom_templates', JSON.stringify(templateResponse.data));
        }
      } catch (tErr) {
        console.warn("Could not fetch templates from backend:", tErr);
      }
    } catch (err) {
      console.error("Backend health check failed:", err);
      setBackendOnline(false);
    }
  };

  const showNotification = (message, severity = 'info') => {
    showToast(message, severity);
  };

  const handleBackendUrlChange = (newUrl) => {
    setBackendUrl(newUrl);
  };

  // Generate Single AI Reply
  const handleSubmit = async () => {
    if (compareMode) {
      handleCompareSubmit();
      return;
    }

    setLoading(true);
    setError('');
    setGeneratedReply('');
    try {
      const response = await axios.post(`${backendUrl}/api/email/generate`, {
        emailContent,
        tone,
        provider,
        model,
        language
      });
      const rawData = response.data;
      const reply = typeof rawData === 'string' ? rawData : (rawData?.reply || JSON.stringify(rawData));

      if (!reply || reply.trim().startsWith("Error:") || reply.includes("API error") || reply.startsWith("Unexpected error")) {
        const errorMsg = reply || "Failed to generate AI reply. Please check API configurations.";
        setError(errorMsg);
        setGeneratedReply('');
        showNotification(errorMsg, 'error');
      } else {
        setGeneratedReply(reply);
        setError('');
        showNotification('AI Reply generated successfully!', 'success');

        // Refresh history list
        try {
          const histRes = await axios.get(`${backendUrl}/api/history`);
          setHistoryList(histRes.data);
        } catch (hErr) {
          console.warn("History refresh skipped:", hErr);
        }
      }
    } catch (err) {
      setError('Failed to generate email reply. Please check your backend connection and API configurations.');
      showNotification('Generation failed. Check backend connection.', 'error');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Generate Multi-Tone Comparisons
  const handleCompareSubmit = async () => {
    setCompareLoading(true);
    setError('');
    setCompareReplies({ professional: '', casual: '', persuasive: '' });

    try {
      const tonesToFetch = ['professional', 'casual', 'persuasive'];
      const requests = tonesToFetch.map(t =>
        axios.post(`${backendUrl}/api/email/generate`, {
          emailContent,
          tone: t,
          provider,
          model,
          language
        })
      );

      const results = await Promise.allSettled(requests);
      const newCompareState = {
        professional: results[0].status === 'fulfilled' ? results[0].value.data : 'Failed to draft',
        casual: results[1].status === 'fulfilled' ? results[1].value.data : 'Failed to draft',
        persuasive: results[2].status === 'fulfilled' ? results[2].value.data : 'Failed to draft'
      };

      setCompareReplies(newCompareState);
      showNotification('Generated multi-tone comparisons!', 'success');

      const histRes = await axios.get(`${backendUrl}/api/history`);
      setHistoryList(histRes.data);
    } catch (err) {
      setError('Failed to generate multi-tone comparison.');
      showNotification('Comparison generation failed.', 'error');
    } finally {
      setCompareLoading(false);
    }
  };

  // Save history comments
  const handleUpdateComment = async (id) => {
    try {
      await axios.put(`${backendUrl}/api/history/${id}/comment`, tempComment, {
        headers: { 'Content-Type': 'application/json' }
      });
      setEditingId(null);
      showNotification('Annotation updated.', 'success');
      const histRes = await axios.get(`${backendUrl}/api/history`);
      setHistoryList(histRes.data);
    } catch (err) {
      console.error("Failed to update comment:", err);
      showNotification('Failed to update annotation note.', 'error');
    }
  };

  // Delete history entry
  const handleDeleteHistory = async (id) => {
    if (window.confirm("Are you sure you want to delete this history record?")) {
      try {
        await axios.delete(`${backendUrl}/api/history/${id}`);
        showNotification('History entry removed.', 'info');
        const histRes = await axios.get(`${backendUrl}/api/history`);
        setHistoryList(histRes.data);
      } catch (err) {
        console.error("Failed to delete history:", err);
      }
    }
  };

  // Export History as JSON / CSV
  const handleExportHistory = (format = 'json') => {
    if (historyList.length === 0) {
      showNotification('No history logs to export.', 'warning');
      return;
    }

    let dataStr = '';
    let mimeType = '';
    let fileName = `mailgenie_history_${Date.now()}`;

    if (format === 'csv') {
      const headers = ['ID', 'Date', 'Provider', 'Tone', 'Language', 'Original Content', 'Generated Reply', 'Comment'];
      const rows = historyList.map(h => [
        h.id,
        `"${new Date(h.createdAt).toLocaleString()}"`,
        `"${h.provider || ''}"`,
        `"${h.tone || ''}"`,
        `"${h.language || ''}"`,
        `"${(h.originalContent || '').replace(/"/g, '""')}"`,
        `"${(h.generatedReply || '').replace(/"/g, '""')}"`,
        `"${(h.userComment || '').replace(/"/g, '""')}"`
      ]);
      dataStr = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
      mimeType = 'text/csv;charset=utf-8;';
      fileName += '.csv';
    } else {
      dataStr = JSON.stringify(historyList, null, 2);
      mimeType = 'application/json;charset=utf-8;';
      fileName += '.json';
    }

    const blob = new Blob([dataStr], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showNotification(`Exported history as ${format.toUpperCase()}!`, 'success');
  };

  // Templates CRUD
  const handleAddTemplate = async () => {
    if (!newTemplateTitle.trim() || !newTemplateBody.trim()) return;
    const newTpl = {
      title: newTemplateTitle,
      body: newTemplateBody
    };
    try {
      const response = await axios.post(`${backendUrl}/api/templates`, newTpl);
      const updated = [...customTemplates, response.data];
      setCustomTemplates(updated);
      localStorage.setItem('mailgenie_custom_templates', JSON.stringify(updated));
      setNewTemplateTitle('');
      setNewTemplateBody('');
      showNotification('New template saved successfully!', 'success');
    } catch (err) {
      console.error("Failed to save template:", err);
      showNotification('Failed to save template to backend.', 'error');
    }
  };

  const handleDeleteTemplate = async (id) => {
    if (window.confirm("Delete this template?")) {
      try {
        await axios.delete(`${backendUrl}/api/templates/${id}`);
        const updated = customTemplates.filter(t => t.id !== id);
        setCustomTemplates(updated);
        localStorage.setItem('mailgenie_custom_templates', JSON.stringify(updated));
        showNotification('Template deleted.', 'info');
      } catch (err) {
        console.error("Failed to delete template:", err);
        showNotification('Failed to delete template from backend.', 'error');
      }
    }
  };

  const handleUseTemplate = (body) => {
    setEmailContent(body);
    setActiveTab('generator');
    showNotification('Template loaded into generator!', 'info');
  };

  const startEditing = (id, comment) => {
    setEditingId(id);
    setTempComment(comment || '');
  };

  const handleCopy = (text = generatedReply) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    showNotification('Copied draft to clipboard!', 'success');
    setTimeout(() => setCopied(false), 2000);
  };

  // Text Analytics Calculations
  const wordCount = emailContent.trim() ? emailContent.trim().split(/\s+/).length : 0;
  const charCount = emailContent.length;
  const estReadingTime = Math.ceil(wordCount / 200);

  // Filter history based on search term
  const filteredHistory = historyList.filter((item) => {
    const term = searchTerm.toLowerCase();
    return (
      (item.originalContent && item.originalContent.toLowerCase().includes(term)) ||
      (item.generatedReply && item.generatedReply.toLowerCase().includes(term)) ||
      (item.userComment && item.userComment.toLowerCase().includes(term)) ||
      (item.tone && item.tone.toLowerCase().includes(term)) ||
      (item.provider && item.provider.toLowerCase().includes(term))
    );
  });

  // Computed Analytics Metrics
  const totalGenerated = historyList.length;
  const timeSavedMinutes = totalGenerated * 2;
  const toneUsage = historyList.reduce((acc, curr) => {
    const t = curr.tone || 'default';
    acc[t] = (acc[t] || 0) + 1;
    return acc;
  }, {});
  const favoriteTone = Object.keys(toneUsage).reduce((a, b) => toneUsage[a] > toneUsage[b] ? a : b, 'default');
  const providerUsage = historyList.reduce((acc, curr) => {
    const p = curr.provider || 'unknown';
    acc[p] = (acc[p] || 0) + 1;
    return acc;
  }, {});
  const primaryProvider = Object.keys(providerUsage).reduce((a, b) => providerUsage[a] > providerUsage[b] ? a : b, 'groq');

  const navigationItems = [
    { id: 'generator', label: '⚡ Email Generator', icon: '📝' },
    { id: 'studio', label: '🎨 Prompt Studio', icon: '✨' },
    { id: 'history', label: '📜 History Logs', icon: '⏳' },
    { id: 'templates', label: '📂 Saved Templates', icon: '📁' },
    { id: 'migrations', label: '🗄️ DB Migrations', icon: '🗄️' },
    { id: 'quotas', label: '🎛️ Quota Policies', icon: '🎛️' },
    { id: 'context_templates', label: '🧠 Context Templates', icon: '🧠' },
    { id: 'campaign_roi', label: '💰 Campaign ROI', icon: '💰' },
    { id: 'workflow_automation', label: '⚡ Workflows', icon: '⚡' },
    { id: 'ab_testing', label: '🧪 A/B Testing', icon: '🧪' },
    { id: 'analytics', label: '📊 Usage Analytics', icon: '📈' },
    { id: 'telemetry', label: '🚀 Enterprise Telemetry', icon: '🚀' },
    { id: 'quality', label: '📊 Quality Analyzer', icon: '📊' },
    { id: 'settings', label: '⚙️ Settings', icon: '⚙️' }
  ];

  // Drawer layout for sidebar
  const drawerContent = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Box sx={{ p: 3, borderBottom: '1px solid rgba(255, 255, 255, 0.08)' }}>
        <Typography variant="h5" component="div" className="app-title" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          💌 MailGenie
        </Typography>
        <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 650, letterSpacing: 0.5 }}>
          AI EMAIL WRITING SUITE v1.1
        </Typography>
      </Box>

      <List sx={{ px: 2, py: 3, flexGrow: 1 }}>
        {navigationItems.map((item) => (
          <ListItem key={item.id} disablePadding sx={{ mb: 1 }}>
            <ListItemButton
              onClick={() => {
                setActiveTab(item.id);
                if (isMobile) setMobileOpen(false);
              }}
              className={activeTab === item.id ? "sidebar-item-active" : "sidebar-item"}
              sx={{
                borderRadius: '12px',
                py: 1.4,
                px: 2,
                transition: 'all 0.2s ease'
              }}
            >
              <ListItemIcon sx={{ minWidth: 36, fontSize: '1.2rem' }}>
                {item.icon}
              </ListItemIcon>
              <ListItemText
                primary={item.label}
                primaryTypographyProps={{ fontSize: '0.95rem', fontWeight: activeTab === item.id ? 700 : 500 }}
              />
            </ListItemButton>
          </ListItem>
        ))}
      </List>

      <Box sx={{ p: 3, borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
        <FormControlLabel
          control={<Switch checked={isDarkMode} onChange={toggleTheme} color="primary" />}
          label={isDarkMode ? "🌙 Dark Mode" : "☀️ Light Mode"}
          sx={{ color: 'text.secondary', '.MuiTypography-root': { fontSize: '0.85rem', fontWeight: 600 } }}
        />
        <Box sx={{ mt: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
          <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: backendOnline ? 'success.main' : 'error.main' }} />
          <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>
            {backendOnline ? 'Backend Connected' : 'Backend Disconnected'}
          </Typography>
        </Box>
      </Box>
    </Box>
  );

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ display: 'flex', minHeight: '100vh' }}>

        {/* Responsive Mobile Drawer */}
        {isMobile && (
          <IconButton
            onClick={() => setMobileOpen(true)}
            sx={{ position: 'fixed', top: 16, left: 16, zIndex: 1100, bgcolor: 'background.paper', boxShadow: 1 }}
          >
            ☰
          </IconButton>
        )}

        <Drawer
          variant={isMobile ? "temporary" : "permanent"}
          open={isMobile ? mobileOpen : true}
          onClose={() => setMobileOpen(false)}
          sx={{
            width: drawerWidth,
            flexShrink: 0,
            [`& .MuiDrawer-paper`]: {
              width: drawerWidth,
              boxSizing: 'border-box',
              borderRight: '1px solid rgba(255, 255, 255, 0.08)',
              background: isDarkMode ? 'linear-gradient(180deg, #090d16 0%, #0e1628 100%)' : '#ffffff'
            },
          }}
        >
          {drawerContent}
        </Drawer>

        {/* Main Content Area */}
        <Box component="main" sx={{ flexGrow: 1, p: 4, width: { md: `calc(100% - ${drawerWidth}px)` }, pt: isMobile ? 8 : 4 }}>
          <Container maxWidth="lg">

            {/* TAB: GENERATOR */}
            {activeTab === 'generator' && (
              <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4, flexWrap: 'wrap', gap: 2 }}>
                  <Box>
                    <Typography variant="h3" component="h1" gutterBottom className="app-title">
                      ⚡ Email Generator
                    </Typography>
                    <Typography variant="body1" sx={{ color: 'text.secondary' }}>
                      Generate tailored drafts using advanced artificial intelligence models.
                    </Typography>
                  </Box>

                  <FormControlLabel
                    control={
                      <Switch
                        checked={compareMode}
                        onChange={(e) => setCompareMode(e.target.checked)}
                        color="secondary"
                      />
                    }
                    label={<Typography variant="body2" sx={{ fontWeight: 700 }}>⚡ Multi-Tone Comparison Mode</Typography>}
                  />
                </Box>

                <Grid container spacing={4}>
                  {/* Left Form */}
                  <Grid item xs={12} md={compareMode ? 5 : 7}>
                    <Paper className="glass-card" sx={{ p: 4 }}>
                      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                        <Box>
                          <TextField
                            fullWidth
                            multiline
                            rows={6}
                            variant='outlined'
                            label="Original Email Context / Prompt"
                            placeholder="Paste details of the email thread or type a prompt instruction here..."
                            value={emailContent}
                            onChange={(e) => setEmailContent(e.target.value)}
                            className="glow-input"
                          />
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1, px: 0.5 }}>
                            <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>
                              {charCount} chars • {wordCount} words
                            </Typography>
                            <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>
                              Est. read: ~{estReadingTime} min
                            </Typography>
                          </Box>
                        </Box>

                        <Grid container spacing={2}>
                          {/* Provider Selection */}
                          <Grid item xs={12} sm={4}>
                            <FormControl fullWidth>
                              <InputLabel>LLM Provider</InputLabel>
                              <Select
                                value={provider}
                                label="LLM Provider"
                                onChange={(e) => {
                                  setProvider(e.target.value);
                                  setModel('');
                                }}
                              >
                                <MenuItem value="groq" disabled={!providerConfig.groq && backendOnline}>
                                  ⚡ Groq {providerConfig.groq ? ' (Active)' : ''}
                                </MenuItem>
                                <MenuItem value="openai" disabled={!providerConfig.openai && backendOnline}>
                                  🧠 OpenAI {providerConfig.openai ? ' (Active)' : ''}
                                </MenuItem>
                                <MenuItem value="gemini" disabled={!providerConfig.gemini && backendOnline}>
                                  ♊ Gemini {providerConfig.gemini ? ' (Active)' : ''}
                                </MenuItem>
                                <MenuItem value="claude" disabled={!providerConfig.claude && backendOnline}>
                                  🦉 Claude {providerConfig.claude ? ' (Active)' : ''}
                                </MenuItem>
                              </Select>
                            </FormControl>
                          </Grid>

                          {/* Tone Selection */}
                          <Grid item xs={12} sm={4}>
                            <FormControl fullWidth disabled={compareMode}>
                              <InputLabel>Tone</InputLabel>
                              <Select
                                value={tone}
                                label="Tone"
                                onChange={(e) => setTone(e.target.value)}
                              >
                                <MenuItem value="">Default ⚙️</MenuItem>
                                <MenuItem value="professional">Professional 👔</MenuItem>
                                <MenuItem value="casual">Casual ☕</MenuItem>
                                <MenuItem value="friendly">Friendly 😊</MenuItem>
                                <MenuItem value="persuasive">Persuasive 🎯</MenuItem>
                                <MenuItem value="urgent">Urgent ⏰</MenuItem>
                                <MenuItem value="empathetic">Empathetic ❤️</MenuItem>
                              </Select>
                            </FormControl>
                          </Grid>

                          {/* Language Selection */}
                          <Grid item xs={12} sm={4}>
                            <FormControl fullWidth>
                              <InputLabel>Language</InputLabel>
                              <Select
                                value={language}
                                label="Language"
                                onChange={(e) => setLanguage(e.target.value)}
                              >
                                <MenuItem value="English">English 🇺🇸</MenuItem>
                                <MenuItem value="Spanish">Spanish 🇪🇸</MenuItem>
                                <MenuItem value="French">French 🇫🇷</MenuItem>
                                <MenuItem value="German">German 🇩🇪</MenuItem>
                                <MenuItem value="Italian">Italian 🇮🇹</MenuItem>
                                <MenuItem value="Japanese">Japanese 🇯🇵</MenuItem>
                                <MenuItem value="Chinese">Chinese 🇨🇳</MenuItem>
                                <MenuItem value="Hindi">Hindi 🇮🇳</MenuItem>
                              </Select>
                            </FormControl>
                          </Grid>
                        </Grid>

                        <TextField
                          fullWidth
                          size="small"
                          variant="outlined"
                          label="Custom Model Override (Optional)"
                          placeholder="e.g. llama-3.3-70b-versatile, gpt-4o-mini"
                          value={model}
                          onChange={(e) => setModel(e.target.value)}
                          className="glow-input"
                        />

                        <Button
                          variant='contained'
                          onClick={handleSubmit}
                          disabled={!emailContent || loading || compareLoading || !backendOnline}
                          fullWidth
                          size="large"
                          className="gradient-btn"
                          sx={{ py: 1.8 }}
                        >
                          {(loading || compareLoading) ? (
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                              <CircularProgress size={20} color="inherit" />
                              <span>{compareMode ? "Drafting Comparisons..." : "Drafting Reply..."}</span>
                            </Box>
                          ) : !backendOnline ? "Backend Offline" : compareMode ? "⚡ Generate 3 Tones Side-by-Side" : "Generate AI Reply"}
                        </Button>
                      </Box>

                      {error && (
                        <Typography color='error' sx={{ mt: 3, textAlign: 'center', fontWeight: 600 }}>
                          ⚠️ {error}
                        </Typography>
                      )}
                    </Paper>
                  </Grid>

                  {/* Right Output Area (Standard or Multi-Tone Comparison) */}
                  <Grid item xs={12} md={compareMode ? 7 : 5}>
                    {compareMode ? (
                      <Paper className="glass-card" sx={{ p: 3, height: '100%' }}>
                        <Typography variant="h6" gutterBottom sx={{ color: 'secondary.main', fontWeight: 700 }}>
                          ⚡ Multi-Tone Side-by-Side Comparison
                        </Typography>
                        <Divider sx={{ my: 2 }} />

                        <Grid container spacing={2}>
                          {[
                            { key: 'professional', label: '👔 Professional', text: compareReplies.professional },
                            { key: 'casual', label: '☕ Casual', text: compareReplies.casual },
                            { key: 'persuasive', label: '🎯 Persuasive', text: compareReplies.persuasive }
                          ].map(col => (
                            <Grid item xs={12} key={col.key}>
                              <Card variant="outlined" sx={{ borderRadius: 3, p: 2, bgcolor: isDarkMode ? 'rgba(0,0,0,0.2)' : '#ffffff' }}>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                                  <Typography variant="subtitle2" sx={{ fontWeight: 700, color: 'primary.main' }}>
                                    {col.label}
                                  </Typography>
                                  {col.text && (
                                    <Button size="small" variant="text" onClick={() => handleCopy(col.text)}>
                                      📋 Copy
                                    </Button>
                                  )}
                                </Box>
                                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', fontSize: '0.82rem', color: 'text.primary' }}>
                                  {col.text || <span style={{ opacity: 0.5, fontStyle: 'italic' }}>Awaiting comparison generation...</span>}
                                </Typography>
                              </Card>
                            </Grid>
                          ))}
                        </Grid>
                      </Paper>
                    ) : (
                      <Paper className="glass-card" sx={{ p: 4, height: '100%', minHeight: 350, display: 'flex', flexDirection: 'column' }}>
                        <Typography variant="h6" gutterBottom sx={{ color: 'primary.main', display: 'flex', alignItems: 'center', gap: 1 }}>
                          ✨ Generated Reply Draft
                        </Typography>
                        <Divider sx={{ my: 2 }} />

                        {generatedReply ? (
                          <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
                            <TextField
                              fullWidth
                              multiline
                              rows={10}
                              variant='outlined'
                              value={generatedReply}
                              inputProps={{ readOnly: true }}
                              sx={{
                                backgroundColor: isDarkMode ? 'rgba(0, 0, 0, 0.25)' : '#f8fafc',
                                borderRadius: 3,
                                flexGrow: 1
                              }}
                            />
                            <Button
                              variant={copied ? 'contained' : 'outlined'}
                              color={copied ? 'success' : 'primary'}
                              fullWidth
                              onClick={() => handleCopy(generatedReply)}
                              sx={{ borderRadius: 3, py: 1.5 }}
                            >
                              {copied ? '✅ Copied to Clipboard!' : '📋 Copy Draft'}
                            </Button>
                          </Box>
                        ) : (
                          <Box sx={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', flexGrow: 1, opacity: 0.6 }}>
                            <Typography variant="body1" sx={{ fontStyle: 'italic', textAlign: 'center' }}>
                              Configure inputs on the left and tap "Generate AI Reply" to craft your draft response here.
                            </Typography>
                          </Box>
                        )}
                      </Paper>
                    )}
                  </Grid>
                </Grid>
              </Box>
            )}

            {/* TAB: PROMPT STUDIO */}
            {activeTab === 'studio' && (
              <Box>
                <Box sx={{ mb: 4 }}>
                  <Typography variant="h3" component="h2" className="app-title">
                    🎨 Prompt Studio & Style Tuning
                  </Typography>
                  <Typography variant="body1" sx={{ color: 'text.secondary' }}>
                    Customize generation guidelines, salutations, signatures, and default formality sliders.
                  </Typography>
                </Box>

                <Grid container spacing={4}>
                  <Grid item xs={12} md={6}>
                    <Paper className="glass-card" sx={{ p: 4 }}>
                      <Typography variant="h6" gutterBottom sx={{ fontWeight: 700 }}>
                        🎛️ Formality & Structure Parameters
                      </Typography>
                      <Divider sx={{ my: 2 }} />

                      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                        <Box>
                          <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                            Formality Level: {studioFormality}%
                          </Typography>
                          <Slider
                            value={studioFormality}
                            onChange={(e, val) => setStudioFormality(val)}
                            valueLabelDisplay="auto"
                            color="primary"
                          />
                        </Box>

                        <TextField
                          fullWidth
                          size="small"
                          label="Default Salutation Prefix"
                          value={studioSalutation}
                          onChange={(e) => setStudioSalutation(e.target.value)}
                        />

                        <TextField
                          fullWidth
                          multiline
                          rows={3}
                          label="Default Signature Suffix"
                          value={studioSignature}
                          onChange={(e) => setStudioSignature(e.target.value)}
                        />

                        <TextField
                          fullWidth
                          multiline
                          rows={3}
                          label="Custom System Prompt Rules (Optional)"
                          placeholder="e.g. Always keep bullet points concise. Never use corporate buzzwords."
                          value={studioCustomInstruction}
                          onChange={(e) => setStudioCustomInstruction(e.target.value)}
                        />

                        <Button
                          variant="contained"
                          className="gradient-btn"
                          onClick={() => showNotification('Studio preferences saved locally!', 'success')}
                        >
                          Save Style Preset
                        </Button>
                      </Box>
                    </Paper>
                  </Grid>

                  <Grid item xs={12} md={6}>
                    <Paper className="glass-card" sx={{ p: 4, height: '100%' }}>
                      <Typography variant="h6" gutterBottom sx={{ fontWeight: 700, color: 'secondary.main' }}>
                        👁️ Live Prompt Construction Preview
                      </Typography>
                      <Divider sx={{ my: 2 }} />

                      <Box sx={{ p: 2.5, bgcolor: isDarkMode ? 'rgba(0,0,0,0.3)' : '#f1f5f9', borderRadius: 3, fontFamily: 'monospace', fontSize: '0.85rem', whiteSpace: 'pre-wrap' }}>
                        {`[SYSTEM INSTRUCTION]
Role: Professional Email Copywriter
Formality Index: ${studioFormality}/100
Salutation: "${studioSalutation}"
Signature: "${studioSignature.replace(/\n/g, ' ')}"
Custom Directives: ${studioCustomInstruction || 'None'}

[INPUT CONTEXT]
"${emailContent || 'Your input email text will appear here...'}"`}
                      </Box>
                    </Paper>
                  </Grid>
                </Grid>
              </Box>
            )}

            {/* TAB: HISTORY */}
            {activeTab === 'history' && (
              <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4, flexWrap: 'wrap', gap: 2 }}>
                  <Box>
                    <Typography variant="h3" component="h2" className="app-title">
                      📜 History & Logs
                    </Typography>
                    <Typography variant="body1" sx={{ color: 'text.secondary' }}>
                      View, search, edit annotations, and export your past AI-generated replies.
                    </Typography>
                  </Box>

                  <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center' }}>
                    <Button variant="outlined" size="small" onClick={() => handleExportHistory('csv')} sx={{ borderRadius: 2 }}>
                      📥 CSV
                    </Button>
                    <Button variant="outlined" size="small" onClick={() => handleExportHistory('json')} sx={{ borderRadius: 2 }}>
                      📥 JSON
                    </Button>
                    <TextField
                      size="small"
                      placeholder="Search history..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      sx={{ width: 220 }}
                      InputProps={{
                        startAdornment: <InputAdornment position="start">🔍</InputAdornment>,
                      }}
                    />
                  </Box>
                </Box>

                {!backendOnline ? (
                  <Paper className="glass-card" sx={{ py: 6, px: 3, textAlign: 'center' }}>
                    <Typography variant="body1" color="error" sx={{ fontWeight: 650 }}>
                      ⚠️ Connection Offline. Unable to load history from backend.
                    </Typography>
                  </Paper>
                ) : filteredHistory.length === 0 ? (
                  <Paper className="glass-card" sx={{ py: 8, px: 3, textAlign: 'center' }}>
                    <Typography variant="body1" sx={{ color: 'text.secondary', fontStyle: 'italic' }}>
                      {searchTerm ? "No matching records found." : "Your generated email records will be shown here."}
                    </Typography>
                  </Paper>
                ) : (
                  filteredHistory.map((item) => (
                    <Card key={item.id} className="history-card" sx={{ mb: 3, boxShadow: 'none' }}>
                      <CardContent sx={{ p: 3 }}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, alignItems: 'center' }}>
                            <span className={`badge-provider badge-${item.provider ? item.provider.toLowerCase() : 'groq'}`}>
                              {item.provider ? item.provider : 'Groq'}
                            </span>
                            {item.tone && (
                              <span className="badge-tone">
                                🎭 {item.tone}
                              </span>
                            )}
                            <span className="badge-lang">
                              🌐 {item.language ? item.language : 'English'}
                            </span>
                            <Typography variant="caption" sx={{ color: 'text.secondary', ml: 1, fontWeight: 600 }}>
                              {new Date(item.createdAt).toLocaleString()}
                            </Typography>
                          </Box>
                          <IconButton
                            color="error"
                            size="small"
                            onClick={() => handleDeleteHistory(item.id)}
                            title="Delete entry"
                          >
                            🗑️
                          </IconButton>
                        </Box>

                        <Typography variant="subtitle2" sx={{ fontWeight: 700, color: 'text.primary', mb: 0.5 }}>
                          ✉️ Original Context:
                        </Typography>
                        <Typography variant="body2" sx={{
                          whiteSpace: 'pre-wrap',
                          backgroundColor: isDarkMode ? 'rgba(255, 255, 255, 0.03)' : '#f8fafc',
                          p: 2,
                          borderRadius: 2.5,
                          mb: 2,
                          color: 'text.secondary',
                          fontSize: '0.85rem'
                        }}>
                          {item.originalContent}
                        </Typography>

                        <Typography variant="subtitle2" sx={{ fontWeight: 700, color: 'primary.main', mb: 0.5 }}>
                          🤖 AI Reply Draft:
                        </Typography>
                        <Typography variant="body2" sx={{
                          whiteSpace: 'pre-wrap',
                          backgroundColor: isDarkMode ? 'rgba(99, 102, 241, 0.08)' : '#eef2ff',
                          p: 2,
                          borderRadius: 2.5,
                          mb: 2,
                          color: 'text.primary',
                          fontSize: '0.85rem'
                        }}>
                          {item.generatedReply}
                        </Typography>

                        <Divider sx={{ my: 2, opacity: 0.3 }} />

                        <Box>
                          {editingId === item.id ? (
                            <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', mt: 1 }}>
                              <TextField
                                fullWidth
                                size="small"
                                placeholder="Edit your private comments/notes..."
                                value={tempComment}
                                onChange={(e) => setTempComment(e.target.value)}
                                className="glow-input"
                              />
                              <Button variant="contained" size="small" onClick={() => handleUpdateComment(item.id)} sx={{ px: 3 }}>
                                Save
                              </Button>
                              <Button variant="outlined" size="small" onClick={() => setEditingId(null)}>
                                Cancel
                              </Button>
                            </Box>
                          ) : (
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 1 }}>
                              <Typography variant="body2" sx={{ color: 'text.secondary', fontSize: '0.85rem' }}>
                                💬 <strong>Annotation Note:</strong> {item.userComment ? item.userComment : <span style={{ fontStyle: 'italic', opacity: 0.7 }}>None saved</span>}
                              </Typography>
                              <Button variant="text" size="small" onClick={() => startEditing(item.id, item.userComment)} sx={{ textTransform: 'none', fontWeight: 650 }}>
                                ✏️ Edit Note
                              </Button>
                            </Box>
                          )}
                        </Box>
                      </CardContent>
                    </Card>
                  ))
                )}
              </Box>
            )}

            {/* TAB: TEMPLATES */}
            {activeTab === 'templates' && (
              <Box>
                <Box sx={{ mb: 4 }}>
                  <Typography variant="h3" component="h2" className="app-title">
                    📂 Reply Templates
                  </Typography>
                  <Typography variant="body1" sx={{ color: 'text.secondary' }}>
                    Quickly launch pre-designed templates directly into the generator or save your own favorites.
                  </Typography>
                </Box>

                <Grid container spacing={4}>
                  {/* Create Template */}
                  <Grid item xs={12} md={4}>
                    <Paper className="glass-card" sx={{ p: 4 }}>
                      <Typography variant="h6" gutterBottom sx={{ fontWeight: 700 }}>
                        ➕ New Template
                      </Typography>
                      <Divider sx={{ my: 2 }} />
                      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                        <TextField
                          fullWidth
                          size="small"
                          label="Template Title"
                          placeholder="e.g. 🏢 Support Decline"
                          value={newTemplateTitle}
                          onChange={(e) => setNewTemplateTitle(e.target.value)}
                        />
                        <TextField
                          fullWidth
                          multiline
                          rows={6}
                          label="Template Body"
                          placeholder="Write template email draft structure here..."
                          value={newTemplateBody}
                          onChange={(e) => setNewTemplateBody(e.target.value)}
                        />
                        <Button
                          variant="contained"
                          onClick={handleAddTemplate}
                          disabled={!newTemplateTitle || !newTemplateBody}
                          fullWidth
                          className="gradient-btn"
                        >
                          Save Template
                        </Button>
                      </Box>
                    </Paper>
                  </Grid>

                  {/* Templates List */}
                  <Grid item xs={12} md={8}>
                    <Grid container spacing={3}>
                      {customTemplates.map((template) => (
                        <Grid item xs={12} sm={6} key={template.id}>
                          <Card className="history-card" sx={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', boxShadow: 'none' }}>
                            <CardContent sx={{ p: 3 }}>
                              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                                <Typography variant="h6" sx={{ fontSize: '1rem', fontWeight: 700 }}>
                                  {template.title}
                                </Typography>
                                <IconButton
                                  size="small"
                                  color="error"
                                  onClick={() => handleDeleteTemplate(template.id)}
                                >
                                  🗑️
                                </IconButton>
                              </Box>
                              <Typography
                                variant="body2"
                                sx={{
                                  color: 'text.secondary',
                                  whiteSpace: 'pre-wrap',
                                  mb: 2.5,
                                  fontSize: '0.82rem',
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis',
                                  display: '-webkit-box',
                                  WebkitLineClamp: 5,
                                  WebkitBoxOrient: 'vertical'
                                }}
                              >
                                {template.body}
                              </Typography>
                              <Button
                                variant="outlined"
                                size="small"
                                fullWidth
                                onClick={() => handleUseTemplate(template.body)}
                                sx={{ textTransform: 'none', borderRadius: 2 }}
                              >
                                ⚡ Load into Generator
                              </Button>
                            </CardContent>
                          </Card>
                        </Grid>
                      ))}
                    </Grid>
                  </Grid>
                </Grid>
              </Box>
            )}

            {/* TAB: ANALYTICS */}
            {activeTab === 'analytics' && (
              <Box>
                <Box sx={{ mb: 4 }}>
                  <Typography variant="h3" component="h2" className="app-title">
                    📊 Usage Analytics
                  </Typography>
                  <Typography variant="body1" sx={{ color: 'text.secondary' }}>
                    View insights on generated volume, active API clients, preferred styles, and productivity gains.
                  </Typography>
                </Box>

                <Grid container spacing={3} sx={{ mb: 5 }}>
                  <Grid item xs={12} sm={6} md={3}>
                    <Paper className="glass-card" sx={{ p: 3, textAlign: 'center' }}>
                      <Typography variant="subtitle2" color="text.secondary" sx={{ fontWeight: 700 }}>
                        Total Generated
                      </Typography>
                      <Typography variant="h3" sx={{ mt: 1, color: 'primary.main', fontWeight: 800 }}>
                        {totalGenerated}
                      </Typography>
                      <Typography variant="caption" sx={{ display: 'block', mt: 1, color: 'text.secondary' }}>
                        draft emails archived
                      </Typography>
                    </Paper>
                  </Grid>

                  <Grid item xs={12} sm={6} md={3}>
                    <Paper className="glass-card" sx={{ p: 3, textAlign: 'center' }}>
                      <Typography variant="subtitle2" color="text.secondary" sx={{ fontWeight: 700 }}>
                        Time Saved (Est.)
                      </Typography>
                      <Typography variant="h3" sx={{ mt: 1, color: '#10b981', fontWeight: 800 }}>
                        {timeSavedMinutes}m
                      </Typography>
                      <Typography variant="caption" sx={{ display: 'block', mt: 1, color: 'text.secondary' }}>
                        estimated productivity gain
                      </Typography>
                    </Paper>
                  </Grid>

                  <Grid item xs={12} sm={6} md={3}>
                    <Paper className="glass-card" sx={{ p: 3, textAlign: 'center' }}>
                      <Typography variant="subtitle2" color="text.secondary" sx={{ fontWeight: 700 }}>
                        Favorite Tone
                      </Typography>
                      <Typography variant="h4" sx={{ mt: 1.5, color: '#f59e0b', fontWeight: 800, textTransform: 'capitalize' }}>
                        {favoriteTone || 'None'}
                      </Typography>
                      <Typography variant="caption" sx={{ display: 'block', mt: 1.5, color: 'text.secondary' }}>
                        most frequently selected tone
                      </Typography>
                    </Paper>
                  </Grid>

                  <Grid item xs={12} sm={6} md={3}>
                    <Paper className="glass-card" sx={{ p: 3, textAlign: 'center' }}>
                      <Typography variant="subtitle2" color="text.secondary" sx={{ fontWeight: 700 }}>
                        Primary Provider
                      </Typography>
                      <Typography variant="h4" sx={{ mt: 1.5, color: '#3b82f6', fontWeight: 800, textTransform: 'uppercase' }}>
                        {primaryProvider}
                      </Typography>
                      <Typography variant="caption" sx={{ display: 'block', mt: 1.5, color: 'text.secondary' }}>
                        most requested API client
                      </Typography>
                    </Paper>
                  </Grid>
                </Grid>

                <Grid container spacing={4}>
                  <Grid item xs={12} md={6}>
                    <Paper className="glass-card" sx={{ p: 4, height: '100%' }}>
                      <Typography variant="h6" gutterBottom sx={{ fontWeight: 700 }}>
                        🤖 LLM Provider Usage Distribution
                      </Typography>
                      <Divider sx={{ my: 2 }} />

                      {totalGenerated > 0 ? (
                        <Box sx={{ mt: 4, display: 'flex', flexDirection: 'column', gap: 3 }}>
                          {['groq', 'openai', 'gemini', 'claude'].map(p => {
                            const count = providerUsage[p] || 0;
                            const pct = totalGenerated > 0 ? Math.round((count / totalGenerated) * 100) : 0;
                            return (
                              <Box key={p}>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                                  <Typography variant="body2" sx={{ textTransform: 'uppercase', fontWeight: 700 }}>
                                    {p} ({count})
                                  </Typography>
                                  <Typography variant="body2" sx={{ fontWeight: 700 }}>
                                    {pct}%
                                  </Typography>
                                </Box>
                                <Box sx={{ width: '100%', height: 12, bgcolor: isDarkMode ? 'rgba(255, 255, 255, 0.05)' : '#e2e8f0', borderRadius: 6, overflow: 'hidden' }}>
                                  <Box
                                    sx={{
                                      width: `${pct}%`,
                                      height: '100%',
                                      background: p === 'groq' ? '#f55036' : p === 'openai' ? '#10a37f' : p === 'gemini' ? '#1a73e8' : '#d97706',
                                      borderRadius: 6,
                                      transition: 'width 0.8s cubic-bezier(0.4, 0, 0.2, 1)'
                                    }}
                                  />
                                </Box>
                              </Box>
                            );
                          })}
                        </Box>
                      ) : (
                        <Box sx={{ display: 'flex', height: 200, alignItems: 'center', justifyContent: 'center' }}>
                          <Typography variant="body2" sx={{ color: 'text.secondary', fontStyle: 'italic' }}>
                            Awaiting logs to populate metrics chart.
                          </Typography>
                        </Box>
                      )}
                    </Paper>
                  </Grid>

                  <Grid item xs={12} md={6}>
                    <Paper className="glass-card" sx={{ p: 4, height: '100%' }}>
                      <Typography variant="h6" gutterBottom sx={{ fontWeight: 700 }}>
                        🎭 Tone Usage Distribution
                      </Typography>
                      <Divider sx={{ my: 2 }} />

                      {totalGenerated > 0 ? (
                        <Box sx={{ mt: 3, display: 'flex', flexDirection: 'column', gap: 2 }}>
                          {Object.entries(toneUsage).map(([toneName, val]) => {
                            const pct = Math.round((val / totalGenerated) * 100);
                            return (
                              <Box key={toneName} sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                                <Box sx={{ width: 12, height: 12, borderRadius: '50%', bgcolor: 'primary.main' }} />
                                <Typography variant="body2" sx={{ minWidth: 100, textTransform: 'capitalize', fontWeight: 600 }}>
                                  {toneName || 'Default'}
                                </Typography>
                                <Box sx={{ flexGrow: 1, height: 8, bgcolor: isDarkMode ? 'rgba(255, 255, 255, 0.05)' : '#e2e8f0', borderRadius: 4, overflow: 'hidden' }}>
                                  <Box sx={{ width: `${pct}%`, height: '100%', bgcolor: 'secondary.main', borderRadius: 4 }} />
                                </Box>
                                <Typography variant="body2" sx={{ fontWeight: 700, minWidth: 40, textAlign: 'right' }}>
                                  {pct}%
                                </Typography>
                              </Box>
                            );
                          })}
                        </Box>
                      ) : (
                        <Box sx={{ display: 'flex', height: 200, alignItems: 'center', justifyContent: 'center' }}>
                          <Typography variant="body2" sx={{ color: 'text.secondary', fontStyle: 'italic' }}>
                            Awaiting logs to populate metrics chart.
                          </Typography>
                        </Box>
                      )}
                    </Paper>
                  </Grid>
                </Grid>
              </Box>
            )}

            {/* TAB: QUALITY ANALYZER */}
            {activeTab === 'quality' && (
              <EmailQualityAnalyzerDashboard backendUrl={backendUrl} />
            )}

            {/* TAB: SETTINGS */}
            {activeTab === 'settings' && (
              <Box>
                <Box sx={{ mb: 4 }}>
                  <Typography variant="h3" component="h2" className="app-title">
                    ⚙️ Settings & Connection
                  </Typography>
                  <Typography variant="body1" sx={{ color: 'text.secondary' }}>
                    Configure the location of your local Spring Boot backend and monitor API connectivity.
                  </Typography>
                </Box>

                <Grid container spacing={4}>
                  <Grid item xs={12} md={6}>
                    <Paper className="glass-card" sx={{ p: 4 }}>
                      <Typography variant="h6" gutterBottom sx={{ fontWeight: 700 }}>
                        🔗 Server Settings
                      </Typography>
                      <Divider sx={{ my: 2 }} />

                      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                        <TextField
                          fullWidth
                          label="Spring Boot Server URL"
                          value={backendUrl}
                          onChange={(e) => handleBackendUrlChange(e.target.value)}
                          placeholder="e.g. http://localhost:8080"
                        />

                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', p: 2, bgcolor: isDarkMode ? 'rgba(255, 255, 255, 0.03)' : '#f8fafc', borderRadius: 3 }}>
                          <Box>
                            <Typography variant="body2" sx={{ fontWeight: 700 }}>
                              Server Connection Status
                            </Typography>
                            <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                              Verifying local Spring Boot health check
                            </Typography>
                          </Box>
                          <span className={`badge-provider ${backendOnline ? 'badge-openai' : 'badge-groq'}`} style={{ textTransform: 'capitalize' }}>
                            {backendOnline ? '🟢 Connected' : '🔴 Offline'}
                          </span>
                        </Box>

                        <Button
                          variant="outlined"
                          onClick={checkBackendHealth}
                          fullWidth
                          sx={{ py: 1.2, borderRadius: 2 }}
                        >
                          Refresh Status
                        </Button>
                      </Box>
                    </Paper>
                  </Grid>

                  <Grid item xs={12} md={6}>
                    <Paper className="glass-card" sx={{ p: 4, height: '100%' }}>
                      <Typography variant="h6" gutterBottom sx={{ fontWeight: 700 }}>
                        🔑 Active API Keys
                      </Typography>
                      <Divider sx={{ my: 2 }} />

                      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                          These properties indicate whether the local backend has active credential configs loaded in its `application.properties`.
                        </Typography>

                        {['groq', 'openai', 'gemini', 'claude'].map(prov => (
                          <Box key={prov} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', py: 1 }}>
                            <Typography variant="body2" sx={{ textTransform: 'uppercase', fontWeight: 700 }}>
                              {prov === 'claude' ? '🦉 CLAUDE / ANTHROPIC' : prov === 'openai' ? '🧠 OPENAI' : prov === 'gemini' ? '♊ GEMINI' : '⚡ GROQ'}
                            </Typography>
                            <span className={`badge-provider ${providerConfig[prov] ? 'badge-openai' : 'badge-groq'}`}>
                              {providerConfig[prov] ? 'ACTIVE' : 'NOT CONFIGURED'}
                            </span>
                          </Box>
                        ))}
                      </Box>
                    </Paper>
                  </Grid>
                </Grid>
              </Box>
            )}

            {activeTab === 'migrations' && (
              <DatabaseMigrationDashboard />
            )}

            {activeTab === 'quotas' && (
              <QuotaManagementDashboard />
            )}

            {activeTab === 'telemetry' && (
              <EnterpriseTelemetryDashboard />
            )}

            {activeTab === 'context_templates' && (
              <ContextAwareTemplateList />
            )}

            {activeTab === 'campaign_roi' && (
              <CampaignROIDashboard />
            )}

            {activeTab === 'workflow_automation' && (
              <WorkflowAutomationBuilder />
            )}

            {activeTab === 'ab_testing' && (
              <ABTestingAnalytics />
            )}

            {activeTab === 'about' && (
              <AboutPage onBack={() => setActiveTab('generator')} />
            )}

            {activeTab === 'contact' && (
              <ContactPage onBack={() => setActiveTab('generator')} />
            )}

            {activeTab === 'guidelines' && (
              <GuidelinesPage onBack={() => setActiveTab('generator')} />
            )}

            {activeTab === 'help' && (
              <HelpPage onBack={() => setActiveTab('generator')} />
            )}

            {activeTab === 'terms' && (
              <TermsPage onBack={() => setActiveTab('generator')} />
            )}

            {activeTab === 'privacy' && (
              <PrivacyPage onBack={() => setActiveTab('generator')} />
            )}

            {activeTab === 'security' && (
              <SecurityPage onBack={() => setActiveTab('generator')} />
            )}

            {['blog', 'careers', 'guides'].includes(activeTab) && (
              <Box sx={{ py: 6, textAlign: 'center' }}>
                <Paper className="glass-card" sx={{ p: 6, maxWidth: 600, mx: 'auto' }}>
                  <Typography variant="h4" gutterBottom className="app-title">
                    🚧 Page Under Construction
                  </Typography>
                  <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                    The <strong>{activeTab.toUpperCase()}</strong> page is currently being designed and will be implemented in the next phase.
                  </Typography>
                  <Button variant="contained" className="gradient-btn" onClick={() => setActiveTab('generator')}>
                    Back to Generator
                  </Button>
                </Paper>
              </Box>
            )}

            <Footer onNavigate={(page) => setActiveTab(page)} />
          </Container>
        </Box>
      </Box>

      {/* Global Snackbar Toast Notification */}
      <Snackbar
        open={toast.open}
        autoHideDuration={3500}
        onClose={closeToast}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert
          onClose={closeToast}
          severity={toast.severity}
          variant="filled"
          sx={{ width: '100%', borderRadius: 3, fontWeight: 600 }}
        >
          {toast.message}
        </Alert>
      </Snackbar>
    </ThemeProvider>
  );
}

export default App;