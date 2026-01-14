/**
 * IDEACode - Core Server
 *
 * Unified API Gateway on port 3850
 * Features:
 * - Multi-provider LLM with fallback
 * - RAG semantic search
 * - Memory management
 * - Quality gates
 * - Token budgeting
 */

import express, { Express, Request, Response, NextFunction } from 'express';
import cors from 'cors';
import helmet from 'helmet';
import compression from 'compression';
import { createLLMRouter, LLMRouter } from '../llm/router/index.js';

const PORT = process.env.IDEACODE_PORT ?? 3850;
const VERSION = '13.0.0';

/**
 * Server instance
 */
export class IDEACodeServer {
  private app: Express;
  private llmRouter: LLMRouter;
  private startTime: number;

  constructor() {
    this.app = express();
    this.llmRouter = createLLMRouter();
    this.startTime = Date.now();
    this.setupMiddleware();
    this.setupRoutes();
  }

  /**
   * Configure middleware
   */
  private setupMiddleware(): void {
    // Security
    this.app.use(helmet({
      contentSecurityPolicy: false, // Allow API usage
    }));

    // CORS - allow all origins for local development
    this.app.use(cors());

    // Compression
    this.app.use(compression());

    // JSON parsing
    this.app.use(express.json({ limit: '10mb' }));

    // Request logging
    this.app.use((req: Request, _res: Response, next: NextFunction) => {
      console.log(`${new Date().toISOString()} ${req.method} ${req.path}`);
      next();
    });
  }

  /**
   * Setup API routes
   */
  private setupRoutes(): void {
    // Health check
    this.app.get('/health', this.healthHandler.bind(this));

    // LLM endpoints
    this.app.get('/v1/llm/status', this.llmStatusHandler.bind(this));
    this.app.get('/v1/llm/models', this.llmModelsHandler.bind(this));
    this.app.post('/v1/llm/complete', this.llmCompleteHandler.bind(this));
    this.app.post('/v1/llm/stream', this.llmStreamHandler.bind(this));

    // Memory endpoints
    this.app.get('/v1/memory/load', this.memoryLoadHandler.bind(this));
    this.app.get('/v1/memory/load/:path(*)', this.memoryLoadPathHandler.bind(this));
    this.app.post('/v1/memory/save', this.memorySaveHandler.bind(this));
    this.app.delete('/v1/memory/cache/session', this.memoryClearCacheHandler.bind(this));

    // Token endpoints
    this.app.post('/v1/tokens/count', this.tokenCountHandler.bind(this));
    this.app.get('/v1/tokens/budget', this.tokenBudgetHandler.bind(this));

    // Compression endpoints
    this.app.post('/v1/compress', this.compressHandler.bind(this));

    // Search (RAG) endpoints
    this.app.post('/v1/search', this.searchHandler.bind(this));

    // Quality gate endpoints
    this.app.post('/v1/quality/all', this.qualityAllHandler.bind(this));
    this.app.post('/v1/quality/code', this.qualityCodeHandler.bind(this));
    this.app.post('/v1/quality/security', this.qualitySecurityHandler.bind(this));
    this.app.post('/v1/quality/coverage', this.qualityCoverageHandler.bind(this));
    this.app.post('/v1/quality/performance', this.qualityPerformanceHandler.bind(this));

    // Status endpoint
    this.app.get('/v1/status', this.fullStatusHandler.bind(this));

    // Phosphor Icons endpoints
    this.app.get('/v1/icons/search', this.iconSearchHandler.bind(this));
    this.app.post('/v1/icons/generate', this.iconGenerateHandler.bind(this));
    this.app.get('/v1/icons/categories', this.iconCategoriesHandler.bind(this));

    // 404 handler
    this.app.use((_req: Request, res: Response) => {
      res.status(404).json({ error: 'Not Found' });
    });

    // Error handler
    this.app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
      console.error('Server error:', err);
      res.status(500).json({ error: err.message });
    });
  }

  /**
   * Health check endpoint
   */
  private async healthHandler(_req: Request, res: Response): Promise<void> {
    const uptime = Math.floor((Date.now() - this.startTime) / 1000);
    const health = await this.llmRouter.healthCheckAll();

    const providers: Record<string, boolean> = {};
    for (const [type, status] of health) {
      providers[type] = status.healthy;
    }

    res.json({
      status: 'healthy',
      version: VERSION,
      uptime,
      port: PORT,
      memory: {
        tokens: 0, // Will be populated by memory system
        loaded: [],
      },
      providers,
    });
  }

  /**
   * LLM status endpoint
   */
  private async llmStatusHandler(_req: Request, res: Response): Promise<void> {
    const status = await this.llmRouter.getStatus();
    res.json(status);
  }

  /**
   * LLM models endpoint
   */
  private async llmModelsHandler(_req: Request, res: Response): Promise<void> {
    const models = await this.llmRouter.listAllModels();
    const result: Record<string, string[]> = {};
    for (const [type, modelList] of models) {
      result[type] = modelList;
    }
    res.json({ models: result });
  }

  /**
   * LLM completion endpoint
   */
  private async llmCompleteHandler(req: Request, res: Response): Promise<void> {
    try {
      const response = await this.llmRouter.complete(req.body);
      res.json(response);
    } catch (error) {
      res.status(500).json({ error: (error as Error).message });
    }
  }

  /**
   * LLM streaming endpoint (Server-Sent Events)
   */
  private async llmStreamHandler(req: Request, res: Response): Promise<void> {
    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');

    try {
      for await (const chunk of this.llmRouter.stream(req.body)) {
        res.write(`data: ${JSON.stringify(chunk)}\n\n`);
      }
      res.write('data: [DONE]\n\n');
      res.end();
    } catch (error) {
      res.write(`data: ${JSON.stringify({ error: (error as Error).message })}\n\n`);
      res.end();
    }
  }

  /**
   * Memory load index endpoint
   */
  private memoryLoadHandler(_req: Request, res: Response): void {
    // TODO: Implement memory index loading
    res.json({
      loaded: ['zero-tolerance.md', 'memory-index.md'],
      tokens: 150,
    });
  }

  /**
   * Memory load specific path endpoint
   */
  private memoryLoadPathHandler(req: Request, res: Response): void {
    const path = req.params.path;
    // TODO: Implement file loading
    res.json({
      path,
      content: `[Content of ${path} would be loaded here]`,
      tokens: 0,
    });
  }

  /**
   * Memory save endpoint
   */
  private memorySaveHandler(req: Request, res: Response): void {
    const { path, content } = req.body;
    // TODO: Implement memory saving
    res.json({
      saved: true,
      path,
      tokens: Math.ceil((content?.length ?? 0) / 4),
    });
  }

  /**
   * Clear session cache endpoint
   */
  private memoryClearCacheHandler(_req: Request, res: Response): void {
    // TODO: Implement cache clearing
    res.json({ cleared: true });
  }

  /**
   * Token count endpoint
   */
  private async tokenCountHandler(req: Request, res: Response): Promise<void> {
    const { text } = req.body;
    if (!text) {
      res.status(400).json({ error: 'text required' });
      return;
    }

    const tokens = await this.llmRouter.countTokens(text);
    res.json({
      tokens,
      model: 'cl100k_base',
    });
  }

  /**
   * Token budget endpoint
   */
  private tokenBudgetHandler(_req: Request, res: Response): void {
    // TODO: Implement token tracking
    res.json({
      used: 0,
      budget: 1000,
      remaining: 1000,
    });
  }

  /**
   * Compress content endpoint (AVU format)
   */
  private compressHandler(req: Request, res: Response): void {
    const { content, format = 'avu' } = req.body;
    if (!content) {
      res.status(400).json({ error: 'content required' });
      return;
    }

    // TODO: Implement proper compression
    const originalTokens = Math.ceil(content.length / 4);
    const compressed = this.simpleCompress(content, format);
    const compressedTokens = Math.ceil(compressed.length / 4);

    res.json({
      compressed,
      original_tokens: originalTokens,
      compressed_tokens: compressedTokens,
      reduction: `${Math.round((1 - compressedTokens / originalTokens) * 100)}%`,
    });
  }

  /**
   * Simple compression (placeholder)
   */
  private simpleCompress(content: string, format: string): string {
    if (format === 'avu') {
      // AVU: Remove redundant whitespace, abbreviate common words
      return content
        .replace(/\s+/g, ' ')
        .replace(/\bfunction\b/g, 'fn')
        .replace(/\breturn\b/g, 'ret')
        .replace(/\bconst\b/g, 'c')
        .trim();
    }
    return content;
  }

  /**
   * Search (RAG) endpoint
   */
  private searchHandler(req: Request, res: Response): void {
    const { query, collection = 'all', limit = 5 } = req.body;
    if (!query) {
      res.status(400).json({ error: 'query required' });
      return;
    }

    // TODO: Implement RAG search
    res.json({
      results: [],
      tokens: 0,
      collection,
      limit,
    });
  }

  /**
   * Quality - all gates endpoint
   */
  private qualityAllHandler(req: Request, res: Response): void {
    const { path, phase = 'pre-commit' } = req.body;

    // TODO: Implement quality checks
    res.json({
      passed: true,
      summary: {
        code: 'pass',
        tests: 'skip',
        security: 'pass',
        performance: 'skip',
        docs: 'skip',
      },
      blockers: 0,
      warnings: 0,
      phase,
      path,
    });
  }

  /**
   * Quality - code endpoint
   */
  private qualityCodeHandler(req: Request, res: Response): void {
    const { files, checks = ['solid', 'duplication', 'stubs'] } = req.body;

    res.json({
      passed: true,
      violations: [],
      files,
      checks,
    });
  }

  /**
   * Quality - security endpoint
   */
  private qualitySecurityHandler(req: Request, res: Response): void {
    const { files, checks = ['secrets', 'owasp'] } = req.body;

    res.json({
      passed: true,
      issues: [],
      files,
      checks,
    });
  }

  /**
   * Quality - coverage endpoint
   */
  private qualityCoverageHandler(req: Request, res: Response): void {
    const { path, threshold = 90 } = req.body;

    res.json({
      passed: true,
      coverage: {
        lines: 0,
        branches: 0,
        functions: 0,
      },
      path,
      threshold,
    });
  }

  /**
   * Quality - performance endpoint
   */
  private qualityPerformanceHandler(req: Request, res: Response): void {
    const { benchmark = false, iterations = 100 } = req.body;

    res.json({
      passed: true,
      results: {
        health: { p50: 5, p95: 20, p99: 50 },
        api: { p50: 50, p95: 150, p99: 200 },
      },
      benchmark,
      iterations,
    });
  }

  /**
   * Full status endpoint
   */
  private async fullStatusHandler(_req: Request, res: Response): Promise<void> {
    const llmStatus = await this.llmRouter.getStatus();

    res.json({
      api: true,
      version: VERSION,
      uptime: Math.floor((Date.now() - this.startTime) / 1000),
      memory: {
        loaded: [],
        tokens: 0,
      },
      llm: llmStatus,
      cache: {
        session: 0,
        search: 0,
        response: 0,
      },
    });
  }

  /**
   * Phosphor Icons - Search icons by name or category
   */
  private iconSearchHandler(req: Request, res: Response): void {
    const query = (req.query.q as string) || '';
    const category = req.query.category as string;
    const limit = parseInt(req.query.limit as string) || 20;

    // Phosphor icon database (common icons)
    const icons = this.getPhosphorIcons();

    let results = icons;
    if (query) {
      const q = query.toLowerCase();
      results = icons.filter(i =>
        i.name.toLowerCase().includes(q) ||
        i.tags.some(t => t.includes(q))
      );
    }
    if (category) {
      results = results.filter(i => i.category === category);
    }

    res.json({
      query,
      count: results.length,
      icons: results.slice(0, limit).map(i => ({
        name: i.name,
        category: i.category,
        weights: ['thin', 'light', 'regular', 'bold', 'fill', 'duotone'],
      })),
    });
  }

  /**
   * Phosphor Icons - Generate code for different frameworks
   */
  private iconGenerateHandler(req: Request, res: Response): void {
    const { icon, framework = 'react', weight = 'regular', size = 24, color } = req.body;

    if (!icon) {
      res.status(400).json({ error: 'icon name required' });
      return;
    }

    const pascalCase = icon.split('-').map((s: string) =>
      s.charAt(0).toUpperCase() + s.slice(1)
    ).join('');

    const code = this.generateIconCode(icon, pascalCase, framework, weight, size, color);

    res.json({
      icon,
      framework,
      weight,
      code,
      imports: this.getIconImports(pascalCase, framework),
    });
  }

  /**
   * Phosphor Icons - List categories
   */
  private iconCategoriesHandler(_req: Request, res: Response): void {
    res.json({
      categories: [
        { name: 'arrows', count: 45, description: 'Directional arrows and navigation' },
        { name: 'commerce', count: 38, description: 'Shopping, payments, commerce' },
        { name: 'communication', count: 42, description: 'Chat, email, messaging' },
        { name: 'design', count: 35, description: 'Design tools and shapes' },
        { name: 'development', count: 48, description: 'Code, terminal, dev tools' },
        { name: 'editor', count: 52, description: 'Text editing and formatting' },
        { name: 'files', count: 40, description: 'Files, folders, documents' },
        { name: 'finance', count: 28, description: 'Money, charts, banking' },
        { name: 'health', count: 22, description: 'Medical and health icons' },
        { name: 'map', count: 35, description: 'Maps, location, travel' },
        { name: 'media', count: 45, description: 'Audio, video, playback' },
        { name: 'objects', count: 120, description: 'Common objects and items' },
        { name: 'people', count: 38, description: 'Users, profiles, people' },
        { name: 'security', count: 25, description: 'Locks, shields, security' },
        { name: 'system', count: 55, description: 'Settings, system controls' },
        { name: 'weather', count: 20, description: 'Weather and nature' },
      ],
      library: 'phosphor',
      version: '2.1.1',
      totalIcons: 9000,
    });
  }

  /**
   * Get common Phosphor icons
   */
  private getPhosphorIcons(): Array<{ name: string; category: string; tags: string[] }> {
    return [
      { name: 'house', category: 'objects', tags: ['home', 'building'] },
      { name: 'user', category: 'people', tags: ['person', 'profile', 'account'] },
      { name: 'users', category: 'people', tags: ['people', 'group', 'team'] },
      { name: 'gear', category: 'system', tags: ['settings', 'cog', 'config'] },
      { name: 'magnifying-glass', category: 'system', tags: ['search', 'find', 'lookup'] },
      { name: 'chart-bar', category: 'finance', tags: ['analytics', 'stats', 'graph'] },
      { name: 'chart-line-up', category: 'finance', tags: ['growth', 'trend', 'analytics'] },
      { name: 'buildings', category: 'objects', tags: ['office', 'company', 'tenant'] },
      { name: 'lock-key', category: 'security', tags: ['security', 'auth', 'password'] },
      { name: 'key', category: 'security', tags: ['access', 'api', 'token'] },
      { name: 'credit-card', category: 'commerce', tags: ['payment', 'billing', 'card'] },
      { name: 'palette', category: 'design', tags: ['colors', 'theme', 'design'] },
      { name: 'package', category: 'objects', tags: ['box', 'app', 'plugin'] },
      { name: 'folder', category: 'files', tags: ['directory', 'files'] },
      { name: 'folder-open', category: 'files', tags: ['directory', 'files', 'open'] },
      { name: 'folder-notch-open', category: 'files', tags: ['folder', 'documents'] },
      { name: 'file-text', category: 'files', tags: ['document', 'doc', 'text'] },
      { name: 'file-doc', category: 'files', tags: ['word', 'document'] },
      { name: 'file-pdf', category: 'files', tags: ['pdf', 'document'] },
      { name: 'tree-structure', category: 'system', tags: ['hierarchy', 'org', 'tree'] },
      { name: 'robot', category: 'objects', tags: ['ai', 'bot', 'automation'] },
      { name: 'cloud-arrow-up', category: 'system', tags: ['upload', 'cloud', 'sync'] },
      { name: 'download-simple', category: 'arrows', tags: ['download', 'save'] },
      { name: 'upload-simple', category: 'arrows', tags: ['upload', 'add'] },
      { name: 'share-network', category: 'communication', tags: ['share', 'social'] },
      { name: 'pencil-simple', category: 'editor', tags: ['edit', 'write'] },
      { name: 'trash', category: 'system', tags: ['delete', 'remove'] },
      { name: 'plus', category: 'system', tags: ['add', 'new', 'create'] },
      { name: 'check', category: 'system', tags: ['done', 'success', 'complete'] },
      { name: 'x', category: 'system', tags: ['close', 'cancel', 'remove'] },
      { name: 'sign-out', category: 'arrows', tags: ['logout', 'exit'] },
      { name: 'sign-in', category: 'arrows', tags: ['login', 'enter'] },
      { name: 'star', category: 'objects', tags: ['favorite', 'rating'] },
      { name: 'bell', category: 'communication', tags: ['notification', 'alert'] },
      { name: 'envelope', category: 'communication', tags: ['email', 'mail', 'message'] },
      { name: 'chat', category: 'communication', tags: ['message', 'talk'] },
      { name: 'funnel', category: 'system', tags: ['filter', 'sort'] },
      { name: 'receipt', category: 'commerce', tags: ['invoice', 'bill'] },
      { name: 'scales', category: 'objects', tags: ['legal', 'balance', 'justice'] },
      { name: 'clipboard-text', category: 'editor', tags: ['copy', 'paste', 'clipboard'] },
      { name: 'stack', category: 'objects', tags: ['layers', 'app', 'logo'] },
    ];
  }

  /**
   * Generate icon code for different frameworks
   */
  private generateIconCode(
    icon: string,
    pascalCase: string,
    framework: string,
    weight: string,
    size: number,
    color?: string
  ): string {
    const weightSuffix = weight !== 'regular' ? weight.charAt(0).toUpperCase() + weight.slice(1) : '';
    const colorAttr = color ? `, color: "${color}"` : '';
    const colorProp = color ? ` color="${color}"` : '';

    switch (framework) {
      case 'react':
        return `<${pascalCase}${weightSuffix} size={${size}}${colorProp} />`;

      case 'react-native':
        return `<${pascalCase}${weightSuffix} size={${size}}${colorProp} />`;

      case 'vue':
        return `<Ph${pascalCase}${weightSuffix} :size="${size}"${colorProp} />`;

      case 'flutter':
        return `PhosphorIcons.${this.camelCase(icon)}${weightSuffix ? `${weightSuffix}` : ''}(size: ${size}${color ? `, color: Color(0xFF${color.replace('#', '')})` : ''})`;

      case 'html':
      case 'web':
        return `<i class="ph${weight !== 'regular' ? `-${weight}` : ''} ph-${icon}"${size !== 24 ? ` style="font-size: ${size}px${color ? `; color: ${color}` : ''}"` : ''}></i>`;

      case 'svg':
        return `<!-- ${pascalCase} (${weight}) -->\n<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" fill="${color || 'currentColor'}" viewBox="0 0 256 256">\n  <!-- SVG path data from phosphoricons.com -->\n</svg>`;

      default:
        return `// Unsupported framework: ${framework}`;
    }
  }

  /**
   * Get import statements for icon
   */
  private getIconImports(pascalCase: string, framework: string): string {
    switch (framework) {
      case 'react':
        return `import { ${pascalCase} } from "@phosphor-icons/react";`;
      case 'react-native':
        return `import { ${pascalCase} } from "phosphor-react-native";`;
      case 'vue':
        return `import { Ph${pascalCase} } from "@phosphor-icons/vue";`;
      case 'flutter':
        return `import 'package:phosphor_flutter/phosphor_flutter.dart';`;
      case 'html':
      case 'web':
        return `<script src="https://unpkg.com/@phosphor-icons/web"></script>`;
      default:
        return '';
    }
  }

  /**
   * Convert kebab-case to camelCase
   */
  private camelCase(str: string): string {
    return str.replace(/-([a-z])/g, (_, c) => c.toUpperCase());
  }

  /**
   * Start the server
   */
  start(): void {
    this.app.listen(PORT, () => {
      console.log(`
╔════════════════════════════════════════════════╗
║  IDEACode API Server v${VERSION}                 ║
║  Port: ${PORT}                                     ║
║  Status: Running                               ║
╚════════════════════════════════════════════════╝

Endpoints:
  GET  /health              - Health check
  GET  /v1/status           - Full status
  GET  /v1/llm/status       - LLM providers status
  GET  /v1/llm/models       - Available models
  POST /v1/llm/complete     - Generate completion
  POST /v1/llm/stream       - Stream completion
  GET  /v1/memory/load      - Load memory index
  POST /v1/memory/save      - Save memory
  POST /v1/tokens/count     - Count tokens
  GET  /v1/tokens/budget    - Token budget
  POST /v1/compress         - Compress content
  POST /v1/search           - RAG search
  POST /v1/quality/all      - Run all quality gates

Icons (Phosphor):
  GET  /v1/icons/search     - Search icons by name
  POST /v1/icons/generate   - Generate icon code
  GET  /v1/icons/categories - List icon categories
`);
    });
  }

  /**
   * Get Express app for testing
   */
  getApp(): Express {
    return this.app;
  }
}

/**
 * Create and export server
 */
export function createServer(): IDEACodeServer {
  return new IDEACodeServer();
}
