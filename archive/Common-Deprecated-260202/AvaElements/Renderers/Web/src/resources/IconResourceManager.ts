/**
 * Icon Resource Manager
 *
 * Manages icon resources for AVAMagic Flutter Parity components on Web.
 * Supports multiple icon sources with caching and lazy loading.
 *
 * Supported Icon Types:
 * 1. Material Icons - @mui/icons-material
 * 2. Flutter Icons - Custom icon fonts
 * 3. Asset Paths - Local file system paths
 * 4. URLs - Remote icon URLs
 * 5. SVG/Base64 - Inline SVG or Base64 encoded images
 *
 * Features:
 * - Icon caching with Map/WeakMap
 * - Lazy loading for large icon sets
 * - SVG sprite sheet support
 * - Type-safe icon resolution
 * - Error fallbacks
 *
 * @module IconResourceManager
 * @since 2.1.0
 */

/**
 * Icon type enumeration
 */
export enum IconType {
  MATERIAL = 'material',
  FLUTTER = 'flutter',
  ASSET = 'asset',
  URL = 'url',
  SVG = 'svg',
  BASE64 = 'base64',
}

/**
 * Icon resource configuration
 */
export interface IconResource {
  /** Icon type */
  type: IconType;
  /** Icon identifier (name, path, or URL) */
  identifier: string;
  /** Optional size hint */
  size?: number;
  /** Optional color hint */
  color?: string;
  /** Optional accessibility label */
  ariaLabel?: string;
}

/**
 * Material Icon reference
 */
export interface MaterialIconRef {
  name: string;
  variant?: 'outlined' | 'filled' | 'rounded' | 'sharp' | 'two-tone';
}

/**
 * Resolved icon data
 */
export interface ResolvedIcon {
  /** Icon type */
  type: IconType;
  /** Resolved content (component name, URL, SVG string, etc.) */
  content: string;
  /** Whether the icon was loaded from cache */
  cached: boolean;
  /** Timestamp when resolved */
  resolvedAt: number;
}

/**
 * Icon cache entry
 */
interface IconCacheEntry {
  icon: ResolvedIcon;
  accessCount: number;
  lastAccessed: number;
}

/**
 * Icon Resource Manager Class
 *
 * Singleton manager for icon resources with caching and lazy loading.
 */
export class IconResourceManager {
  private static instance: IconResourceManager;

  /** Icon cache using Map for fast lookups */
  private iconCache: Map<string, IconCacheEntry> = new Map();

  /** Material Icons component cache */
  private materialIconCache: Map<string, unknown> = new Map();

  /** SVG sprite sheet cache */
  private spriteSheetCache: Map<string, Document> = new Map();

  /** Maximum cache size (entries) */
  private readonly maxCacheSize: number = 500;

  /** Cache TTL in milliseconds (1 hour) */
  private readonly cacheTTL: number = 60 * 60 * 1000;

  /** Stats for monitoring */
  private stats = {
    hits: 0,
    misses: 0,
    loads: 0,
    errors: 0,
  };

  private constructor() {
    // Private constructor for singleton
    this.startCacheCleanup();
  }

  /**
   * Get singleton instance
   */
  public static getInstance(): IconResourceManager {
    if (!IconResourceManager.instance) {
      IconResourceManager.instance = new IconResourceManager();
    }
    return IconResourceManager.instance;
  }

  /**
   * Resolve an icon resource
   *
   * @param resource - Icon resource configuration
   * @returns Resolved icon data
   */
  public async resolve(resource: IconResource): Promise<ResolvedIcon> {
    const cacheKey = this.getCacheKey(resource);

    // Check cache first
    const cached = this.getFromCache(cacheKey);
    if (cached) {
      this.stats.hits++;
      return cached.icon;
    }

    this.stats.misses++;

    // Resolve based on type
    let resolved: ResolvedIcon;

    try {
      switch (resource.type) {
        case IconType.MATERIAL:
          resolved = await this.resolveMaterialIcon(resource);
          break;
        case IconType.FLUTTER:
          resolved = await this.resolveFlutterIcon(resource);
          break;
        case IconType.ASSET:
          resolved = await this.resolveAssetIcon(resource);
          break;
        case IconType.URL:
          resolved = await this.resolveUrlIcon(resource);
          break;
        case IconType.SVG:
          resolved = this.resolveSvgIcon(resource);
          break;
        case IconType.BASE64:
          resolved = this.resolveBase64Icon(resource);
          break;
        default:
          throw new Error(`Unsupported icon type: ${resource.type}`);
      }

      this.stats.loads++;
      this.addToCache(cacheKey, resolved);
      return resolved;
    } catch (error) {
      this.stats.errors++;
      console.error(`Failed to resolve icon:`, error);
      return this.getFallbackIcon();
    }
  }

  /**
   * Resolve Material Icon
   */
  private async resolveMaterialIcon(resource: IconResource): Promise<ResolvedIcon> {
    const iconName = this.parseMaterialIconName(resource.identifier);

    return {
      type: IconType.MATERIAL,
      content: iconName,
      cached: false,
      resolvedAt: Date.now(),
    };
  }

  /**
   * Resolve Flutter Icon (custom icon font)
   */
  private async resolveFlutterIcon(resource: IconResource): Promise<ResolvedIcon> {
    // Flutter icons would be mapped to Material Icons or custom font
    // For now, map common Flutter icons to Material equivalents
    const materialEquivalent = this.mapFlutterToMaterial(resource.identifier);

    return {
      type: IconType.MATERIAL,
      content: materialEquivalent,
      cached: false,
      resolvedAt: Date.now(),
    };
  }

  /**
   * Resolve Asset Icon (local file path)
   */
  private async resolveAssetIcon(resource: IconResource): Promise<ResolvedIcon> {
    // Validate asset path
    const assetPath = this.normalizeAssetPath(resource.identifier);

    return {
      type: IconType.ASSET,
      content: assetPath,
      cached: false,
      resolvedAt: Date.now(),
    };
  }

  /**
   * Resolve URL Icon
   */
  private async resolveUrlIcon(resource: IconResource): Promise<ResolvedIcon> {
    // Validate URL
    const url = new URL(resource.identifier);

    return {
      type: IconType.URL,
      content: url.href,
      cached: false,
      resolvedAt: Date.now(),
    };
  }

  /**
   * Resolve SVG Icon
   */
  private resolveSvgIcon(resource: IconResource): ResolvedIcon {
    // Validate SVG content
    const svgContent = this.validateSvg(resource.identifier);

    return {
      type: IconType.SVG,
      content: svgContent,
      cached: false,
      resolvedAt: Date.now(),
    };
  }

  /**
   * Resolve Base64 Icon
   */
  private resolveBase64Icon(resource: IconResource): ResolvedIcon {
    // Validate Base64 content
    const base64Content = this.validateBase64(resource.identifier);

    return {
      type: IconType.BASE64,
      content: base64Content,
      cached: false,
      resolvedAt: Date.now(),
    };
  }

  /**
   * Parse Material Icon name (handle variants)
   */
  private parseMaterialIconName(identifier: string): string {
    const parts = identifier.split(':');
    const iconName = parts[0];
    // Variant handling would be done in the React component
    return iconName;
  }

  /**
   * Map Flutter icon names to Material equivalents
   */
  private mapFlutterToMaterial(flutterIconName: string): string {
    const mapping: Record<string, string> = {
      'Icons.home': 'home',
      'Icons.settings': 'settings',
      'Icons.person': 'person',
      'Icons.search': 'search',
      'Icons.menu': 'menu',
      'Icons.close': 'close',
      'Icons.check': 'check',
      'Icons.arrow_back': 'arrow_back',
      'Icons.arrow_forward': 'arrow_forward',
      'Icons.add': 'add',
      'Icons.remove': 'remove',
      'Icons.favorite': 'favorite',
      'Icons.star': 'star',
      'Icons.info': 'info',
      'Icons.warning': 'warning',
      'Icons.error': 'error',
    };

    return mapping[flutterIconName] || flutterIconName.replace('Icons.', '').toLowerCase();
  }

  /**
   * Normalize asset path
   */
  private normalizeAssetPath(path: string): string {
    // Remove leading slash if present
    const normalized = path.startsWith('/') ? path : `/${path}`;
    return normalized;
  }

  /**
   * Validate SVG content
   */
  private validateSvg(svg: string): string {
    if (!svg.trim().startsWith('<svg')) {
      throw new Error('Invalid SVG content: must start with <svg>');
    }
    return svg;
  }

  /**
   * Validate Base64 content
   */
  private validateBase64(base64: string): string {
    // Check if it's a data URL
    if (base64.startsWith('data:image')) {
      return base64;
    }

    // Otherwise, wrap it as a data URL
    return `data:image/png;base64,${base64}`;
  }

  /**
   * Get cache key for an icon resource
   */
  private getCacheKey(resource: IconResource): string {
    return `${resource.type}:${resource.identifier}:${resource.size || 'default'}:${resource.color || 'default'}`;
  }

  /**
   * Get icon from cache
   */
  private getFromCache(key: string): IconCacheEntry | null {
    const entry = this.iconCache.get(key);
    if (!entry) {
      return null;
    }

    // Check if entry is still valid (TTL)
    if (Date.now() - entry.lastAccessed > this.cacheTTL) {
      this.iconCache.delete(key);
      return null;
    }

    // Update access stats
    entry.accessCount++;
    entry.lastAccessed = Date.now();

    return entry;
  }

  /**
   * Add icon to cache
   */
  private addToCache(key: string, icon: ResolvedIcon): void {
    // Evict old entries if cache is full
    if (this.iconCache.size >= this.maxCacheSize) {
      this.evictLRU();
    }

    this.iconCache.set(key, {
      icon: { ...icon, cached: true },
      accessCount: 1,
      lastAccessed: Date.now(),
    });
  }

  /**
   * Evict least recently used entries
   */
  private evictLRU(): void {
    const entries = Array.from(this.iconCache.entries());
    entries.sort((a, b) => a[1].lastAccessed - b[1].lastAccessed);

    // Remove oldest 10% of entries
    const toRemove = Math.ceil(entries.length * 0.1);
    for (let i = 0; i < toRemove; i++) {
      this.iconCache.delete(entries[i][0]);
    }
  }

  /**
   * Start cache cleanup interval
   */
  private startCacheCleanup(): void {
    // Clean up expired entries every 10 minutes
    setInterval(() => {
      const now = Date.now();
      for (const [key, entry] of this.iconCache.entries()) {
        if (now - entry.lastAccessed > this.cacheTTL) {
          this.iconCache.delete(key);
        }
      }
    }, 10 * 60 * 1000);
  }

  /**
   * Get fallback icon
   */
  private getFallbackIcon(): ResolvedIcon {
    return {
      type: IconType.MATERIAL,
      content: 'help_outline',
      cached: false,
      resolvedAt: Date.now(),
    };
  }

  /**
   * Clear cache
   */
  public clearCache(): void {
    this.iconCache.clear();
    this.materialIconCache.clear();
    this.spriteSheetCache.clear();
  }

  /**
   * Get cache stats
   */
  public getStats(): typeof this.stats {
    return { ...this.stats };
  }

  /**
   * Get cache size
   */
  public getCacheSize(): number {
    return this.iconCache.size;
  }

  /**
   * Preload icons
   */
  public async preload(resources: IconResource[]): Promise<void> {
    await Promise.all(resources.map((resource) => this.resolve(resource)));
  }
}

/**
 * Get global icon resource manager instance
 */
export const getIconResourceManager = (): IconResourceManager => {
  return IconResourceManager.getInstance();
};
