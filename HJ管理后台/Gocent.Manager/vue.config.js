'use strict'
const path = require('path')
const defaultSettings = require('./src/settings.js')

function resolve(dir) {
  return path.join(__dirname, dir)
}

const name = defaultSettings.title || 'vue Admin Template'
const port = process.env.port || process.env.npm_config_port || 1514

module.exports = {
  publicPath: '/PDJHManagement',
  outputDir: 'dist',
  assetsDir: 'static',
  lintOnSave: false,
  productionSourceMap: false,
  transpileDependencies: ['element-plus'],
  devServer: {
    port: port,
    open: true,
    client: {
      overlay: false
    },
    proxy: {
      [process.env.VUE_APP_BASE_API]: {
        target: process.env.VUE_APP_BACKEND_TARGET || 'http://127.0.0.1:8081',
        changeOrigin: true,
        pathRewrite: {
          ['^' + process.env.VUE_APP_BASE_API]: ''
        }
      }
    }
  },
  configureWebpack: {
    name: name,
    resolve: {
      alias: {
        '@': resolve('src')
      },
      fallback: {
        path: require.resolve('path-browserify')
      }
    }
  },
  css: {
    loaderOptions: {
      scss: {
        sassOptions: { quietDeps: true }
      }
    }
  },
  chainWebpack(config) {
    if (config.plugins.has('preload')) {
      config.plugin('preload').tap(() => [
        {
          rel: 'preload',
          fileBlacklist: [/\.map$/, /hot-update\.js$/, /runtime\..*\.js$/],
          include: 'initial'
        }
      ])
    }
    config.plugins.delete('prefetch')

    config.module
      .rule('svg')
      .exclude.add(resolve('src/icons'))
      .end()
    config.module
      .rule('icons')
      .test(/\.svg$/)
      .include.add(resolve('src/icons'))
      .end()
      .use('svg-sprite-loader')
      .loader('svg-sprite-loader')
      .options({
        symbolId: 'icon-[name]'
      })
      .end()

    config.when(process.env.NODE_ENV !== 'development', config => {
      config.optimization.splitChunks({
        chunks: 'all',
        cacheGroups: {
          libs: {
            name: 'chunk-libs',
            test: /[\\/]node_modules[\\/]/,
            priority: 10,
            chunks: 'initial'
          },
          elementPlus: {
            name: 'chunk-elementPlus',
            priority: 20,
            test: /[\\/]node_modules[\\/]_?element-plus(.*)/
          },
          commons: {
            name: 'chunk-commons',
            test: resolve('src/components'),
            minChunks: 3,
            priority: 5,
            reuseExistingChunk: true
          }
        }
      })
      config.optimization.runtimeChunk('single')
    })
  }
}
