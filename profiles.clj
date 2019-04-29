{:user {#_ #_ :repositories [["vemv" #=(eval (System/getenv "MYMAVENREPO_READ_URL"))]]
        :plugins [[lein-eftest "0.5.4"]
                  [lein-pprint "1.1.2" :exclusions [org.clojure/clojure]]
                  [lein-nvd "0.6.0"]
                  [lein-lein "0.2.0"]]
        :jvm-opts ["-Dapple.awt.UIElement=true"
                   "-XX:-OmitStackTraceInFastThrow"
                   "-Xmx18G"]
        :eftest {:multithread? false
                 :fail-fast? true}}
 :auth {:deploy-repositories [["releases" {:url "https://clojars.org/repo" :sign-releases false}]
                              ["snapshots" :clojars]]
        :repository-auth {#"https://clojars\.org/repo" {:username #=(eval (System/getenv "CLOJARS_USERNAME"))
                                                        :password #=(eval (System/getenv "CLOJARS_PASSWORD"))}}}
 ;; the following profile serves for two use cases:
 ;; * Launching `lein repl` from iTerm
 ;; * Launching an in-Emacs JVM
 ;; Perhaps for the latter, the :plugins section is redundant. Hasn't given problems so far.
 :emacs-backend {:dependencies [[cider/cider-nrepl "0.16.0"]
                                [criterium "0.4.4"]
                                [formatting-stack "0.17.0"]
                                [lambdaisland/deep-diff "0.0-29"]
                                [org.clojure/tools.reader "1.1.1"]
                                [com.clojure-goes-fast/clj-java-decompiler "0.2.1"]
                                [org.clojure/tools.nrepl "0.2.13" :exclusions [org.clojure/clojure]]
                                [org.clojure/tools.namespace "0.3.0-alpha4"]
                                [nrepl-debugger "0.1.0-SNAPSHOT"]
                                [com.stuartsierra/component.repl "0.2.0"]]
                 :plugins [[refactor-nrepl "2.4.0" :exclusions [org.clojure/tools.logging]]
                           [cider/cider-nrepl "0.16.0"]]
                 :repl-options {:port 41235
                                :timeout 120000}}
 :emacs-backend-init {:repl-options {:init (do
                                             (clojure.core/require 'refactor-nrepl.core)
                                             (clojure.core/require 'refactor-nrepl.middleware)
                                             (clojure.core/require 'refactor-nrepl.analyzer)
                                             (clojure.core/require 'net.vemv.nrepl-debugger)
                                             (clojure.core/require 'clj-java-decompiler.core)
                                             (clojure.core/require 'lambdaisland.deep-diff)
                                             (clojure.core/require 'criterium.core)
                                             (clojure.core/require 'formatting-stack.core)
                                             (clojure.core/require 'formatting-stack.branch-formatter)
                                             (clojure.core/require 'formatting-stack.project-formatter)
                                             (clojure.core/require 'clojure.tools.namespace.repl)
                                             (clojure.core/require 'com.stuartsierra.component.repl)
                                             (clojure.tools.namespace.repl/set-refresh-dirs "src" "test")
                                             (clojure.tools.namespace.repl/refresh))}}
 :nedap-key {:source-paths ["specs/server"]
             :jvm-opts ["-Dlogback.configurationFile=resources/logback-no-stdout.xml"]
             :repl-options ^:replace {:port 41234
                                      :timeout 120000
                                      :init-ns user
                                      :init (do
                                              (clojure.core/require 'refactor-nrepl.core)
                                              (clojure.core/require 'refactor-nrepl.middleware)
                                              (clojure.core/require 'refactor-nrepl.analyzer)
                                              (clojure.core/require 'net.vemv.nrepl-debugger)
                                              (clojure.core/require 'clj-java-decompiler.core)
                                              (clojure.core/require 'lambdaisland.deep-diff)
                                              (clojure.core/require 'criterium.core)
                                              (clojure.core/require 'formatting-stack.core)
                                              (clojure.core/require 'formatting-stack.branch-formatter)
                                              (clojure.core/require 'formatting-stack.project-formatter)
                                              (clojure.core/require 'clojure.tools.namespace.repl)
                                              (clojure.core/require 'com.stuartsierra.component.repl)
                                              (clojure.core/require 'clojure.test)
                                              (clojure.core/alter-var-root #'clojure.test/*load-tests* (clojure.core/constantly false))
                                              (clojure.tools.namespace.repl/set-refresh-dirs "dev/server"
                                                                                             "src/server"
                                                                                             "specs/server"
                                                                                             "pepkey-migrations"
                                                                                             "modules")
                                              (com.stuartsierra.component.repl/reset))}}
 :emacs-figwheel {:dependencies [[com.cemerick/piggieback "0.2.2"]
                                 [figwheel-sidecar "0.5.16"]]
                  :plugins [[cider/cider-nrepl "0.16.0"]]
                  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                  :figwheel {:nrepl-middleware ["cider.nrepl/cider-middleware"
                                                "refactor-nrepl.middleware/wrap-refactor"
                                                "cemerick.piggieback/wrap-cljs-repl"]}}}
