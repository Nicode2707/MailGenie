/**
 * Chrome/Firefox extension background service worker offline inference fallback.
 */

export class ExtensionOfflineBridge {
    /**
     * Detects if the browser is currently offline.
     * @returns {boolean}
     */
    static isOffline() {
        return typeof navigator !== 'undefined' && !navigator.onLine;
    }

    /**
     * Generates an email draft via local rules-based fallback when offline without WebGPU.
     * @param {Object} payload 
     * @param {string} payload.prompt 
     * @param {string} payload.tone 
     * @returns {string}
     */
    static generateQuickFallback({ prompt, tone = 'Professional' }) {
        const greetings = tone.toLowerCase() === 'casual' ? 'Hi there,' : 'Dear recipient,';
        const signoff = tone.toLowerCase() === 'casual' ? 'Best,\n[Your Name]' : 'Sincerely,\n[Your Name]';

        return `${greetings}\n\nThank you for reaching out. In response to your message regarding "${prompt.slice(0, 80)}", I am reviewing the details and will follow up shortly.\n\n${signoff}`;
    }

    /**
     * Routes email generation request to cloud API or local fallback depending on network state.
     * @param {Object} options 
     * @param {Function} cloudApiFn 
     * @returns {Promise<string>}
     */
    static async executeWithFallback(options, cloudApiFn) {
        if (this.isOffline()) {
            console.log('[ExtensionOffline] Network disconnected, using local fallback.');
            return this.generateQuickFallback(options);
        }

        try {
            return await cloudApiFn(options);
        } catch (err) {
            console.warn('[ExtensionOffline] Cloud API failed, engaging offline fallback:', err);
            return this.generateQuickFallback(options);
        }
    }
}
