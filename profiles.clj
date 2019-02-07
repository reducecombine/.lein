{:user {:dependencies [[nrepl-debugger "0.1.0-SNAPSHOT"]]
        :plugins [[lein-eftest "0.5.4"]
                  [lein-bikeshed "0.5.1" :exclusions [org.clojure/clojure]]
                  [lein-cljfmt "0.6.3" :exclusions [org.clojure/clojure org.clojure/tools.cli]]
                  [lein-pprint "1.1.2" :exclusions [org.clojure/clojure]]
                  [com.jakemccrary/lein-test-refresh "0.23.0" :exclusions [org.clojure/clojure
                                                                           org.clojure/tools.namespace]]
                  [nrepl/lein-nrepl "0.1.2" :exclusions [org.clojure/clojure]]
                  [lein-lein "0.2.0"]
                  [com.gfredericks/how-to-ns "0.1.9" :exclusions [org.clojure/clojure]]
                  [com.gfredericks/lein-all-my-files-should-end-with-exactly-one-newline-character
                   "0.1.1"
                   :exclusions
                   [org.clojure/clojure]]]
        :jvm-opts ["-Dapple.awt.UIElement=true"
                   "-XX:-OmitStackTraceInFastThrow"
                   "-Xmx18G"]
        :eftest {:multithread? false
                 :fail-fast? true}
        :how-to-ns {:require-docstring?      false
                    :sort-clauses?           true
                    :allow-refer-all?        false
                    :allow-extra-clauses?    false
                    :align-clauses?          false
                    :import-square-brackets? true}}
 :auth {:deploy-repositories [["releases" {:url "https://clojars.org/repo" :sign-releases false}]
                              ["snapshots" :clojars]]
        :repository-auth {#"https://clojars\.org/repo" {:username "vemv"
                                                        :password #=(eval (System/getenv "CLOJARS_PASSWORD"))}}}
 ;; the following profile is only necessary when staring `lein repl` from iTerm:
 :emacs-backend {:dependencies [[org.clojure/tools.nrepl "0.2.13" :exclusions [org.clojure/clojure]]
                                [org.clojure/tools.namespace "0.3.0-alpha4"]
                                [com.stuartsierra/component.repl "0.2.0"]]
                 :plugins [[refactor-nrepl "2.4.0" :exclusions [org.clojure/tools.logging]]
                           [cider/cider-nrepl "0.16.0"]]}
 :nedap-key {:source-paths ["specs/server"]
             :jvm-opts ["-Dlogback.configurationFile=resources/logback-no-stdout.xml"]
             :repl-options ^:replace {:port 41235
                                      :timeout 120000
                                      :init-ns user
                                      :init (do
                                              (clojure.core/require 'net.vemv.nrepl-debugger)
                                              (clojure.core/require 'clojure.tools.namespace.repl)
                                              (clojure.core/require 'com.stuartsierra.component.repl)
                                              (clojure.tools.namespace.repl/set-refresh-dirs "dev/server" "src/server" "specs/server")
                                              (com.stuartsierra.component.repl/reset))}}
 :emacs-figwheel {:dependencies [[com.cemerick/piggieback "0.2.2"]
                                 [figwheel-sidecar "0.5.16"]]
                  :plugins [[cider/cider-nrepl "0.16.0"]]
                  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                  :figwheel {:nrepl-middleware ["cider.nrepl/cider-middleware"
                                                "refactor-nrepl.middleware/wrap-refactor"
                                                "cemerick.piggieback/wrap-cljs-repl"]}}}
