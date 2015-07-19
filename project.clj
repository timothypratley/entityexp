(defproject entityexp "0.1.0-SNAPSHOT"
  :description "Entity explorer for Datomic"
  :url "http://github.com/timothypratley/entityexp"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]
                 [routegen "0.1.4"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [compojure "1.4.0"]
                 [com.datomic/datomic-free "0.9.5198" :exclusions [joda-time]]
                 [reagent "0.5.0"]
                 [datascript "0.11.5"]
                 [cljs-http "0.1.35"]]
  :plugins [[lein-ring "0.9.6"]
            [lein-cljsbuild "1.0.5"]
            [lein-figwheel "0.3.7"]]
  :ring {:handler entityexp.core/handler}
  :cljsbuild {:builds {:app {:source-paths ["src-cljs"]
                             :figwheel {:on-jsload "entityexp.main/mount-root" }
                             :compiler {:main entityexp.main
                                        :output-to "resources/public/js/main.js"
                                        :output-dir "resources/public/js/out"
                                        :asset-path "js/out"}}}}
  :profiles {:dev {:plugins [[lein-figwheel "0.3.3"]]
                   :figwheel {:http-server-root "public"
                              :server-port 3449
                              :css-dirs ["resources/public/css"]
                              :ring-handler entityexp.core/handler}}})
