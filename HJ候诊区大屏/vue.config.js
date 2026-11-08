var webpack = require('webpack');

// 与 pdjh-server 一致（默认 8081）；本机改端口时建 .env.development.local 覆盖 VUE_APP_PROXY_TARGET
var backendTarget = process.env.VUE_APP_PROXY_TARGET || 'http://127.0.0.1:8081'

module.exports = {
    // 在vue.config.js中configureWebpack中配置
    // 要引入webpack
    publicPath: '/',
    outputDir: 'dist',
    assetsDir: 'static',
    //lintOnSave: process.env.NODE_ENV === 'development',
    //productionSourceMap: false,
    //lintOnSave: false,   //关闭语法检查 比如少了个空格什么的
    configureWebpack: {
        plugins: [
            new webpack.ProvidePlugin({
                'window.Quill': 'quill/dist/quill.js',
                'Quill': 'quill/dist/quill.js'
            }),
            new webpack.ProvidePlugin({
                $: 'jquery',
                jQuery: 'jquery',
                'window.jQuery': 'jquery',
                'root.jQuery': 'jquery'
            })
        ]
    },
    pages: {
        index: {
            //入口文件
            entry: "./src/main.js",
            //模板文件
            template: "public/index.html",
            filename: "index.html",
            title: "首页",
            chunks: ["chunk-vendors", 'chunk-common', 'index']

        }

    },
    transpileDependencies: [/packages[\\/]common/],
    lintOnSave: false,
    // //开启代理服务器
    // devServer: {
    //     proxy: 'http://localhost:5000'//访问的端口号
    // },
    devServer: {
        proxy: {
            '/PDJH': {
                target: backendTarget,
                pathRewrite: { "^/PDJH": "" },
                ws: true,
                changeOrigin: true
            },
        }
    },

}