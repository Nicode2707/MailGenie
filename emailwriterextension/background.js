// MailGenie: Background Service Worker (Manifest V3 Architecture)
// Solves CORS and Mixed-Content (HTTPS->HTTP) restrictions when fetching backend APIs from Gmail content script.

console.log("MailGenie: Background Service Worker initialized");

// Default fallback templates when backend is offline or unreachable
const DEFAULT_FALLBACK_TEMPLATES = [
  {
    id: "default_1",
    title: "👔 Professional Reply",
    body: "Dear {{name}},\n\nThank you for reaching out. I have reviewed your message and would be happy to assist. Let us schedule a brief call to discuss the next steps.\n\nBest regards,\n{{sender}}"
  },
  {
    id: "default_2",
    title: "☕ Casual Response",
    body: "Hi {{name}},\n\nThanks for the update! Sounds good to me. Let me know if you need anything else from my end.\n\nCheers,\n{{sender}}"
  },
  {
    id: "default_3",
    title: "📅 Schedule Meeting",
    body: "Hi {{name}},\n\nThanks for contacting me. I am available for a quick meeting to discuss this further. Please let me know your availability for this week.\n\nBest regards,\n{{sender}}"
  },
  {
    id: "default_4",
    title: "🙏 Thank You Note",
    body: "Dear {{name}},\n\nThank you very much for your prompt response and helpful assistance. I really appreciate your time and support.\n\nWarm regards,\n{{sender}}"
  },
  {
    id: "default_5",
    title: "✋ Polite Decline",
    body: "Dear {{name}},\n\nThank you for reaching out and considering me. Unfortunately, I am unable to proceed with this at the moment due to current commitments. I wish you all the best.\n\nBest regards,\n{{sender}}"
  }
];

// Helper: Normalize URL string
function normalizeUrl(url) {
  if (!url) return 'http://localhost:8080';
  let clean = url.trim();
  if (!/^https?:\/\//i.test(clean)) {
    clean = 'http://' + clean;
  }
  return clean.endsWith('/') ? clean.slice(0, -1) : clean;
}

// Get user settings from storage
async function getStoredSettings() {
  const defaults = {
    backendUrl: 'http://localhost:8080',
    provider: 'groq',
    apiKey: '',
    defaultTone: 'professional',
    defaultLanguage: 'English',
    customModel: '',
    customTemplates: []
  };

  return new Promise((resolve) => {
    if (typeof chrome === 'undefined' || !chrome.storage || !chrome.storage.local) {
      resolve(defaults);
      return;
    }
    chrome.storage.local.get(defaults, (items) => {
      resolve(items || defaults);
    });
  });
}

// Listen for messages from Content Script or Popup Script
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  const action = message.action || message.type;

  switch (action) {
    case 'GENERATE_EMAIL':
    case 'GENERATE_REPLY':
      handleGenerateEmail(message, sendResponse);
      return true; // Async response

    case 'FETCH_TEMPLATES':
      handleFetchTemplates(message, sendResponse);
      return true; // Async response

    case 'CHECK_BACKEND':
    case 'HEALTH_CHECK':
      handleHealthCheck(message, sendResponse);
      return true; // Async response

    case 'CHECK_CONFIG':
      handleCheckConfig(message, sendResponse);
      return true; // Async response

    case 'GET_SETTINGS':
      getStoredSettings().then((settings) => sendResponse({ success: true, settings }));
      return true;

    case 'SAVE_CUSTOM_TEMPLATE':
      handleSaveCustomTemplate(message, sendResponse);
      return true;

    case 'DELETE_CUSTOM_TEMPLATE':
      handleDeleteCustomTemplate(message, sendResponse);
      return true;

    default:
      sendResponse({ success: false, error: 'Unknown action requested' });
      return false;
  }
});

// Handler for AI Email Generation
async function handleGenerateEmail(message, sendResponse) {
  try {
    const settings = await getStoredSettings();
    const backendUrl = normalizeUrl(message.backendUrl || settings.backendUrl);

    const payload = {
      emailContent: message.emailContent || '',
      subject: message.subject || message.emailSubject || '',
      tone: message.tone || settings.defaultTone || 'professional',
      provider: message.provider || settings.provider || 'groq',
      model: message.model || settings.customModel || '',
      language: message.language || settings.defaultLanguage || 'English',
      apiKey: message.apiKey || settings.apiKey || '',
      customInstructions: message.customInstructions || '',
      composeMode: !!message.composeMode
    };

    console.log("MailGenie Worker: Sending generation request to backend:", backendUrl);

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 45000); // 45s timeout

    const response = await fetch(`${backendUrl}/api/email/generate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload),
      signal: controller.signal
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || `Backend returned status ${response.status}`);
    }

    const replyText = await response.text();
    sendResponse({ success: true, reply: replyText, data: replyText, backendUrl });

  } catch (error) {
    console.error("MailGenie Worker: Generation Error", error);
    let isNetworkError = false;
    let errorMsg = error.message;

    if (error.name === 'AbortError') {
      errorMsg = 'Request timed out waiting for AI model response.';
    } else if (errorMsg.includes('Failed to fetch') || errorMsg.includes('NetworkError') || errorMsg.includes('ERR_CONNECTION_REFUSED')) {
      isNetworkError = true;
      errorMsg = 'Cannot connect to MailGenie Backend. Please ensure your backend server is running on ' + (message.backendUrl || 'http://localhost:8080');
    }

    sendResponse({
      success: false,
      error: errorMsg,
      isNetworkError
    });
  }
}

// Handler for Fetching Templates (Backend + Local Custom + Defaults)
async function handleFetchTemplates(message, sendResponse) {
  try {
    const settings = await getStoredSettings();
    const backendUrl = normalizeUrl(message.backendUrl || settings.backendUrl);
    const customTemplates = settings.customTemplates || [];

    let remoteTemplates = [];
    let isBackendOnline = false;

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 4000);

      const response = await fetch(`${backendUrl}/api/templates`, {
        signal: controller.signal
      });
      clearTimeout(timeoutId);

      if (response.ok) {
        remoteTemplates = await response.json();
        isBackendOnline = true;
      }
    } catch (e) {
      console.warn("MailGenie Worker: Remote template fetch skipped/failed, using fallback.");
      isBackendOnline = false;
    }

    // Combine templates prioritizing remote, then user custom, then default fallbacks
    const allTemplates = [
      ...remoteTemplates,
      ...customTemplates,
      ...DEFAULT_FALLBACK_TEMPLATES
    ];

    // Deduplicate templates by title/body
    const uniqueTemplates = [];
    const seen = new Set();
    allTemplates.forEach(t => {
      const key = `${t.title}_${t.body}`;
      if (!seen.has(key)) {
        seen.add(key);
        uniqueTemplates.push(t);
      }
    });

    // Save cached templates in local storage
    chrome.storage.local.set({ cachedTemplates: uniqueTemplates, isBackendOnline });

    sendResponse({
      success: true,
      templates: uniqueTemplates,
      isBackendOnline
    });

  } catch (error) {
    console.error("MailGenie Worker: Fetch Templates Error", error);
    sendResponse({
      success: true,
      templates: DEFAULT_FALLBACK_TEMPLATES,
      isBackendOnline: false
    });
  }
}

// Handler for Backend Server Health Check & Ping
async function handleHealthCheck(message, sendResponse) {
  const startTime = Date.now();
  try {
    const settings = await getStoredSettings();
    const backendUrl = normalizeUrl(message.backendUrl || settings.backendUrl);

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4000);

    const response = await fetch(`${backendUrl}/api/email/config`, {
      signal: controller.signal
    });
    clearTimeout(timeoutId);

    const latency = Date.now() - startTime;

    if (response.ok) {
      const config = await response.json();
      sendResponse({
        success: true,
        online: true,
        latency,
        config
      });
    } else {
      sendResponse({
        success: true,
        online: false,
        latency,
        error: `Status code ${response.status}`
      });
    }
  } catch (err) {
    sendResponse({
      success: true,
      online: false,
      latency: Date.now() - startTime,
      error: err.message
    });
  }
}

// Handler for Provider Configuration status
async function handleCheckConfig(message, sendResponse) {
  try {
    const settings = await getStoredSettings();
    const backendUrl = normalizeUrl(message.backendUrl || settings.backendUrl);

    const response = await fetch(`${backendUrl}/api/email/config`);
    if (response.ok) {
      const config = await response.json();
      sendResponse({ success: true, config });
    } else {
      sendResponse({ success: false, error: 'Config endpoint returned error' });
    }
  } catch (err) {
    sendResponse({ success: false, error: err.message });
  }
}

// Handler for saving a user custom local template
async function handleSaveCustomTemplate(message, sendResponse) {
  try {
    const settings = await getStoredSettings();
    const customTemplates = settings.customTemplates || [];

    const newTemplate = {
      id: 'custom_' + Date.now(),
      title: message.title || 'Custom Template',
      body: message.body || ''
    };

    customTemplates.push(newTemplate);
    chrome.storage.local.set({ customTemplates }, () => {
      sendResponse({ success: true, template: newTemplate, customTemplates });
    });
  } catch (err) {
    sendResponse({ success: false, error: err.message });
  }
}

// Handler for deleting a user custom local template
async function handleDeleteCustomTemplate(message, sendResponse) {
  try {
    const settings = await getStoredSettings();
    let customTemplates = settings.customTemplates || [];

    customTemplates = customTemplates.filter(t => t.id !== message.id && t.title !== message.title);
    chrome.storage.local.set({ customTemplates }, () => {
      sendResponse({ success: true, customTemplates });
    });
  } catch (err) {
    sendResponse({ success: false, error: err.message });
  }
}
