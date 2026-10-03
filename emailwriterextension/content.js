// MailGenie: Enterprise Modern Gmail Integration Content Script v2.1
console.log("MailGenie Extension - Content Script Loaded v2.1");

(function () {
  'use strict';

  // Store undo state per compose container
  const composeUndoStateMap = new WeakMap();

  let cachedTemplates = [];
  let templatesFetched = false;
  let isBackendConnected = true;
  let isContextValid = true;

  // Guard against Extension Context Invalidation
  function checkContext() {
    try {
      if (typeof chrome === 'undefined' || !chrome.runtime || !chrome.runtime.id) {
        isContextValid = false;
        return false;
      }
      void chrome.runtime.id;
      isContextValid = true;
      return true;
    } catch (e) {
      isContextValid = false;
      return false;
    }
  }

  // Safely send messages to background service worker with direct fallback
  function sendMessageToWorker(message) {
    return new Promise((resolve) => {
      if (!checkContext()) {
        resolve({ success: false, error: 'Extension context invalidated. Refresh Gmail tab.' });
        return;
      }

      try {
        chrome.runtime.sendMessage(message, (response) => {
          if (chrome.runtime.lastError) {
            console.warn("MailGenie: Message passing error:", chrome.runtime.lastError.message);
            resolve({ success: false, error: chrome.runtime.lastError.message });
          } else {
            resolve(response || { success: false, error: 'No response received' });
          }
        });
      } catch (err) {
        console.warn("MailGenie: Messaging failed", err);
        resolve({ success: false, error: err.message });
      }
    });
  }

  // Global Toast Notification UI
  function showToast(message, type = 'info', duration = 3000) {
    let container = document.getElementById('mailgenie-toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'mailgenie-toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `mailgenie-toast mailgenie-toast-${type}`;

    let icon = '✨';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '⚠️';
    if (type === 'undo') icon = '↩️';

    toast.innerHTML = `<span class="toast-icon">${icon}</span><span class="toast-msg">${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    requestAnimationFrame(() => {
      toast.classList.add('show');
    });

    setTimeout(() => {
      toast.classList.remove('show');
      toast.addEventListener('transitionend', () => toast.remove());
    }, duration);
  }

  function escapeHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  // Modern text insertion alternative to document.execCommand
  function insertTextAtCursor(text, composeBox) {
    composeBox.focus();
    const selection = window.getSelection();
    if (selection.getRangeAt && selection.rangeCount) {
      let range = selection.getRangeAt(0);
      if (!composeBox.contains(range.commonAncestorContainer)) {
        range = document.createRange();
        range.selectNodeContents(composeBox);
        range.collapse(false);
        selection.removeAllRanges();
        selection.addRange(range);
      }
      range.deleteContents();
      const textNode = document.createTextNode(text);
      range.insertNode(textNode);
      range.setStartAfter(textNode);
      range.collapse(true);
      selection.removeAllRanges();
      selection.addRange(range);
      composeBox.dispatchEvent(new Event('input', { bubbles: true }));
    } else {
      composeBox.innerText += text;
      composeBox.dispatchEvent(new Event('input', { bubbles: true }));
    }
  }

  // Get user settings safely
  async function getSettings() {
    const defaults = {
      backendUrl: 'http://localhost:8080',
      provider: 'groq',
      apiKey: '',
      defaultTone: 'professional',
      defaultLanguage: 'English',
      customModel: ''
    };

    if (!checkContext()) return defaults;

    return new Promise((resolve) => {
      try {
        chrome.storage.local.get(defaults, (items) => {
          if (chrome.runtime.lastError) {
            resolve(defaults);
          } else {
            resolve(items || defaults);
          }
        });
      } catch (e) {
        resolve(defaults);
      }
    });
  }

  // Fetch templates via Background Worker with fallback
  async function fetchTemplates(backendUrl) {
    if (templatesFetched && cachedTemplates.length > 0) return cachedTemplates;

    const response = await sendMessageToWorker({
      action: 'FETCH_TEMPLATES',
      backendUrl
    });

    if (response && response.success && Array.isArray(response.templates)) {
      cachedTemplates = response.templates;
      templatesFetched = true;
      isBackendConnected = !!response.isBackendOnline;
    } else {
      cachedTemplates = [
        { title: '👔 Professional Reply', body: 'Dear [Name],\n\nThank you for reaching out. I have reviewed your request and would be glad to assist.\n\nBest regards,\n[Your Name]' },
        { title: '☕ Casual Response', body: 'Hi [Name],\n\nThanks for the update! Sounds good. Let me know if you need anything else.\n\nBest,\n[Your Name]' },
        { title: '📅 Schedule Meeting', body: 'Hi [Name],\n\nI am available for a meeting to discuss this further. Let me know what times work best for you.\n\nBest regards,\n[Your Name]' }
      ];
      templatesFetched = true;
      isBackendConnected = false;
    }

    return cachedTemplates;
  }

  // Theme auto-detection for Gmail light/dark mode
  function detectTheme(element) {
    try {
      let current = element;
      while (current && current !== document.body) {
        const bg = window.getComputedStyle(current).backgroundColor;
        if (bg && bg !== 'transparent' && bg !== 'rgba(0, 0, 0, 0)') {
          const rgb = bg.match(/\d+/g);
          if (rgb && rgb.length >= 3) {
            const r = parseInt(rgb[0]);
            const g = parseInt(rgb[1]);
            const b = parseInt(rgb[2]);
            const brightness = (r * 299 + g * 587 + b * 114) / 1000;
            return brightness < 128 ? 'dark' : 'light';
          }
        }
        current = current.parentElement;
      }
    } catch (e) {
      if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
        return 'dark';
      }
    }
    return 'light';
  }

  // Create UI Controls
  function createAIButton() {
    const button = document.createElement('button');
    button.className = 'mailgenie-btn mailgenie-btn-primary';
    button.innerHTML = '<span class="mailgenie-btn-icon">✨</span><span class="mailgenie-btn-text">AI Reply</span>';
    button.setAttribute('type', 'button');
    button.setAttribute('title', 'Generate AI Email Reply (Ctrl+Shift+G)');
    return button;
  }

  function createUndoButton() {
    const button = document.createElement('button');
    button.className = 'mailgenie-btn mailgenie-btn-secondary mailgenie-btn-undo';
    button.innerHTML = '<span class="mailgenie-btn-icon">↩</span><span class="mailgenie-btn-text">Undo</span>';
    button.setAttribute('type', 'button');
    button.setAttribute('title', 'Undo AI generated text insertion');
    button.disabled = true;
    return button;
  }

  function createCopyButton() {
    const button = document.createElement('button');
    button.className = 'mailgenie-btn mailgenie-btn-icon-only';
    button.innerHTML = '📋';
    button.setAttribute('type', 'button');
    button.setAttribute('title', 'Copy draft to clipboard');
    return button;
  }

  function createToneSelect(defaultValue) {
    const select = document.createElement('select');
    select.className = 'mailgenie-select mailgenie-tone-select';
    select.title = 'Select reply tone';

    const tones = [
      { value: 'professional', label: '👔 Professional' },
      { value: 'casual', label: '☕ Casual' },
      { value: 'friendly', label: '😊 Friendly' },
      { value: 'persuasive', label: '🎯 Persuasive' },
      { value: 'urgent', label: '⏰ Urgent' },
      { value: 'empathetic', label: '❤️ Empathetic' },
      { value: 'concise', label: '⚡ Concise' },
      { value: 'detailed', label: '📝 Detailed' },
      { value: 'enthusiastic', label: '🚀 Enthusiastic' }
    ];

    tones.forEach(t => {
      const option = document.createElement('option');
      option.value = t.value;
      option.text = t.label;
      if (t.value === defaultValue) option.selected = true;
      select.appendChild(option);
    });

    return select;
  }

  function createLanguageSelect(defaultValue) {
    const select = document.createElement('select');
    select.className = 'mailgenie-select mailgenie-lang-select';
    select.title = 'Select reply language';

    const languages = [
      { value: 'English', label: '🇺🇸 EN' },
      { value: 'Spanish', label: '🇪🇸 ES' },
      { value: 'French', label: '🇫🇷 FR' },
      { value: 'German', label: '🇩🇪 DE' },
      { value: 'Italian', label: '🇮🇹 IT' },
      { value: 'Japanese', label: '🇯🇵 JA' },
      { value: 'Chinese', label: '🇨🇳 ZH' },
      { value: 'Hindi', label: '🇮🇳 HI' },
      { value: 'Portuguese', label: '🇵🇹 PT' }
    ];

    languages.forEach(l => {
      const option = document.createElement('option');
      option.value = l.value;
      option.text = l.label;
      if (l.value === defaultValue) option.selected = true;
      select.appendChild(option);
    });

    return select;
  }

  function createTemplateSelect() {
    const select = document.createElement('select');
    select.className = 'mailgenie-select mailgenie-template-select';
    select.title = 'Quick Reply Presets / Prompts';

    const templates = [
      { value: '', label: '💡 Templates' },
      { value: 'thank_confirm', label: '🙏 Thank & Confirm' },
      { value: 'schedule_meeting', label: '📅 Schedule Meeting' },
      { value: 'polite_decline', label: '✋ Decline Gracefully' },
      { value: 'request_info', label: '❓ Request Details' },
      { value: 'follow_up', label: '📌 Polite Follow-up' },
      { value: 'custom_prompt', label: '✏️ Custom Prompt...' }
    ];

    templates.forEach(t => {
      const option = document.createElement('option');
      option.value = t.value;
      option.text = t.label;
      select.appendChild(option);
    });

    return select;
  }

  // Custom Prompt Dialog Modal
  function openCustomPromptModal(onApply) {
    const existingModal = document.getElementById('mailgenie-custom-modal');
    if (existingModal) existingModal.remove();

    const backdrop = document.createElement('div');
    backdrop.id = 'mailgenie-custom-modal';
    backdrop.className = 'mailgenie-modal-backdrop';

    const content = document.createElement('div');
    content.className = 'mailgenie-modal-card';
    content.innerHTML = `
      <div class="mailgenie-modal-header">
        <h3>✨ MailGenie Custom Prompt</h3>
        <button class="mailgenie-modal-close">&times;</button>
      </div>
      <div class="mailgenie-modal-body">
        <label for="mailgenie-custom-input">Specify custom instructions for AI reply:</label>
        <textarea id="mailgenie-custom-input" placeholder="e.g. Thank them for the invite and propose meeting next Tuesday at 3 PM EST. Ask to send calendar invite."></textarea>
      </div>
      <div class="mailgenie-modal-footer">
        <button class="mailgenie-btn mailgenie-btn-cancel">Cancel</button>
        <button class="mailgenie-btn mailgenie-btn-primary mailgenie-btn-submit">Generate Reply</button>
      </div>
    `;

    backdrop.appendChild(content);
    document.body.appendChild(backdrop);

    const input = content.querySelector('#mailgenie-custom-input');
    input.focus();

    const closeModal = () => backdrop.remove();

    content.querySelector('.mailgenie-modal-close').addEventListener('click', closeModal);
    content.querySelector('.mailgenie-btn-cancel').addEventListener('click', closeModal);
    backdrop.addEventListener('click', (e) => {
      if (e.target === backdrop) closeModal();
    });

    content.querySelector('.mailgenie-btn-submit').addEventListener('click', () => {
      const promptText = input.value.trim();
      if (promptText) {
        closeModal();
        onApply(promptText);
      } else {
        input.style.borderColor = '#ef4444';
      }
    });
  }

  // Extract email thread content
  function getEmailContent(composeContainer) {
    const selectors = [
      '.a3s.aiL',
      '.gmail_quote',
      '.h7',
      '[role="presentation"]',
      '[data-message-id]'
    ];

    if (composeContainer) {
      const threadContainer = composeContainer.closest('.g3') || composeContainer.closest('.dw') || composeContainer.closest('.nH');
      if (threadContainer) {
        for (const selector of selectors) {
          const contents = threadContainer.querySelectorAll(selector);
          if (contents.length > 0) {
            const latestContent = contents[contents.length - 1];
            if (latestContent && latestContent.innerText.trim()) {
              return cleanEmailText(latestContent.innerText);
            }
          }
        }
      }
    }

    for (const selector of selectors) {
      const contents = document.querySelectorAll(selector);
      if (contents.length > 0) {
        const latestContent = contents[contents.length - 1];
        if (latestContent && latestContent.innerText.trim()) {
          return cleanEmailText(latestContent.innerText);
        }
      }
    }
    return '';
  }

  // Extract email subject line context
  function getSubjectContent(composeContainer) {
    const subjectSelectors = [
      'input[name="subjectbox"]',
      'input[name="subject"]',
      'div[aria-label="Subject"]',
      'div[aria-label*="Subject"]',
      'input[placeholder="Subject"]',
      'input[placeholder*="Subject"]',
      'h2.hP'
    ];

    if (composeContainer) {
      for (const sel of subjectSelectors) {
        const el = composeContainer.querySelector(sel);
        if (el) {
          const val = el.value || el.innerText || el.textContent;
          if (val && val.trim()) return val.trim();
        }
      }

      const root = composeContainer.closest('[role="dialog"]') ||
                   composeContainer.closest('form') ||
                   composeContainer.closest('.g3') ||
                   composeContainer.closest('.dw') ||
                   composeContainer.closest('.nH');
      if (root) {
        for (const sel of subjectSelectors) {
          const el = root.querySelector(sel);
          if (el) {
            const val = el.value || el.innerText || el.textContent;
            if (val && val.trim()) return val.trim();
          }
        }
      }
    }

    for (const sel of subjectSelectors) {
      const el = document.querySelector(sel);
      if (el) {
        const val = el.value || el.innerText || el.textContent;
        if (val && val.trim()) return val.trim();
      }
    }
    return '';
  }

  function cleanEmailText(text) {
    if (!text) return '';
    let cleaned = text.trim();
    const splitIdx = cleaned.indexOf('On ') && cleaned.indexOf(' wrote:');
    if (splitIdx && splitIdx > 50) {
      cleaned = cleaned.substring(0, splitIdx);
    }
    return cleaned.trim();
  }

  // Client-side Template Variable Replacer
  function replaceTemplateVariables(template, variables = {}) {
    if (!template) return '';
    let result = template;
    for (const [key, value] of Object.entries(variables)) {
      if (!key) continue;
      const mustacheRegex = new RegExp(`\\{\\{\\s*${key}\\s*\\}\\}`, 'gi');
      const bracketRegex = new RegExp(`\\[\\s*${key}\\s*\\]`, 'gi');
      const val = value != null ? value : '';
      result = result.replace(mustacheRegex, val).replace(bracketRegex, val);
    }
    return result;
  }

  // Locate compose window roots
  function findComposeContainers() {
    const containers = new Set();
    const mainComposeBoxes = document.querySelectorAll('div[role="dialog"], form.aaq, .gM, .nH.if, .dw .g3');
    mainComposeBoxes.forEach(box => {
      if (box.querySelector('[role="textbox"][g_editable="true"]') || box.querySelector('.btC')) {
        containers.add(box);
      }
    });

    const sendButtons = document.querySelectorAll('div[aria-label*="Send"], [role="button"][aria-label*="Send"]');
    sendButtons.forEach(btn => {
      const parentDialog = btn.closest('[role="dialog"]') || btn.closest('form') || btn.closest('.gM') || btn.closest('.nH.if') || btn.closest('.btC')?.parentElement;
      if (parentDialog) {
        containers.add(parentDialog);
      }
    });

    return Array.from(containers);
  }

  // Find the exact editable text area for a given compose container
  function getComposeEditor(container) {
    const selectors = [
      '[role="textbox"][g_editable="true"]',
      '[role="textbox"][contenteditable="true"]',
      '[contenteditable="true"]',
      'div[aria-label*="Message"]',
      'div[aria-label*="Body"]',
      '.Am.Al.editable'
    ];

    if (container) {
      for (const sel of selectors) {
        const box = container.querySelector(sel);
        if (box) return box;
      }

      // Check parent dialog/form/thread wrappers if container is just the toolbar element
      const root = container.closest('[role="dialog"]') ||
                   container.closest('form') ||
                   container.closest('.gM') ||
                   container.closest('.nH') ||
                   container.closest('.g3') ||
                   container.closest('.dw') ||
                   container.parentElement;

      if (root) {
        for (const sel of selectors) {
          const box = root.querySelector(sel);
          if (box) return box;
        }
      }
    }

    // Fallback: locate any visible contenteditable box in Gmail
    const allBoxes = document.querySelectorAll('[contenteditable="true"], [role="textbox"]');
    for (const box of allBoxes) {
      if (box.offsetWidth > 0 && box.offsetHeight > 0) {
        return box;
      }
    }
    return null;
  }

  // Prevent duplicate toolbar rendering
  function cleanupDuplicateToolbars() {
    const wrappers = document.querySelectorAll('.mailgenie-wrapper');
    const seenContainers = new Set();

    wrappers.forEach(wrapper => {
      const parentContainer = wrapper.closest('[role="dialog"]') || wrapper.closest('form') || wrapper.closest('.gM') || wrapper.parentElement;
      if (parentContainer) {
        if (seenContainers.has(parentContainer)) {
          wrapper.remove();
        } else {
          seenContainers.add(parentContainer);
        }
      }
    });
  }

  async function injectButton() {
    if (!checkContext()) return;

    cleanupDuplicateToolbars();

    const composeContainers = findComposeContainers();
    if (composeContainers.length === 0) return;

    const settings = await getSettings();

    composeContainers.forEach(container => {
      if (container.getAttribute('data-mailgenie-injected') === 'true' || container.querySelector('.mailgenie-wrapper')) {
        return;
      }

      const toolbar = container.querySelector('.btC') || container.querySelector('[role="toolbar"]') || container.querySelector('.gU.Up');
      if (!toolbar) return;
      if (toolbar.querySelector('.mailgenie-wrapper')) return;

      container.setAttribute('data-mailgenie-injected', 'true');
      console.log("MailGenie: Injecting unified toolbar control");

      const wrapper = document.createElement('div');
      wrapper.className = 'mailgenie-wrapper';

      const theme = detectTheme(toolbar);
      if (theme === 'dark') {
        wrapper.classList.add('mailgenie-dark');
      }

      const button = createAIButton();
      const undoButton = createUndoButton();
      const toneSelect = createToneSelect(settings.defaultTone);
      const langSelect = createLanguageSelect(settings.defaultLanguage);
      const templateSelect = createTemplateSelect();
      const copyButton = createCopyButton();

      wrapper.appendChild(button);
      wrapper.appendChild(undoButton);
      wrapper.appendChild(toneSelect);
      wrapper.appendChild(langSelect);
      wrapper.appendChild(templateSelect);
      wrapper.appendChild(copyButton);

      // Core Generation Handler
      const executeGeneration = async (customPrompt = '') => {
        const emailContent = getEmailContent(container);
        const emailSubject = getSubjectContent(container);

        if (!emailContent && !customPrompt && !emailSubject) {
          showToast('Could not find email thread content to reply to', 'error');
          return;
        }

        const composeBox = getComposeEditor(container);

        if (!composeBox) {
          showToast('Could not locate email compose editor', 'error');
          return;
        }

        const selectedTone = toneSelect.value || settings.defaultTone;
        const selectedLang = langSelect.value || settings.defaultLanguage;

        // UI Loading state
        button.innerHTML = '<span class="mailgenie-btn-icon">⏳</span><span class="mailgenie-btn-text">Generating...</span>';
        button.disabled = true;

        try {
          let responseData = null;

          const bgResponse = await sendMessageToWorker({
            action: 'GENERATE_EMAIL',
            backendUrl: settings.backendUrl,
            emailContent: emailContent || (emailSubject ? `Regarding email subject: ${emailSubject}` : 'Generate email based on custom instructions'),
            subject: emailSubject,
            tone: selectedTone,
            provider: settings.provider,
            model: settings.customModel,
            language: selectedLang,
            apiKey: settings.apiKey,
            customInstructions: customPrompt
          });

          if (bgResponse && bgResponse.success && (bgResponse.data || bgResponse.reply)) {
            responseData = bgResponse.data || bgResponse.reply;
          } else {
            // Direct fetch fallback
            const res = await fetch(`${settings.backendUrl.replace(/\/$/, '')}/api/email/generate`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                emailContent: emailContent || (emailSubject ? `Regarding email subject: ${emailSubject}` : 'Generate email based on custom instructions'),
                subject: emailSubject,
                tone: selectedTone,
                provider: settings.provider,
                model: settings.customModel,
                language: selectedLang,
                apiKey: settings.apiKey,
                customInstructions: customPrompt
              })
            });

            if (!res.ok) throw new Error('API response failed');
            responseData = await res.text();
          }

          if (responseData) {
            // Save Undo snapshot
            composeUndoStateMap.set(container, composeBox.innerHTML);
            undoButton.disabled = false;
            undoButton.classList.add('mailgenie-btn-active');

            insertTextAtCursor(responseData, composeBox);
            showToast('AI Draft inserted successfully!', 'success');
          }
        } catch (err) {
          console.error("MailGenie error:", err);
          showToast('Failed to generate reply. Check backend settings.', 'error');
        } finally {
          button.innerHTML = '<span class="mailgenie-btn-icon">✨</span><span class="mailgenie-btn-text">AI Reply</span>';
          button.disabled = false;
        }
      };

      button.addEventListener('click', () => executeGeneration());

      undoButton.addEventListener('click', () => {
        const previousState = composeUndoStateMap.get(container);
        if (previousState !== undefined) {
          const composeBox = getComposeEditor(container);
          if (composeBox) {
            composeBox.innerHTML = previousState;
            composeUndoStateMap.delete(container);
            undoButton.disabled = true;
            undoButton.classList.remove('mailgenie-btn-active');
            showToast('Restored previous draft state', 'undo');
          }
        }
      });

      copyButton.addEventListener('click', () => {
        const composeBox = getComposeEditor(container);
        if (composeBox && composeBox.innerText.trim()) {
          navigator.clipboard.writeText(composeBox.innerText);
          showToast('Copied draft to clipboard!', 'success');
        } else {
          showToast('Compose window is empty', 'error');
        }
      });

      templateSelect.addEventListener('change', (e) => {
        const val = e.target.value;
        if (!val) return;

        if (val === 'custom_prompt') {
          openCustomPromptModal((customText) => {
            executeGeneration(customText);
          });
          e.target.value = '';
          return;
        }

        const composeBox = getComposeEditor(container);

        if (composeBox) {
          composeUndoStateMap.set(container, composeBox.innerHTML);
          undoButton.disabled = false;
          undoButton.classList.add('mailgenie-btn-active');

          let textToInsert = '';
          if (val === 'thank_confirm') textToInsert = 'Thank you for your email. I confirm receipt and agree with the details provided.';
          else if (val === 'schedule_meeting') textToInsert = 'Thanks for reaching out! I would be glad to meet. Please let me know your available time slots.';
          else if (val === 'polite_decline') textToInsert = 'Thank you for considering me. Unfortunately, I am unable to participate at this time.';
          else if (val === 'request_info') textToInsert = 'Thank you for the update. Could you please share more details regarding the next steps?';
          else if (val === 'follow_up') textToInsert = 'Hi, just following up on my previous email. Looking forward to hearing from you.';

          insertTextAtCursor(textToInsert, composeBox);
          showToast('Template preset inserted!', 'success');
        } else {
          showToast('Could not locate email compose editor', 'error');
        }
        e.target.value = '';
      });

      // Insert toolbar before send button
      const sendButtonWrapper = toolbar.querySelector('.gU.Up') || toolbar.firstChild;
      if (sendButtonWrapper) {
        toolbar.insertBefore(wrapper, sendButtonWrapper);
      } else {
        toolbar.appendChild(wrapper);
      }
    });
  }

  // Keyboard shortcut listener (Ctrl+Shift+G)
  document.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key.toLowerCase() === 'g') {
      const activeEl = document.activeElement;
      if (activeEl) {
        const container = activeEl.closest('[role="dialog"]') || activeEl.closest('form') || activeEl.closest('.gM');
        if (container) {
          const aiBtn = container.querySelector('.mailgenie-btn-primary');
          if (aiBtn) aiBtn.click();
        }
      }
    }
  });

  // Debounced Observer for Gmail SPA Mutations
  let observerTimer = null;
  const observer = new MutationObserver(() => {
    if (observerTimer) clearTimeout(observerTimer);
    observerTimer = setTimeout(() => {
      injectButton();
    }, 250);
  });

  observer.observe(document.body, {
    childList: true,
    subtree: true
  });

  // Initial trigger
  injectButton();
})();
