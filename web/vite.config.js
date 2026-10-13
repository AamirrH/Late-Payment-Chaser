import {defineConfig,loadEnv} from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
export default defineConfig(({mode})=>({plugins:[react(),tailwindcss()],server:{proxy:{'/api':{target:loadEnv(mode,process.cwd(),'').FINANCE_PROXY||'http://127.0.0.1:8080',rewrite:path=>path.replace(/^\/api/,'')}}}}));
