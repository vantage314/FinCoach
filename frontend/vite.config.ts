import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
        },
    },
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
            '/admin/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    },
    build: {
        rollupOptions: {
            output: {
                manualChunks(id) {
                    if (!id.includes('node_modules')) {
                        return;
                    }
                    if (/element-plus/.test(id)) {
                        return 'element-plus';
                    }
                    if (/echarts|vue-echarts/.test(id)) {
                        return 'echarts';
                    }
                    if (/axios/.test(id)) {
                        return 'axios';
                    }
                    if (/[\\/]vue[\\/]/.test(id) || /@vue/.test(id) || /vue-router/.test(id) || /pinia/.test(id)) {
                        return 'vue-core';
                    }
                },
            },
        },
    },
})
