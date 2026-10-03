import { create } from 'zustand';

const defaultCustomTemplates = [
  { id: 'p1', title: '💼 Professional Follow-up', body: 'Dear {{name}},\n\nI wanted to follow up on our discussion regarding {{topic}}. Please let me know if you have had a chance to review the details.\n\nBest regards,\n{{sender}}' },
  { id: 'p2', title: '📅 Schedule Meeting', body: 'Hi {{name}},\n\nI would love to schedule a quick 15-minute call to align on our next steps. Please let me know your availability this week.\n\nThanks,\n{{sender}}' },
  { id: 'p3', title: '☕ Casual Check-in', body: 'Hey {{name}},\n\nHope you are doing well! Just wanted to check in and see how things are going with {{project}}. Let me know when you are free to catch up.\n\nCheers,\n{{sender}}' }
];

const useStore = create((set, get) => ({
  // UI & Theme Slice
  isDarkMode: localStorage.getItem('theme') === 'dark' || (!localStorage.getItem('theme') && (typeof window !== 'undefined' && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches)),
  toggleTheme: () => set((state) => {
    const newTheme = !state.isDarkMode;
    localStorage.setItem('theme', newTheme ? 'dark' : 'light');
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-theme', newTheme ? 'dark' : 'light');
    }
    return { isDarkMode: newTheme };
  }),
  setIsDarkMode: (val) => set(() => {
    const isDark = typeof val === 'function' ? val(get().isDarkMode) : !!val;
    localStorage.setItem('theme', isDark ? 'dark' : 'light');
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
    }
    return { isDarkMode: isDark };
  }),

  activeTab: 'generator',
  setActiveTab: (tab) => set({ activeTab: tab }),

  mobileOpen: false,
  setMobileOpen: (open) => set((state) => ({
    mobileOpen: typeof open === 'function' ? open(state.mobileOpen) : open
  })),

  toast: { open: false, message: '', severity: 'info' },
  showToast: (message, severity = 'info') => set({ toast: { open: true, message, severity } }),
  closeToast: () => set((state) => ({ toast: { ...state.toast, open: false } })),

  // Config & Provider Slice
  backendUrl: localStorage.getItem('mailgenie_backend_url') || 'http://localhost:8080',
  setBackendUrl: (url) => set(() => {
    localStorage.setItem('mailgenie_backend_url', url);
    return { backendUrl: url };
  }),

  backendOnline: false,
  setBackendOnline: (status) => set({ backendOnline: status }),

  provider: 'groq',
  setProvider: (provider) => set({ provider }),

  model: '',
  setModel: (model) => set({ model }),

  tone: '',
  setTone: (tone) => set({ tone }),

  language: 'English',
  setLanguage: (language) => set({ language }),

  providerConfig: { groq: false, openai: false, gemini: false, claude: false },
  setProviderConfig: (config) => set({ providerConfig: config }),

  // Templates Slice
  customTemplates: (() => {
    try {
      const saved = localStorage.getItem('mailgenie_custom_templates');
      return saved ? JSON.parse(saved) : defaultCustomTemplates;
    } catch {
      return defaultCustomTemplates;
    }
  })(),
  setCustomTemplates: (templates) => set((state) => {
    const resolved = typeof templates === 'function' ? templates(state.customTemplates) : templates;
    try {
      localStorage.setItem('mailgenie_custom_templates', JSON.stringify(resolved));
    } catch (e) {
      console.warn('Failed to save templates to localStorage:', e);
    }
    return { customTemplates: resolved };
  }),

  // History Slice
  historyList: [],
  setHistoryList: (list) => set((state) => {
    const resolved = typeof list === 'function' ? list(state.historyList) : list;
    return { historyList: resolved };
  }),

  searchTerm: '',
  setSearchTerm: (term) => set({ searchTerm: term }),

  // Multi-Tone Compare Mode Slice
  compareMode: false,
  setCompareMode: (mode) => set((state) => ({
    compareMode: typeof mode === 'function' ? mode(state.compareMode) : mode
  })),

  compareReplies: {
    professional: '',
    casual: '',
    persuasive: ''
  },
  setCompareReplies: (replies) => set((state) => ({
    compareReplies: typeof replies === 'function' ? replies(state.compareReplies) : replies
  })),

  compareLoading: false,
  setCompareLoading: (loading) => set({ compareLoading: loading }),

  // Prompt Studio Preset Slice
  studioFormality: 70,
  setStudioFormality: (val) => set({ studioFormality: val }),

  studioLength: 'medium',
  setStudioLength: (val) => set({ studioLength: val }),

  studioSignature: 'Best regards,\n[Your Name]',
  setStudioSignature: (val) => set({ studioSignature: val }),

  studioSalutation: 'Dear [Name],',
  setStudioSalutation: (val) => set({ studioSalutation: val }),

  studioCustomInstruction: '',
  setStudioCustomInstruction: (val) => set({ studioCustomInstruction: val })
}));

export default useStore;
