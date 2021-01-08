(defproject ez-wire "0.3.0-SNAPSHOT"

  :description "Wiring galore"

  :url "https://github.com/emil0r/ez-wire"

  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/clojurescript "1.10.597" :scope "provided"]
                 [re-frame "0.10.5" :scope "provided"]
                 [reagent "1.0.0" :scope "provided"]]

  :repl-options {:init-ns ez-wire.core}

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.18"]
            [cider/cider-nrepl "0.21.1"]
            [lein-doo "0.1.10"]]
  

  :clojurescript? true
  :jar-exclusions [#"\.swp|\.swo|\.DS_Store"]
  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]]

  :profiles {:dev
             {:dependencies
              [[ring-server "0.5.0"]
               [ring-webjars "0.2.0"]
               [ring "1.7.1"]
               [ring/ring-defaults "0.3.2"]
               [compojure "1.6.1"]
               [hiccup "1.0.5"]
               [binaryage/devtools "0.9.10"]
               [cider/piggieback "0.4.1"]
               [figwheel-sidecar "0.5.18"]
               ;; react components
               [cljsjs/semantic-ui-react "0.88.1-0"]
               [cljsjs/antd "4.6.1-0"]
               ;; i18n
               [tongue "0.2.9"]]

              :source-paths ["src" "dev/clj" "dev/cljs"]
              :resource-paths ["resources" "dev/resources" "target/cljsbuild"]

              :figwheel
              {:server-port      3450
               :nrepl-port       7000
               :nrepl-middleware [cider.piggieback/wrap-cljs-repl
                                  cider.nrepl/cider-middleware]
               :css-dirs         ["dev/resources/public/css"]
               :ring-handler     ez-wire.server/app}
              :cljsbuild
              {:builds
               {:app
                {:source-paths ["src" "dev/cljs"]
                 :figwheel     {:on-jsload "ez-wire.test-page/mount-root"}
                 :compiler     {:main          ez-wire.dev
                                :asset-path    "/js/out"
                                :output-to     "target/cljsbuild/public/js/app.js"
                                :output-dir    "target/cljsbuild/public/js/out"
                                :source-map-timestamp true
                                :source-map    true
                                :optimizations :none
                                :pretty-print  true}}
                :demo
                {:source-paths ["src" "demo/src"]
                 :figwheel     {:on-jsload "demo.core/start"}
                 :compiler     {:main          demo.core
                                :asset-path    "js/out"
                                :output-to     "demo/resources/js/app.js"
                                :output-dir    "demo/resources/js/out"
                                :source-map-timestamp true
                                :source-map    true
                                :optimizations :none
                                :pretty-print  true}}
                :demo-prod
                {:source-paths ["src" "demo/src"]
                 :compiler     {:main          demo.core
                                :asset-path    "/js/out"
                                :output-to     "demo/resources/js/prod/app.js"
                                :output-dir    "demo/resources/js/prod/out"
                                :source-map-timestamp true
                                :source-map    "demo/resources/js/prod/app.js.map"
                                :optimizations :advanced}}}}}

             :test
             {:cljsbuild
              {:builds
               {:test
                {:source-paths ["src" "test"]
                 :compiler     {:main          ez-wire.runner
                                :output-to     "target/test/core.js"
                                :target        :nodejs
                                :optimizations :none
                                :source-map    true
                                :pretty-print  true}}}}
              :doo {:build "test"}}}
  
  :aliases {"test"
            ["do"
             ["clean"]
             ["with-profile" "test" "doo" "node" "once"]]
            "test-watch"
            ["do"
             ["clean"]
             ["with-profile" "test" "doo" "node"]]})
