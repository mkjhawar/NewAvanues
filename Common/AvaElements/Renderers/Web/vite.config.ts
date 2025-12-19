import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react({
      jsxRuntime: 'automatic',
      babel: {
        plugins: [
          ['@emotion/babel-plugin', { sourceMap: true }]
        ]
      }
    })
  ],

  build: {
    lib: {
      entry: path.resolve(__dirname, 'src/index.ts'),
      name: 'AvaElementsWeb',
      formats: ['es', 'cjs'],
      fileName: (format) => format === 'es' ? 'index.js' : 'index.cjs'
    },
    rollupOptions: {
      // Externalize peer dependencies
      external: [
        'react',
        'react-dom',
        'react/jsx-runtime',
        '@mui/material',
        '@emotion/react',
        '@emotion/styled'
      ],
      output: {
        // Provide global variables for UMD build
        globals: {
          react: 'React',
          'react-dom': 'ReactDOM',
          '@mui/material': 'MaterialUI',
          '@emotion/react': 'emotionReact',
          '@emotion/styled': 'emotionStyled'
        },
        // Preserve module structure for tree-shaking
        preserveModules: false,
        // Use named exports
        exports: 'named',
        // Source maps for debugging
        sourcemap: true,
        // Manual chunks for better code splitting
        manualChunks: {
          'layout': [
            './src/components/layout/AppBar.tsx',
            './src/components/layout/BottomAppBar.tsx',
            './src/components/layout/Drawer.tsx',
            './src/components/layout/Scaffold.tsx',
            './src/components/layout/TabBar.tsx'
          ],
          'input': [
            './src/components/input/Checkbox.tsx',
            './src/components/input/Radio.tsx',
            './src/components/input/Switch.tsx',
            './src/components/input/Slider.tsx',
            './src/components/input/TextField.tsx'
          ],
          'buttons': [
            './src/components/buttons/ElevatedButton.tsx',
            './src/components/buttons/FilledButton.tsx',
            './src/components/buttons/OutlinedButton.tsx',
            './src/components/buttons/TextButton.tsx',
            './src/components/buttons/IconButton.tsx',
            './src/components/buttons/FloatingActionButton.tsx'
          ],
          'display': [
            './src/components/display/Card.tsx',
            './src/components/display/Chip.tsx',
            './src/components/display/Badge.tsx',
            './src/components/display/Avatar.tsx',
            './src/components/display/Divider.tsx'
          ]
        }
      }
    },
    // Target modern browsers
    target: 'es2020',
    // Minification
    minify: 'esbuild',
    // Source maps
    sourcemap: true,
    // Optimize dependencies
    commonjsOptions: {
      include: [/node_modules/]
    },
    // Bundle size reporting
    reportCompressedSize: true,
    chunkSizeWarningLimit: 500
  },

  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@components': path.resolve(__dirname, './src/components'),
      '@utils': path.resolve(__dirname, './src/utils'),
      '@types': path.resolve(__dirname, './src/types'),
      '@hooks': path.resolve(__dirname, './src/hooks'),
      '@theme': path.resolve(__dirname, './src/theme')
    }
  },

  // Optimize dependency pre-bundling
  optimizeDeps: {
    include: [
      'react',
      'react-dom',
      '@mui/material',
      '@mui/icons-material',
      '@emotion/react',
      '@emotion/styled'
    ]
  },

  // Development server configuration
  server: {
    port: 3000,
    strictPort: false,
    open: true,
    cors: true
  },

  // Preview server configuration
  preview: {
    port: 3001,
    strictPort: false,
    open: true
  },

  // Testing configuration
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/setupTests.ts',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html', 'lcov'],
      exclude: [
        'node_modules/',
        'src/setupTests.ts',
        '**/*.stories.tsx',
        '**/*.test.tsx',
        '**/*.spec.tsx',
        'dist/'
      ],
      statements: 90,
      branches: 90,
      functions: 90,
      lines: 90
    }
  },

  // Define global constants
  define: {
    __APP_VERSION__: JSON.stringify(process.env.npm_package_version),
    __BUILD_DATE__: JSON.stringify(new Date().toISOString())
  },

  // CSS configuration
  css: {
    modules: {
      localsConvention: 'camelCase'
    },
    preprocessorOptions: {
      scss: {
        additionalData: '@import "@/theme/variables.scss";'
      }
    }
  }
});
