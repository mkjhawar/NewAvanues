/**
 * Avanues Gestures - Universal JavaScript Gesture Library
 *
 * Portable gesture execution library for WebView injection.
 * Uses only standard Web APIs for maximum compatibility.
 * Coordinates provided by VoiceOS VoiceCursor system.
 *
 * @version 1.0.0
 * @author Manoj Jhawar <manoj@ideahq.net>
 */

(function() {
    'use strict';

    // ========== Utility Functions ==========

    /**
     * Get element at coordinates
     */
    function getElementAt(x, y) {
        return document.elementFromPoint(x, y);
    }

    /**
     * Create and dispatch synthetic event
     */
    function fireEvent(element, eventType, options = {}) {
        if (!element) return false;

        const defaults = {
            bubbles: true,
            cancelable: true,
            view: window,
            clientX: options.x || 0,
            clientY: options.y || 0,
            screenX: options.x || 0,
            screenY: options.y || 0,
            button: options.button || 0,
            buttons: options.buttons || 1,
            detail: options.detail || 1
        };

        const eventOptions = { ...defaults, ...options };

        let event;
        if (eventType.startsWith('pointer')) {
            event = new PointerEvent(eventType, {
                ...eventOptions,
                pointerId: options.pointerId || 1,
                pointerType: options.pointerType || 'mouse',
                isPrimary: true
            });
        } else if (eventType.startsWith('touch')) {
            const touch = new Touch({
                identifier: options.identifier || 0,
                target: element,
                clientX: eventOptions.clientX,
                clientY: eventOptions.clientY,
                screenX: eventOptions.screenX,
                screenY: eventOptions.screenY,
                pageX: eventOptions.clientX + window.pageXOffset,
                pageY: eventOptions.clientY + window.pageYOffset
            });
            event = new TouchEvent(eventType, {
                bubbles: true,
                cancelable: true,
                touches: [touch],
                targetTouches: [touch],
                changedTouches: [touch]
            });
        } else {
            event = new MouseEvent(eventType, eventOptions);
        }

        return element.dispatchEvent(event);
    }

    /**
     * Smooth animation helper
     */
    function animate(duration, callback) {
        const start = performance.now();

        function step(timestamp) {
            const elapsed = timestamp - start;
            const progress = Math.min(elapsed / duration, 1);

            // Ease out cubic
            const eased = 1 - Math.pow(1 - progress, 3);

            callback(eased);

            if (progress < 1) {
                requestAnimationFrame(step);
            }
        }

        requestAnimationFrame(step);
    }

    /**
     * Get scroll container for element
     */
    function getScrollContainer(element) {
        if (!element) return document.documentElement;

        let current = element;
        while (current && current !== document.body) {
            const style = window.getComputedStyle(current);
            const overflow = style.overflow + style.overflowY + style.overflowX;

            if (/(auto|scroll)/.test(overflow)) {
                return current;
            }
            current = current.parentElement;
        }

        return document.documentElement;
    }

    // ========== State Management ==========

    const gestureState = {
        isDragging: false,
        dragElement: null,
        dragStartX: 0,
        dragStartY: 0,
        dragOffsetX: 0,
        dragOffsetY: 0,
        isDrawing: false,
        drawingPath: [],
        longPressTimer: null,
        grabbedElement: null,
        selectionStart: null
    };

    // ========== Basic Pointer Gestures ==========

    /**
     * Execute click at coordinates
     */
    function click(x, y) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        fireEvent(element, 'pointerdown', { x, y });
        fireEvent(element, 'mousedown', { x, y });

        setTimeout(() => {
            fireEvent(element, 'pointerup', { x, y });
            fireEvent(element, 'mouseup', { x, y });
            fireEvent(element, 'click', { x, y });
        }, 50);

        return { success: true, element: element.tagName };
    }

    /**
     * Execute double click at coordinates
     */
    function doubleClick(x, y) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        // First click
        fireEvent(element, 'pointerdown', { x, y });
        fireEvent(element, 'mousedown', { x, y });
        fireEvent(element, 'pointerup', { x, y });
        fireEvent(element, 'mouseup', { x, y });
        fireEvent(element, 'click', { x, y });

        // Second click (250ms delay)
        setTimeout(() => {
            fireEvent(element, 'pointerdown', { x, y });
            fireEvent(element, 'mousedown', { x, y });
            fireEvent(element, 'pointerup', { x, y });
            fireEvent(element, 'mouseup', { x, y });
            fireEvent(element, 'click', { x, y });
            fireEvent(element, 'dblclick', { x, y, detail: 2 });
        }, 100);

        return { success: true, element: element.tagName };
    }

    /**
     * Execute long press at coordinates
     */
    function longPress(x, y, duration = 800) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        fireEvent(element, 'pointerdown', { x, y });
        fireEvent(element, 'mousedown', { x, y });
        fireEvent(element, 'touchstart', { x, y });

        gestureState.longPressTimer = setTimeout(() => {
            // Trigger context menu event
            fireEvent(element, 'contextmenu', { x, y });

            fireEvent(element, 'pointerup', { x, y });
            fireEvent(element, 'mouseup', { x, y });
            fireEvent(element, 'touchend', { x, y });
        }, duration);

        return { success: true, element: element.tagName, duration };
    }

    /**
     * Tap gesture (quick touch)
     */
    function tap(x, y) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        fireEvent(element, 'touchstart', { x, y });

        setTimeout(() => {
            fireEvent(element, 'touchend', { x, y });
            fireEvent(element, 'click', { x, y });
        }, 50);

        return { success: true, element: element.tagName };
    }

    // ========== Drag Gestures ==========

    /**
     * Start drag operation
     */
    function dragStart(x, y) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        gestureState.isDragging = true;
        gestureState.dragElement = element;
        gestureState.dragStartX = x;
        gestureState.dragStartY = y;

        fireEvent(element, 'pointerdown', { x, y });
        fireEvent(element, 'mousedown', { x, y });
        fireEvent(element, 'dragstart', { x, y });

        return { success: true, element: element.tagName };
    }

    /**
     * Update drag position
     */
    function dragMove(x, y) {
        if (!gestureState.isDragging || !gestureState.dragElement) {
            return { success: false, error: 'No active drag operation' };
        }

        const element = gestureState.dragElement;

        fireEvent(element, 'pointermove', { x, y });
        fireEvent(element, 'mousemove', { x, y });
        fireEvent(element, 'drag', { x, y });

        return { success: true, dx: x - gestureState.dragStartX, dy: y - gestureState.dragStartY };
    }

    /**
     * End drag operation
     */
    function dragEnd(x, y) {
        if (!gestureState.isDragging || !gestureState.dragElement) {
            return { success: false, error: 'No active drag operation' };
        }

        const element = gestureState.dragElement;

        fireEvent(element, 'pointerup', { x, y });
        fireEvent(element, 'mouseup', { x, y });
        fireEvent(element, 'dragend', { x, y });

        const result = {
            success: true,
            totalDx: x - gestureState.dragStartX,
            totalDy: y - gestureState.dragStartY
        };

        gestureState.isDragging = false;
        gestureState.dragElement = null;

        return result;
    }

    /**
     * Execute complete drag operation
     */
    function drag(startX, startY, endX, endY, duration = 300) {
        const element = getElementAt(startX, startY);
        if (!element) return { success: false, error: 'No element at start coordinates' };

        fireEvent(element, 'pointerdown', { x: startX, y: startY });
        fireEvent(element, 'mousedown', { x: startX, y: startY });
        fireEvent(element, 'dragstart', { x: startX, y: startY });

        const steps = 20;
        const stepDelay = duration / steps;

        for (let i = 0; i <= steps; i++) {
            setTimeout(() => {
                const progress = i / steps;
                const x = startX + (endX - startX) * progress;
                const y = startY + (endY - startY) * progress;

                fireEvent(element, 'pointermove', { x, y });
                fireEvent(element, 'mousemove', { x, y });
                fireEvent(element, 'drag', { x, y });

                if (i === steps) {
                    fireEvent(element, 'pointerup', { x, y });
                    fireEvent(element, 'mouseup', { x, y });
                    fireEvent(element, 'dragend', { x, y });
                }
            }, i * stepDelay);
        }

        return { success: true, distance: Math.hypot(endX - startX, endY - startY) };
    }

    // ========== Swipe Gestures ==========

    /**
     * Swipe left from coordinates
     */
    function swipeLeft(x, y, distance = 200, duration = 200) {
        return swipe(x, y, x - distance, y, duration);
    }

    /**
     * Swipe right from coordinates
     */
    function swipeRight(x, y, distance = 200, duration = 200) {
        return swipe(x, y, x + distance, y, duration);
    }

    /**
     * Swipe up from coordinates
     */
    function swipeUp(x, y, distance = 200, duration = 200) {
        return swipe(x, y, x, y - distance, duration);
    }

    /**
     * Swipe down from coordinates
     */
    function swipeDown(x, y, distance = 200, duration = 200) {
        return swipe(x, y, x, y + distance, duration);
    }

    /**
     * Generic swipe operation
     */
    function swipe(startX, startY, endX, endY, duration = 200) {
        const element = getElementAt(startX, startY);
        if (!element) return { success: false, error: 'No element at start coordinates' };

        fireEvent(element, 'touchstart', { x: startX, y: startY });
        fireEvent(element, 'pointerdown', { x: startX, y: startY });

        animate(duration, (progress) => {
            const x = startX + (endX - startX) * progress;
            const y = startY + (endY - startY) * progress;

            fireEvent(element, 'touchmove', { x, y });
            fireEvent(element, 'pointermove', { x, y });

            if (progress === 1) {
                fireEvent(element, 'touchend', { x, y });
                fireEvent(element, 'pointerup', { x, y });
            }
        });

        return { success: true, direction: getSwipeDirection(startX, startY, endX, endY) };
    }

    function getSwipeDirection(startX, startY, endX, endY) {
        const dx = endX - startX;
        const dy = endY - startY;

        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? 'right' : 'left';
        } else {
            return dy > 0 ? 'down' : 'up';
        }
    }

    // ========== Text Selection ==========

    /**
     * Start text selection at coordinates
     */
    function selectStart(x, y) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        gestureState.selectionStart = { x, y, element };

        // Create selection
        const range = document.caretRangeFromPoint(x, y);
        if (range) {
            const selection = window.getSelection();
            selection.removeAllRanges();
            selection.addRange(range);
        }

        return { success: true, element: element.tagName };
    }

    /**
     * Extend selection to coordinates
     */
    function selectExtend(x, y) {
        if (!gestureState.selectionStart) {
            return { success: false, error: 'No selection started' };
        }

        const range = document.caretRangeFromPoint(x, y);
        if (range) {
            const selection = window.getSelection();
            if (selection.rangeCount > 0) {
                selection.extend(range.startContainer, range.startOffset);
            }
        }

        const selection = window.getSelection();
        return {
            success: true,
            selectedText: selection.toString().substring(0, 100),
            length: selection.toString().length
        };
    }

    /**
     * Select word at coordinates
     */
    function selectWord(x, y) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        const range = document.caretRangeFromPoint(x, y);
        if (range) {
            range.expand('word');
            const selection = window.getSelection();
            selection.removeAllRanges();
            selection.addRange(range);

            return { success: true, selectedText: selection.toString() };
        }

        return { success: false, error: 'Could not create selection range' };
    }

    /**
     * Select all content
     */
    function selectAll() {
        const selection = window.getSelection();
        const range = document.createRange();
        range.selectNodeContents(document.body);
        selection.removeAllRanges();
        selection.addRange(range);

        return { success: true, length: selection.toString().length };
    }

    /**
     * Clear selection
     */
    function clearSelection() {
        const selection = window.getSelection();
        selection.removeAllRanges();
        gestureState.selectionStart = null;

        return { success: true };
    }

    // ========== Clipboard Operations ==========

    /**
     * Copy selected text to clipboard
     */
    async function copy() {
        const selection = window.getSelection();
        const text = selection.toString();

        if (!text) {
            return { success: false, error: 'No text selected' };
        }

        try {
            if (navigator.clipboard && navigator.clipboard.writeText) {
                await navigator.clipboard.writeText(text);
                return { success: true, length: text.length };
            } else {
                // Fallback: execCommand
                document.execCommand('copy');
                return { success: true, length: text.length, method: 'execCommand' };
            }
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Cut selected text to clipboard
     */
    async function cut() {
        const selection = window.getSelection();
        const text = selection.toString();

        if (!text) {
            return { success: false, error: 'No text selected' };
        }

        try {
            if (navigator.clipboard && navigator.clipboard.writeText) {
                await navigator.clipboard.writeText(text);
                document.execCommand('delete');
                return { success: true, length: text.length };
            } else {
                document.execCommand('cut');
                return { success: true, length: text.length, method: 'execCommand' };
            }
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Paste from clipboard at coordinates
     */
    async function paste(x, y) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        try {
            if (navigator.clipboard && navigator.clipboard.readText) {
                const text = await navigator.clipboard.readText();

                // Focus element if it's an input
                if (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA') {
                    element.focus();
                    const start = element.selectionStart || 0;
                    const end = element.selectionEnd || 0;
                    element.value = element.value.substring(0, start) + text + element.value.substring(end);
                    element.selectionStart = element.selectionEnd = start + text.length;
                } else {
                    document.execCommand('insertText', false, text);
                }

                return { success: true, length: text.length };
            } else {
                element.focus();
                document.execCommand('paste');
                return { success: true, method: 'execCommand' };
            }
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    // ========== 3D Transform Gestures ==========

    /**
     * Apply CSS transform to element at coordinates
     */
    function applyTransform(element, transform) {
        if (!element) return false;

        const currentTransform = element.style.transform || '';
        element.style.transform = currentTransform + ' ' + transform;
        return true;
    }

    /**
     * Rotate element around X axis
     */
    function rotateX(x, y, angle) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        applyTransform(element, `rotateX(${angle}deg)`);
        return { success: true, element: element.tagName, angle };
    }

    /**
     * Rotate element around Y axis
     */
    function rotateY(x, y, angle) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        applyTransform(element, `rotateY(${angle}deg)`);
        return { success: true, element: element.tagName, angle };
    }

    /**
     * Rotate element around Z axis (2D rotation)
     */
    function rotateZ(x, y, angle) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        applyTransform(element, `rotateZ(${angle}deg)`);
        return { success: true, element: element.tagName, angle };
    }

    /**
     * Pan viewport
     */
    function pan(dx, dy) {
        window.scrollBy({
            left: dx,
            top: dy,
            behavior: 'smooth'
        });

        return { success: true, dx, dy };
    }

    /**
     * Tilt view (3D perspective)
     */
    function tilt(x, y, angle) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        element.style.transformStyle = 'preserve-3d';
        element.style.perspective = '1000px';
        applyTransform(element, `rotateX(${angle}deg)`);

        return { success: true, element: element.tagName, angle };
    }

    /**
     * Orbit camera around element
     */
    function orbit(x, y, deltaX, deltaY) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        const rotY = deltaX * 0.5;
        const rotX = deltaY * 0.5;

        element.style.transformStyle = 'preserve-3d';
        element.style.perspective = '1000px';
        applyTransform(element, `rotateY(${rotY}deg) rotateX(${rotX}deg)`);

        return { success: true, element: element.tagName, rotX, rotY };
    }

    // ========== Zoom/Scale Gestures ==========

    /**
     * Zoom in at coordinates
     */
    function zoomIn(x, y, factor = 1.2) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        const scrollContainer = getScrollContainer(element);
        const currentZoom = parseFloat(scrollContainer.style.zoom || '1');
        const newZoom = currentZoom * factor;

        scrollContainer.style.zoom = newZoom;

        return { success: true, zoom: newZoom, element: element.tagName };
    }

    /**
     * Zoom out at coordinates
     */
    function zoomOut(x, y, factor = 0.8) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        const scrollContainer = getScrollContainer(element);
        const currentZoom = parseFloat(scrollContainer.style.zoom || '1');
        const newZoom = currentZoom * factor;

        scrollContainer.style.zoom = newZoom;

        return { success: true, zoom: newZoom, element: element.tagName };
    }

    /**
     * Reset zoom
     */
    function resetZoom() {
        document.documentElement.style.zoom = '1';
        document.body.style.zoom = '1';

        return { success: true, zoom: 1 };
    }

    /**
     * Scale element at coordinates
     */
    function scale(x, y, factor) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        applyTransform(element, `scale(${factor})`);

        return { success: true, element: element.tagName, factor };
    }

    /**
     * Pinch gesture (two-finger zoom)
     */
    function pinch(x1, y1, x2, y2, scale) {
        const centerX = (x1 + x2) / 2;
        const centerY = (y1 + y2) / 2;

        const element = getElementAt(centerX, centerY);
        if (!element) return { success: false, error: 'No element at coordinates' };

        applyTransform(element, `scale(${scale})`);

        return { success: true, element: element.tagName, scale };
    }

    // ========== Scrolling Gestures ==========

    /**
     * Scroll to absolute position
     */
    function scrollTo(x, y) {
        window.scrollTo({
            left: x,
            top: y,
            behavior: 'smooth'
        });

        return { success: true, x, y };
    }

    /**
     * Scroll by relative amount
     */
    function scrollBy(dx, dy) {
        window.scrollBy({
            left: dx,
            top: dy,
            behavior: 'smooth'
        });

        return { success: true, dx, dy };
    }

    /**
     * Scroll element at coordinates
     */
    function scrollElement(x, y, dx, dy) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        const scrollContainer = getScrollContainer(element);
        scrollContainer.scrollBy({
            left: dx,
            top: dy,
            behavior: 'smooth'
        });

        return { success: true, element: scrollContainer.tagName };
    }

    /**
     * Fling scroll with momentum
     */
    function fling(velocity, direction) {
        const duration = 1000;
        const distance = velocity * duration / 1000;

        let dx = 0, dy = 0;
        switch (direction) {
            case 'up': dy = -distance; break;
            case 'down': dy = distance; break;
            case 'left': dx = -distance; break;
            case 'right': dx = distance; break;
        }

        animate(duration, (progress) => {
            const eased = 1 - Math.pow(1 - progress, 2); // Ease out
            window.scrollBy({
                left: dx * eased / 100,
                top: dy * eased / 100,
                behavior: 'auto'
            });
        });

        return { success: true, velocity, direction, distance };
    }

    /**
     * Scroll to top
     */
    function scrollToTop() {
        window.scrollTo({ top: 0, behavior: 'smooth' });
        return { success: true };
    }

    /**
     * Scroll to bottom
     */
    function scrollToBottom() {
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
        return { success: true };
    }

    /**
     * Page up
     */
    function pageUp() {
        window.scrollBy({ top: -window.innerHeight * 0.9, behavior: 'smooth' });
        return { success: true };
    }

    /**
     * Page down
     */
    function pageDown() {
        window.scrollBy({ top: window.innerHeight * 0.9, behavior: 'smooth' });
        return { success: true };
    }

    // ========== Grab Gestures ==========

    /**
     * Grab element at coordinates
     */
    function grab(x, y) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        gestureState.grabbedElement = element;
        gestureState.dragOffsetX = x - element.offsetLeft;
        gestureState.dragOffsetY = y - element.offsetTop;

        element.style.cursor = 'grabbing';
        element.style.position = 'absolute';
        element.style.zIndex = '9999';

        return { success: true, element: element.tagName };
    }

    /**
     * Move grabbed element
     */
    function moveGrabbed(x, y) {
        if (!gestureState.grabbedElement) {
            return { success: false, error: 'No element grabbed' };
        }

        const element = gestureState.grabbedElement;
        element.style.left = (x - gestureState.dragOffsetX) + 'px';
        element.style.top = (y - gestureState.dragOffsetY) + 'px';

        return { success: true, x, y };
    }

    /**
     * Release grabbed element
     */
    function release() {
        if (!gestureState.grabbedElement) {
            return { success: false, error: 'No element grabbed' };
        }

        const element = gestureState.grabbedElement;
        element.style.cursor = '';

        gestureState.grabbedElement = null;

        return { success: true, element: element.tagName };
    }

    /**
     * Throw grabbed element with momentum
     */
    function throwElement(velocityX, velocityY) {
        if (!gestureState.grabbedElement) {
            return { success: false, error: 'No element grabbed' };
        }

        const element = gestureState.grabbedElement;
        const startX = parseInt(element.style.left) || 0;
        const startY = parseInt(element.style.top) || 0;

        const duration = 500;
        const friction = 0.95;

        animate(duration, (progress) => {
            const damping = Math.pow(friction, progress * 50);
            const x = startX + velocityX * progress * damping;
            const y = startY + velocityY * progress * damping;

            element.style.left = x + 'px';
            element.style.top = y + 'px';
        });

        gestureState.grabbedElement = null;
        element.style.cursor = '';

        return { success: true, velocityX, velocityY };
    }

    // ========== Drawing Gestures ==========

    /**
     * Start drawing stroke
     */
    function strokeStart(x, y) {
        gestureState.isDrawing = true;
        gestureState.drawingPath = [{ x, y }];

        return { success: true, x, y };
    }

    /**
     * Continue drawing stroke
     */
    function strokeMove(x, y) {
        if (!gestureState.isDrawing) {
            return { success: false, error: 'No active drawing' };
        }

        gestureState.drawingPath.push({ x, y });

        return { success: true, pathLength: gestureState.drawingPath.length };
    }

    /**
     * End drawing stroke
     */
    function strokeEnd() {
        if (!gestureState.isDrawing) {
            return { success: false, error: 'No active drawing' };
        }

        const path = [...gestureState.drawingPath];
        gestureState.isDrawing = false;
        gestureState.drawingPath = [];

        return { success: true, points: path.length, path };
    }

    /**
     * Get current drawing path
     */
    function getDrawingPath() {
        return {
            success: true,
            isDrawing: gestureState.isDrawing,
            path: gestureState.drawingPath
        };
    }

    /**
     * Erase at coordinates
     */
    function erase(x, y, radius = 10) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        // For canvas elements
        if (element.tagName === 'CANVAS') {
            const ctx = element.getContext('2d');
            if (ctx) {
                ctx.clearRect(x - radius, y - radius, radius * 2, radius * 2);
                return { success: true, element: 'CANVAS', radius };
            }
        }

        return { success: false, error: 'Erase only works on canvas elements' };
    }

    // ========== Focus & Input Gestures ==========

    /**
     * Focus element at coordinates
     */
    function focus(x, y) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        if (element.focus) {
            element.focus();
            return { success: true, element: element.tagName };
        }

        return { success: false, error: 'Element is not focusable' };
    }

    /**
     * Type text into focused element
     */
    function type(text) {
        const element = document.activeElement;
        if (!element) return { success: false, error: 'No focused element' };

        if (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA') {
            const start = element.selectionStart || 0;
            const end = element.selectionEnd || 0;
            element.value = element.value.substring(0, start) + text + element.value.substring(end);
            element.selectionStart = element.selectionEnd = start + text.length;

            // Trigger input event
            element.dispatchEvent(new Event('input', { bubbles: true }));

            return { success: true, length: text.length };
        }

        return { success: false, error: 'Focused element is not an input' };
    }

    // ========== Hover Gestures ==========

    /**
     * Hover over element at coordinates
     */
    function hover(x, y) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        fireEvent(element, 'pointerenter', { x, y });
        fireEvent(element, 'mouseenter', { x, y });
        fireEvent(element, 'mouseover', { x, y });

        return { success: true, element: element.tagName };
    }

    /**
     * Leave hover from element
     */
    function hoverOut(x, y) {
        const element = getElementAt(x, y);
        if (!element) return { success: false, error: 'No element at coordinates' };

        fireEvent(element, 'pointerleave', { x, y });
        fireEvent(element, 'mouseleave', { x, y });
        fireEvent(element, 'mouseout', { x, y });

        return { success: true, element: element.tagName };
    }

    // ========== Public API ==========

    window.AvanuesGestures = {
        // Basic pointer
        click,
        doubleClick,
        longPress,
        tap,

        // Drag
        dragStart,
        dragMove,
        dragEnd,
        drag,

        // Swipe
        swipe,
        swipeLeft,
        swipeRight,
        swipeUp,
        swipeDown,

        // Selection
        selectStart,
        selectExtend,
        selectWord,
        selectAll,
        clearSelection,

        // Clipboard
        copy,
        cut,
        paste,

        // 3D transforms
        rotateX,
        rotateY,
        rotateZ,
        pan,
        tilt,
        orbit,

        // Zoom/Scale
        zoomIn,
        zoomOut,
        resetZoom,
        scale,
        pinch,

        // Scrolling
        scrollTo,
        scrollBy,
        scrollElement,
        fling,
        scrollToTop,
        scrollToBottom,
        pageUp,
        pageDown,

        // Grab
        grab,
        moveGrabbed,
        release,
        throwElement,

        // Drawing
        strokeStart,
        strokeMove,
        strokeEnd,
        getDrawingPath,
        erase,

        // Focus & Input
        focus,
        type,

        // Hover
        hover,
        hoverOut,

        // Utilities
        getElementAt,
        fireEvent,
        getState: () => ({ ...gestureState })
    };

    // Initialize message
    console.log('[Avanues Gestures] Library loaded - 60+ gestures available');

})();
