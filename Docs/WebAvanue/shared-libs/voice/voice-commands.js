/**
 * Voice Command System for WebAvanue OS
 * Voice-first design: Every element has voice commands
 * Displays voice hints and handles voice interactions
 * Version: 1.0.0
 */

class VoiceCommandSystem {
    constructor(themeEngine) {
        this.themeEngine = themeEngine;
        this.commands = new Map();
        this.voiceHintsVisible = false;
        this.hintsContainer = null;
        this.activeElement = null;
        this.commandAliases = new Map();

        // Voice recognition (if available)
        this.recognition = null;
        this.isListening = false;

        // Initialize
        this.initializeVoiceRecognition();
        this.createHintsContainer();
        this.registerGlobalCommands();
    }

    /**
     * Initialize Web Speech API
     */
    initializeVoiceRecognition() {
        if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
            const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
            this.recognition = new SpeechRecognition();
            this.recognition.continuous = true;
            this.recognition.interimResults = false;
            this.recognition.lang = 'en-US';

            this.recognition.onresult = (event) => {
                const last = event.results.length - 1;
                const command = event.results[last][0].transcript.toLowerCase().trim();
                this.processVoiceCommand(command);
            };

            this.recognition.onerror = (event) => {
                console.error('Voice recognition error:', event.error);
                if (event.error === 'no-speech') {
                    this.stopListening();
                }
            };
        } else {
            console.warn('Voice recognition not supported in this browser');
        }
    }

    /**
     * Register element with voice commands
     */
    registerElement(element, commands) {
        if (!element.id) {
            console.warn('Element must have an ID for voice commands');
            return;
        }

        const commandData = {
            element: element,
            primary: commands.primary || '',
            alternatives: commands.alternatives || [],
            description: commands.description || '',
            action: commands.action || (() => element.click()),
            context: commands.context || 'global',
            ariaLabel: commands.ariaLabel || commands.primary
        };

        // Store command
        this.commands.set(element.id, commandData);

        // Register aliases
        [commandData.primary, ...commandData.alternatives].forEach(cmd => {
            if (cmd) {
                this.commandAliases.set(cmd.toLowerCase(), element.id);
            }
        });

        // Set ARIA labels
        element.setAttribute('aria-label', commandData.ariaLabel);
        element.setAttribute('data-voice-command', commandData.primary);

        // Add voice hint on focus
        element.addEventListener('focus', () => {
            this.showVoiceHintForElement(element);
        });

        return commandData;
    }

    /**
     * Register global commands (always available)
     */
    registerGlobalCommands() {
        // These commands work without specific elements
        const globalCommands = {
            'show voice commands': () => this.toggleVoiceHints(),
            'hide voice commands': () => this.hideVoiceHints(),
            'help': () => this.showVoiceHints(),
            'start listening': () => this.startListening(),
            'stop listening': () => this.stopListening(),
            'what can i say': () => this.announceAvailableCommands()
        };

        Object.entries(globalCommands).forEach(([command, action]) => {
            this.commandAliases.set(command, {
                isGlobal: true,
                action: action
            });
        });
    }

    /**
     * Create hints overlay container
     */
    createHintsContainer() {
        this.hintsContainer = document.createElement('div');
        this.hintsContainer.id = 'voice-hints-container';
        this.hintsContainer.className = 'voice-hints-overlay';
        this.hintsContainer.style.cssText = `
            position: fixed;
            inset: 0;
            pointer-events: none;
            z-index: 9999;
            opacity: 0;
            transition: opacity 0.3s ease;
        `;
        document.body.appendChild(this.hintsContainer);

        // Listening indicator
        this.listeningIndicator = document.createElement('div');
        this.listeningIndicator.className = 'listening-indicator';
        this.listeningIndicator.innerHTML = `
            <div style="
                position: fixed;
                top: 20px;
                right: 20px;
                background: rgba(59, 130, 246, 0.95);
                color: white;
                padding: 12px 20px;
                border-radius: 24px;
                font-size: 14px;
                font-weight: 600;
                display: none;
                align-items: center;
                gap: 10px;
                box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
                pointer-events: all;
                cursor: pointer;
            " id="listening-badge">
                <span style="
                    width: 8px;
                    height: 8px;
                    background: #ef4444;
                    border-radius: 50%;
                    animation: pulse 1.5s ease-in-out infinite;
                "></span>
                <span>Listening...</span>
            </div>
            <style>
                @keyframes pulse {
                    0%, 100% { opacity: 1; transform: scale(1); }
                    50% { opacity: 0.5; transform: scale(1.2); }
                }
            </style>
        `;
        document.body.appendChild(this.listeningIndicator);

        // Click listening badge to stop
        const badge = document.getElementById('listening-badge');
        if (badge) {
            badge.addEventListener('click', () => this.stopListening());
        }
    }

    /**
     * Show voice hints for all registered elements
     */
    showVoiceHints() {
        if (this.voiceHintsVisible) return;

        this.hintsContainer.innerHTML = '';

        this.commands.forEach((cmdData, elementId) => {
            const element = cmdData.element;
            if (!element || !element.offsetParent) return; // Skip hidden elements

            const hint = this.createVoiceHint(element, cmdData);
            this.hintsContainer.appendChild(hint);
        });

        this.hintsContainer.style.opacity = this.themeEngine.settings.voiceHintsOpacity;
        this.voiceHintsVisible = true;

        // Auto-hide after 10 seconds
        setTimeout(() => {
            if (this.voiceHintsVisible) {
                this.hideVoiceHints();
            }
        }, 10000);
    }

    /**
     * Create voice hint for element
     */
    createVoiceHint(element, cmdData) {
        const rect = element.getBoundingClientRect();
        const hint = document.createElement('div');
        hint.className = 'voice-hint';

        const position = this.themeEngine.settings.voiceHintsPosition;

        if (position === 'floating') {
            // Floating badge style
            hint.style.cssText = `
                position: absolute;
                left: ${rect.left + rect.width / 2}px;
                top: ${rect.top - 32}px;
                transform: translateX(-50%);
                background: rgba(59, 130, 246, 0.95);
                color: white;
                padding: 6px 12px;
                border-radius: 12px;
                font-size: 11px;
                font-weight: 600;
                white-space: nowrap;
                pointer-events: all;
                box-shadow: 0 2px 8px rgba(59, 130, 246, 0.4);
                animation: voiceHintFadeIn 0.3s ease;
            `;
        } else if (position === 'overlay') {
            // Overlay on element
            hint.style.cssText = `
                position: absolute;
                left: ${rect.left}px;
                top: ${rect.top}px;
                width: ${rect.width}px;
                height: ${rect.height}px;
                background: rgba(59, 130, 246, 0.2);
                border: 2px solid rgba(59, 130, 246, 0.8);
                border-radius: 8px;
                display: flex;
                align-items: center;
                justify-content: center;
                color: white;
                font-size: 12px;
                font-weight: 600;
                pointer-events: all;
                backdrop-filter: blur(4px);
            `;
        }

        hint.innerHTML = `
            <span style="display: flex; align-items: center; gap: 6px;">
                <span style="opacity: 0.8;">🎤</span>
                <span>"${cmdData.primary}"</span>
            </span>
        `;

        // Add tooltip with description
        if (cmdData.description) {
            hint.title = cmdData.description;
        }

        return hint;
    }

    /**
     * Show voice hint for specific element
     */
    showVoiceHintForElement(element) {
        const cmdData = this.commands.get(element.id);
        if (!cmdData) return;

        // Create temporary hint
        const hint = this.createVoiceHint(element, cmdData);
        hint.style.opacity = '0';
        this.hintsContainer.appendChild(hint);

        requestAnimationFrame(() => {
            hint.style.opacity = '1';
            hint.style.transition = 'opacity 0.3s ease';
        });

        // Remove after 3 seconds
        setTimeout(() => {
            hint.style.opacity = '0';
            setTimeout(() => hint.remove(), 300);
        }, 3000);
    }

    /**
     * Hide voice hints
     */
    hideVoiceHints() {
        this.hintsContainer.style.opacity = '0';
        this.voiceHintsVisible = false;
        setTimeout(() => {
            this.hintsContainer.innerHTML = '';
        }, 300);
    }

    /**
     * Toggle voice hints
     */
    toggleVoiceHints() {
        if (this.voiceHintsVisible) {
            this.hideVoiceHints();
        } else {
            this.showVoiceHints();
        }
    }

    /**
     * Start voice listening
     */
    startListening() {
        if (!this.recognition) {
            console.warn('Voice recognition not available');
            this.speak('Voice recognition is not available in this browser');
            return;
        }

        if (this.isListening) return;

        try {
            this.recognition.start();
            this.isListening = true;
            document.getElementById('listening-badge').style.display = 'flex';
            this.speak('Listening for voice commands');
        } catch (error) {
            console.error('Failed to start listening:', error);
        }
    }

    /**
     * Stop voice listening
     */
    stopListening() {
        if (!this.recognition || !this.isListening) return;

        this.recognition.stop();
        this.isListening = false;
        document.getElementById('listening-badge').style.display = 'none';
        this.speak('Stopped listening');
    }

    /**
     * Process voice command
     */
    processVoiceCommand(command) {
        console.log('Voice command:', command);

        // Check for element command
        const elementId = this.commandAliases.get(command);

        if (elementId) {
            if (typeof elementId === 'object' && elementId.isGlobal) {
                // Global command
                elementId.action();
                this.speak('Command executed');
            } else {
                // Element command
                const cmdData = this.commands.get(elementId);
                if (cmdData) {
                    cmdData.action();
                    this.speak(`${cmdData.primary} activated`);
                }
            }
            return true;
        }

        // No matching command
        this.speak('Command not recognized. Say "help" to see available commands.');
        return false;
    }

    /**
     * Text-to-speech
     */
    speak(text) {
        if ('speechSynthesis' in window) {
            const utterance = new SpeechSynthesisUtterance(text);
            utterance.rate = 1.2;
            utterance.pitch = 1.0;
            utterance.volume = 0.8;
            speechSynthesis.speak(utterance);
        }
    }

    /**
     * Announce available commands
     */
    announceAvailableCommands() {
        const commandList = Array.from(this.commands.values())
            .map(cmd => cmd.primary)
            .filter(cmd => cmd)
            .slice(0, 5)
            .join(', ');

        this.speak(`Available commands include: ${commandList}, and more. Say "show voice commands" to see all.`);
        this.showVoiceHints();
    }

    /**
     * Get all registered commands
     */
    getAllCommands() {
        const cmds = [];
        this.commands.forEach((cmdData, id) => {
            cmds.push({
                id: id,
                primary: cmdData.primary,
                alternatives: cmdData.alternatives,
                description: cmdData.description,
                context: cmdData.context
            });
        });
        return cmds;
    }

    /**
     * Update hints appearance based on theme settings
     */
    updateHintsAppearance() {
        if (this.voiceHintsVisible) {
            this.hintsContainer.style.opacity = this.themeEngine.settings.voiceHintsOpacity;
        }
    }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = VoiceCommandSystem;
}
