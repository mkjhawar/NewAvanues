/**
 * WebActions - Web Element Action Extraction Library
 *
 * Extracts clickable/actionable elements from web pages and converts them
 * to voice-friendly commands for AVA voice assistant integration.
 *
 * Part of WebAvanue browser module.
 *
 * @version 1.0.0
 * @created 2025-12-01
 */

(function(window) {
    'use strict';

    // ==================== Configuration ====================

    const CONFIG = {
        // Maximum elements to extract per type
        maxButtons: 20,
        maxLinks: 30,
        maxInputs: 15,
        maxTotal: 50,

        // Minimum text length for valid voice label
        minLabelLength: 2,
        maxLabelLength: 50,

        // Element visibility threshold (0-1)
        visibilityThreshold: 0.5,

        // Z-index minimum for visible elements
        minZIndex: 0
    };

    // ==================== Element Types ====================

    const ActionType = {
        BUTTON: 'button',
        LINK: 'link',
        INPUT: 'input',
        SELECT: 'select',
        CHECKBOX: 'checkbox',
        RADIO: 'radio',
        TOGGLE: 'toggle',
        MENU_ITEM: 'menu_item',
        TAB: 'tab',
        MEDIA_CONTROL: 'media_control',
        CUSTOM: 'custom'
    };

    // ==================== Utility Functions ====================

    /**
     * Check if element is visible in viewport
     */
    function isElementVisible(element) {
        if (!element) return false;

        const style = window.getComputedStyle(element);
        if (style.display === 'none' ||
            style.visibility === 'hidden' ||
            style.opacity === '0') {
            return false;
        }

        const rect = element.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
            return false;
        }

        // Check if partially visible in viewport
        const viewportWidth = window.innerWidth || document.documentElement.clientWidth;
        const viewportHeight = window.innerHeight || document.documentElement.clientHeight;

        return (
            rect.top < viewportHeight &&
            rect.bottom > 0 &&
            rect.left < viewportWidth &&
            rect.right > 0
        );
    }

    /**
     * Get human-readable label for element
     */
    function getElementLabel(element) {
        // Priority order for labels
        const sources = [
            // Explicit labels
            () => element.getAttribute('aria-label'),
            () => element.getAttribute('title'),
            () => element.getAttribute('alt'),
            () => element.getAttribute('placeholder'),

            // Text content
            () => {
                const text = element.innerText || element.textContent;
                return text ? text.trim().split('\n')[0] : null;
            },

            // Value for inputs
            () => element.value,

            // Associated label
            () => {
                const id = element.id;
                if (id) {
                    const label = document.querySelector(`label[for="${id}"]`);
                    return label ? label.textContent.trim() : null;
                }
                return null;
            },

            // Name attribute
            () => element.getAttribute('name'),

            // Class-based inference
            () => {
                const classes = element.className;
                if (typeof classes === 'string') {
                    // Convert class names to readable labels
                    const match = classes.match(/btn[-_]?(\w+)|button[-_]?(\w+)/i);
                    if (match) {
                        return (match[1] || match[2]).replace(/[-_]/g, ' ');
                    }
                }
                return null;
            }
        ];

        for (const getLabel of sources) {
            try {
                const label = getLabel();
                if (label &&
                    label.length >= CONFIG.minLabelLength &&
                    label.length <= CONFIG.maxLabelLength) {
                    // Clean up label
                    return label
                        .replace(/\s+/g, ' ')
                        .replace(/[^\w\s-]/g, '')
                        .trim()
                        .toLowerCase();
                }
            } catch (e) {
                // Continue to next source
            }
        }

        return null;
    }

    /**
     * Generate unique voice command from label
     */
    function generateVoiceCommand(label, type, index) {
        if (!label) {
            // Generate fallback based on type
            return `${type} ${index + 1}`;
        }

        // Simplify common patterns
        const simplified = label
            .replace(/click here/i, 'here')
            .replace(/learn more/i, 'learn')
            .replace(/read more/i, 'read')
            .replace(/sign up/i, 'signup')
            .replace(/sign in/i, 'signin')
            .replace(/log in/i, 'login')
            .replace(/log out/i, 'logout')
            .replace(/submit/i, 'submit')
            .replace(/search/i, 'search')
            .replace(/next/i, 'next')
            .replace(/previous|prev/i, 'previous')
            .replace(/close/i, 'close')
            .replace(/open/i, 'open')
            .replace(/menu/i, 'menu');

        return simplified;
    }

    /**
     * Get element coordinates for clicking
     */
    function getElementCoordinates(element) {
        const rect = element.getBoundingClientRect();
        return {
            x: Math.round(rect.left + rect.width / 2),
            y: Math.round(rect.top + rect.height / 2),
            width: Math.round(rect.width),
            height: Math.round(rect.height)
        };
    }

    /**
     * Create CSS selector for element (for later retrieval)
     */
    function createSelector(element) {
        // ID-based selector (most reliable)
        if (element.id) {
            return `#${element.id}`;
        }

        // Build path-based selector
        const path = [];
        let current = element;

        while (current && current !== document.body && path.length < 5) {
            let selector = current.tagName.toLowerCase();

            // Add classes if present
            if (current.className && typeof current.className === 'string') {
                const classes = current.className.trim().split(/\s+/).slice(0, 2);
                if (classes.length > 0) {
                    selector += '.' + classes.join('.');
                }
            }

            // Add nth-child if needed
            const parent = current.parentElement;
            if (parent) {
                const siblings = Array.from(parent.children).filter(
                    c => c.tagName === current.tagName
                );
                if (siblings.length > 1) {
                    const index = siblings.indexOf(current) + 1;
                    selector += `:nth-of-type(${index})`;
                }
            }

            path.unshift(selector);
            current = current.parentElement;
        }

        return path.join(' > ');
    }

    // ==================== Extraction Functions ====================

    /**
     * Extract button elements
     */
    function extractButtons() {
        const buttons = [];
        const selectors = [
            'button',
            'input[type="button"]',
            'input[type="submit"]',
            'input[type="reset"]',
            '[role="button"]',
            '.btn',
            '.button',
            '[onclick]'
        ];

        const elements = document.querySelectorAll(selectors.join(', '));

        for (const element of elements) {
            if (buttons.length >= CONFIG.maxButtons) break;
            if (!isElementVisible(element)) continue;

            const label = getElementLabel(element);
            if (!label && !element.id) continue; // Skip unlabeled buttons without ID

            buttons.push({
                type: ActionType.BUTTON,
                label: label,
                voiceCommand: generateVoiceCommand(label, 'button', buttons.length),
                selector: createSelector(element),
                coordinates: getElementCoordinates(element),
                attributes: {
                    disabled: element.disabled || element.getAttribute('aria-disabled') === 'true',
                    form: element.form ? element.form.id : null
                }
            });
        }

        return buttons;
    }

    /**
     * Extract link elements
     */
    function extractLinks() {
        const links = [];
        const elements = document.querySelectorAll('a[href]');

        for (const element of elements) {
            if (links.length >= CONFIG.maxLinks) break;
            if (!isElementVisible(element)) continue;

            const label = getElementLabel(element);
            const href = element.getAttribute('href');

            // Skip empty, javascript:, and anchor-only links without label
            if (!href || href === '#' || href.startsWith('javascript:')) {
                if (!label) continue;
            }

            links.push({
                type: ActionType.LINK,
                label: label,
                voiceCommand: generateVoiceCommand(label, 'link', links.length),
                selector: createSelector(element),
                coordinates: getElementCoordinates(element),
                attributes: {
                    href: href,
                    target: element.getAttribute('target'),
                    external: element.getAttribute('target') === '_blank'
                }
            });
        }

        return links;
    }

    /**
     * Extract input elements
     */
    function extractInputs() {
        const inputs = [];
        const elements = document.querySelectorAll(
            'input:not([type="hidden"]):not([type="button"]):not([type="submit"]):not([type="reset"]), ' +
            'textarea, ' +
            'select, ' +
            '[contenteditable="true"]'
        );

        for (const element of elements) {
            if (inputs.length >= CONFIG.maxInputs) break;
            if (!isElementVisible(element)) continue;

            const label = getElementLabel(element);
            const type = element.type || element.tagName.toLowerCase();

            let actionType = ActionType.INPUT;
            if (type === 'checkbox') actionType = ActionType.CHECKBOX;
            else if (type === 'radio') actionType = ActionType.RADIO;
            else if (element.tagName === 'SELECT') actionType = ActionType.SELECT;

            inputs.push({
                type: actionType,
                label: label,
                voiceCommand: generateVoiceCommand(label, type, inputs.length),
                selector: createSelector(element),
                coordinates: getElementCoordinates(element),
                attributes: {
                    inputType: type,
                    required: element.required,
                    placeholder: element.placeholder,
                    value: element.value,
                    checked: element.checked
                }
            });
        }

        return inputs;
    }

    /**
     * Extract media control elements
     */
    function extractMediaControls() {
        const controls = [];

        // Video/Audio controls
        const mediaElements = document.querySelectorAll('video, audio');
        for (const media of mediaElements) {
            if (!isElementVisible(media)) continue;

            controls.push({
                type: ActionType.MEDIA_CONTROL,
                label: media.paused ? 'play video' : 'pause video',
                voiceCommand: media.paused ? 'play' : 'pause',
                selector: createSelector(media),
                coordinates: getElementCoordinates(media),
                attributes: {
                    mediaType: media.tagName.toLowerCase(),
                    paused: media.paused,
                    muted: media.muted,
                    duration: media.duration
                }
            });
        }

        // Custom video players (YouTube, Vimeo, etc.)
        const customPlayers = document.querySelectorAll(
            '.ytp-play-button, ' +
            '.vp-play-button, ' +
            '[aria-label*="play"], ' +
            '[aria-label*="pause"]'
        );

        for (const player of customPlayers) {
            if (controls.length >= 5) break;
            if (!isElementVisible(player)) continue;

            const label = getElementLabel(player);
            controls.push({
                type: ActionType.MEDIA_CONTROL,
                label: label,
                voiceCommand: generateVoiceCommand(label, 'media', controls.length),
                selector: createSelector(player),
                coordinates: getElementCoordinates(player),
                attributes: {
                    custom: true
                }
            });
        }

        return controls;
    }

    /**
     * Extract navigation/menu items
     */
    function extractMenuItems() {
        const items = [];
        const selectors = [
            'nav a',
            '[role="menuitem"]',
            '[role="tab"]',
            '.nav-link',
            '.menu-item',
            '.tab',
            'header a'
        ];

        const elements = document.querySelectorAll(selectors.join(', '));

        for (const element of elements) {
            if (items.length >= 15) break;
            if (!isElementVisible(element)) continue;

            const label = getElementLabel(element);
            if (!label) continue;

            // Check if it's a tab
            const isTab = element.getAttribute('role') === 'tab' ||
                          element.classList.contains('tab');

            items.push({
                type: isTab ? ActionType.TAB : ActionType.MENU_ITEM,
                label: label,
                voiceCommand: generateVoiceCommand(label, isTab ? 'tab' : 'menu', items.length),
                selector: createSelector(element),
                coordinates: getElementCoordinates(element),
                attributes: {
                    active: element.classList.contains('active') ||
                            element.getAttribute('aria-selected') === 'true',
                    href: element.getAttribute('href')
                }
            });
        }

        return items;
    }

    // ==================== Main API ====================

    const WebActions = {
        /**
         * Extract all actionable elements from current page
         * @returns {Object} Extracted actions grouped by type
         */
        extractAll: function() {
            const startTime = performance.now();

            const actions = {
                buttons: extractButtons(),
                links: extractLinks(),
                inputs: extractInputs(),
                menuItems: extractMenuItems(),
                mediaControls: extractMediaControls(),

                // Metadata
                meta: {
                    url: window.location.href,
                    title: document.title,
                    timestamp: Date.now(),
                    extractionTime: 0,
                    totalCount: 0
                }
            };

            // Calculate totals
            actions.meta.totalCount =
                actions.buttons.length +
                actions.links.length +
                actions.inputs.length +
                actions.menuItems.length +
                actions.mediaControls.length;

            actions.meta.extractionTime = Math.round(performance.now() - startTime);

            return actions;
        },

        /**
         * Extract only visible viewport elements (faster)
         * @returns {Object} Extracted actions from visible viewport
         */
        extractViewport: function() {
            // Same as extractAll but already filters for visible elements
            return this.extractAll();
        },

        /**
         * Get flat list of all voice commands
         * @returns {Array} Array of {command, type, selector, coordinates}
         */
        getVoiceCommands: function() {
            const actions = this.extractAll();
            const commands = [];
            const usedCommands = new Set();

            const processItems = (items) => {
                for (const item of items) {
                    let command = item.voiceCommand;

                    // Ensure uniqueness
                    let suffix = 1;
                    while (usedCommands.has(command)) {
                        command = `${item.voiceCommand} ${++suffix}`;
                    }
                    usedCommands.add(command);

                    commands.push({
                        command: command,
                        type: item.type,
                        label: item.label,
                        selector: item.selector,
                        coordinates: item.coordinates
                    });
                }
            };

            processItems(actions.buttons);
            processItems(actions.links);
            processItems(actions.inputs);
            processItems(actions.menuItems);
            processItems(actions.mediaControls);

            // Sort by position (top to bottom, left to right)
            commands.sort((a, b) => {
                const yDiff = a.coordinates.y - b.coordinates.y;
                if (Math.abs(yDiff) > 50) return yDiff;
                return a.coordinates.x - b.coordinates.x;
            });

            // Limit total
            return commands.slice(0, CONFIG.maxTotal);
        },

        /**
         * Click element by voice command
         * @param {string} command Voice command to match
         * @returns {Object} Result with success/failure
         */
        clickByCommand: function(command) {
            const commands = this.getVoiceCommands();
            const normalized = command.toLowerCase().trim();

            // Find matching command
            const match = commands.find(c =>
                c.command === normalized ||
                c.command.includes(normalized) ||
                (c.label && c.label.includes(normalized))
            );

            if (!match) {
                return {
                    success: false,
                    error: 'Command not found',
                    command: command,
                    availableCommands: commands.slice(0, 10).map(c => c.command)
                };
            }

            // Find and click element
            const element = document.querySelector(match.selector);
            if (!element) {
                return {
                    success: false,
                    error: 'Element not found',
                    selector: match.selector
                };
            }

            // Perform click
            try {
                // Focus first if input
                if (match.type === 'input' || match.type === 'select') {
                    element.focus();
                } else {
                    element.click();
                }

                return {
                    success: true,
                    type: match.type,
                    command: match.command,
                    label: match.label
                };
            } catch (e) {
                return {
                    success: false,
                    error: e.message
                };
            }
        },

        /**
         * Click element at coordinates
         * @param {number} x X coordinate
         * @param {number} y Y coordinate
         * @returns {Object} Result
         */
        clickAt: function(x, y) {
            const element = document.elementFromPoint(x, y);
            if (!element) {
                return { success: false, error: 'No element at coordinates' };
            }

            try {
                element.click();
                return {
                    success: true,
                    tagName: element.tagName,
                    label: getElementLabel(element)
                };
            } catch (e) {
                return { success: false, error: e.message };
            }
        },

        /**
         * Type text into focused/specified element
         * @param {string} text Text to type
         * @param {string} selector Optional CSS selector
         * @returns {Object} Result
         */
        typeText: function(text, selector) {
            let element = selector
                ? document.querySelector(selector)
                : document.activeElement;

            if (!element ||
                (element.tagName !== 'INPUT' &&
                 element.tagName !== 'TEXTAREA' &&
                 !element.isContentEditable)) {
                return { success: false, error: 'No input element focused' };
            }

            try {
                if (element.isContentEditable) {
                    element.textContent = text;
                } else {
                    element.value = text;
                    element.dispatchEvent(new Event('input', { bubbles: true }));
                }
                return { success: true };
            } catch (e) {
                return { success: false, error: e.message };
            }
        },

        /**
         * Get element info at coordinates
         * @param {number} x X coordinate
         * @param {number} y Y coordinate
         * @returns {Object} Element information
         */
        getElementAt: function(x, y) {
            const element = document.elementFromPoint(x, y);
            if (!element) return null;

            return {
                tagName: element.tagName,
                label: getElementLabel(element),
                selector: createSelector(element),
                coordinates: getElementCoordinates(element),
                isClickable: element.tagName === 'A' ||
                             element.tagName === 'BUTTON' ||
                             element.onclick !== null ||
                             element.getAttribute('role') === 'button'
            };
        },

        // Configuration
        config: CONFIG
    };

    // Expose to global scope
    window.AvanuesWebActions = WebActions;

})(window);
