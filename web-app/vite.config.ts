import { reactRouter } from "@react-router/dev/vite";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from "vite";
import tsconfigPaths from "vite-tsconfig-paths";

const backendProxyTarget =
  process.env.VITE_BACKEND_PROXY_TARGET ?? "http://localhost:8080";

export default defineConfig({
  plugins: [tailwindcss(), reactRouter(), tsconfigPaths()],
  server: {
    proxy: {
      // Keep browser traffic same-origin in dev.
      "/api": {
        target: backendProxyTarget,
        changeOrigin: true,
      },
      "/actuator": {
        target: backendProxyTarget,
        changeOrigin: true,
      },
    },
  },
});
